package at.osa.minecraftplayground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * BlockEntity for RedstoneChainBlock that manages long-range wire connections and network updates.
 * <p>
 * This entity handles:
 * - Storing connections to other chain blocks (up to 3 connections per block)
 * - Managing a network of connected chain blocks
 * - Calculating and distributing redstone power across the network
 * - Syncing data between server and client for rendering
 * <p>
 * Unlike traditional adjacent-only connections, this entity allows chain blocks to connect
 * over distances (up to 24 blocks), with visible cables rendered between them.
 */
public class RedstoneChainEntity extends BlockEntity {

    // List of positions this chain block is connected to (max 3 connections)
    private final List<BlockPos> connections = new ArrayList<>();

    // Set of all positions in the connected network (cached for performance)
    private final Set<BlockPos> network = new HashSet<>();

    // Flag indicating the network needs to be recalculated
    private boolean networkDirty = true;

    // Maximum distance allowed between connected chain blocks (in blocks)
    private static final int MAX_CONNECTION_DISTANCE = 24;

    // How often to update the network (in ticks, 20 ticks = 1 second)
    private static final int UPDATE_INTERVAL = 20;

    // Counter for ticks since the last network update
    private int ticksSinceLastUpdate = 0;

    // Prevents recursive update calls (feedback loop protection)
    private boolean isUpdating = false;

    // Counts how many ticks the network has had no input signal
    private int ticksWithoutInput = 0;

    // How many ticks to wait before clearing the signal when input is lost
    private static final int SIGNAL_LOSS_DELAY = 1;

    // Cached value of the last input signal received
    private int cachedInputSignal = 0;

    /**
     * Constructor for the RedstoneChainEntity.
     * <p>
     * This creates a new block entity for a RedstoneChainBlock. The entity is responsible
     * for managing connections to other chain blocks and distributing redstone power across
     * the network.
     *
     * @param pos   The position of this block entity in the world
     * @param state The block state of the RedstoneChainBlock
     */
    public RedstoneChainEntity(BlockPos pos, BlockState state) {
        super(MinecraftPlayground.REDSTONE_CHAIN_ENTITY.get(), pos, state);
    }

    /**
     * Returns the list of positions this chain block is connected to.
     * <p>
     * This list contains the BlockPos of other RedstoneChainBlocks that this one is
     * connected to via visible cables. The list can contain 0-3 connections.
     * <p>
     * This is primarily used by:
     * - The renderer to draw cables between connected blocks
     * - Network management code to traverse connections
     * - Cleanup code when blocks are removed
     *
     * @return A list of BlockPos representing connected chain blocks
     */
    public List<BlockPos> getConnections() {
        return connections;
    }

    /**
     * Adds a connection from this chain block to another chain block.
     * <p>
     * This method creates a one-way connection from this block to the target block.
     * Connections are used to create visible cables between blocks and to form networks
     * that share redstone power.
     * <p>
     * The method performs several validation checks:
     * 1. If already connected to this target, do nothing (prevents duplicates)
     * 2. If already at max connections (3), do nothing (prevents too many cables)
     * 3. If target is too far away (>24 blocks), do nothing (prevents unrealistic distances)
     * <p>
     * After adding the connection:
     * - Marks the network as dirty (needs recalculation)
     * - Saves the change to disk
     * - Syncs to the client so the cable renders
     * - Merges networks if the target is part of another network
     *
     * @param target The position of the chain block to connect to
     */
    public void addConnection(BlockPos target) {
        if (connections.contains(target)) {
            return;
        }
        if (connections.size() >= 3) {
            return;
        }

        if (worldPosition.distSqr(target) > MAX_CONNECTION_DISTANCE * MAX_CONNECTION_DISTANCE) {
            return;
        }

        connections.add(target);
        networkDirty = true;
        setChanged();
        syncToClient();

        if (level != null) {
            BlockEntity be = level.getBlockEntity(target);
            if (be instanceof RedstoneChainEntity other) {
                mergeWithOtherNetwork(other);
            }
        }
    }

    /**
     * Removes a connection from this chain block to another chain block.
     * <p>
     * This is called when a cable connection is broken, either manually by the player
     * or automatically when a block is removed.
     * <p>
     * What happens when a connection is removed:
     * 1. The target position is removed from the connections list
     * 2. The network is marked as dirty (needs recalculation)
     * 3. Changes are saved to disk
     * 4. The client is synced so the cable disappears
     * 5. Both this block's network and the target's network are invalidated
     *    - This forces both networks to rebuild, as they may have been split
     * <p>
     * Note: This only removes the connection from THIS block to the target.
     * If there was a bidirectional connection, the target block also needs to
     * remove its connection back to this block.
     *
     * @param target The position of the chain block to disconnect from
     */
    public void removeConnection(BlockPos target) {
        if (connections.remove(target)) {
            networkDirty = true;
            setChanged();
            syncToClient();
            invalidateNetwork();
            if (level != null) {
                BlockEntity be = level.getBlockEntity(target);
                if (be instanceof RedstoneChainEntity cable) {
                    cable.invalidateNetwork();
                }
            }
        }
    }

    /**
     * Marks this chain block's entire network as needing recalculation.
     * <p>
     * This method uses a breadth-first search to find ALL chain blocks connected to
     * this one (directly or indirectly through other chain blocks), and marks each
     * one's network as "dirty" (needing rebuild).
     * <p>
     * This is necessary when:
     * - A connection is removed (network may have split into two separate networks)
     * - A block is removed from the network
     * - The network structure changes in any way
     * <p>
     * How it works:
     * 1. Start with this block's position
     * 2. Create a "visited" set to track which blocks we've already processed
     * 3. Use a queue for breadth-first traversal
     * 4. For each block in the queue:
     *    a. Skip if already visited
     *    b. Mark it as visited
     *    c. If it's a RedstoneChainEntity:
     *       - Set its networkDirty flag to true
     *       - Save the change
     *       - Add all its connections to the queue for processing
     * 5. Continue until all reachable blocks are processed
     * <p>
     * This ensures that when you break a connection, both resulting networks
     * properly rebuild themselves.
     */
    public void invalidateNetwork() {
        if (level == null) return;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(this.worldPosition);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!visited.add(current)) continue;

            BlockEntity be = level.getBlockEntity(current);
            if (be instanceof RedstoneChainEntity chain) {
                chain.networkDirty = true;
                chain.setChanged();

                for (BlockPos neighbor : chain.getConnectedChains()) {
                    if (!visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    /**
     * Removes all connections from this chain block.
     * <p>
     * This is typically called when the block is being removed from the world.
     * It ensures clean cleanup of all cable connections.
     * <p>
     * What happens:
     * 1. Make a copy of the connections list (to avoid modification during iteration)
     * 2. Clear the connections list
     * 3. Mark the network as dirty
     * 4. Save changes to disk
     * 5. Sync to client (cables disappear)
     * 6. Invalidate this block's network
     * 7. For each previously connected block:
     *    - Invalidate its network too
     *    - This ensures all affected networks rebuild properly
     * <p>
     * This is important for cleanup because when a block is removed, all blocks
     * that were connected to it need to recalculate their networks.
     */
    public void clearConnections() {
        List<BlockPos> oldConnections = new ArrayList<>(connections);
        connections.clear();
        networkDirty = true;
        setChanged();
        syncToClient();
        invalidateNetwork();

        if (level != null) {
            for (BlockPos target : oldConnections) {
                BlockEntity be = level.getBlockEntity(target);
                if (be instanceof RedstoneChainEntity chain) {
                    chain.invalidateNetwork();
                }
            }
        }
    }

    /**
     * Returns a list of positions that are valid chain block connections.
     * <p>
     * This method filters the connections list to only include positions that:
     * 1. Actually have a block entity at that location
     * 2. That block entity is a RedstoneChainEntity
     * 3. The position hasn't already been added to the result (prevents duplicates)
     * <p>
     * This is different from getConnections() because it validates that the
     * connected blocks still exist and are still chain blocks. This is useful
     * for network traversal where you only want to follow valid connections.
     * <p>
     * Use cases:
     * - Network building (rebuildNetwork)
     * - Network invalidation (invalidateNetwork)
     * - Any algorithm that needs to traverse the network graph
     *
     * @return A list of BlockPos for valid chain block connections
     */
    public List<BlockPos> getConnectedChains() {
        List<BlockPos> result = new ArrayList<>();
        if (level == null) return result;

        for (BlockPos pos : connections) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RedstoneChainEntity && !result.contains(pos)) {
                result.add(pos);
            }
        }
        return result;
    }

    /**
     * Rebuilds the network by discovering all connected chain blocks.
     * <p>
     * This method uses a breadth-first search (BFS) algorithm to find every chain block
     * that is reachable from this one through connections. The result is stored in the
     * 'network' set, which is used for power distribution.
     * <p>
     * How the algorithm works:
     * 1. Create a 'visited' set to track blocks we've already processed
     * 2. Create a queue and add this block's position as the starting point
     * 3. While the queue has positions to process:
     *    a. Take a position from the front of the queue
     *    b. If we've already visited this position, skip it
     *    c. Mark this position as visited
     *    d. If there's a RedstoneChainEntity at this position:
     *       - Get all its valid chain connections
     *       - Add any unvisited connections to the queue
     * 4. Once the queue is empty, we've found all reachable blocks
     * 5. Clear the current network set and add all visited positions
     * <p>
     * The result is a complete graph of all interconnected chain blocks.
     * This network is then used to:
     * - Find external power sources (computeNetworkInputPower)
     * - Distribute power evenly across all blocks (applySignalToNetwork)
     */
    public void rebuildNetwork() {
        if (level == null) return;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!visited.add(current)) continue;

            BlockEntity be = level.getBlockEntity(current);
            if (be instanceof RedstoneChainEntity chain) {
                for (BlockPos neighbor : chain.getConnectedChains()) {
                    if (!visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        network.clear();
        network.addAll(visited);
    }

    /**
     * Computes the maximum redstone input power for the entire network.
     * <p>
     * This method scans every block in the network and checks all their neighbors
     * to find the strongest redstone signal being input from outside the network.
     * <p>
     * How it works:
     * 1. Start with maxInput = 0
     * 2. For each block position in the network:
     *    a. Check all 6 directions (up, down, north, south, east, west)
     *    b. Get the neighbor block in that direction
     *    c. Skip if the neighbor is part of our network (RedstoneChainBlock)
     *    d. Skip if the neighbor is vanilla redstone wire (we don't want to create loops)
     *    e. Ask the neighbor for its redstone signal strength
     *       - Uses level.getSignal() with opposite direction (due to Minecraft's API quirk)
     *    f. If the signal is greater than 0, update maxInput to the higher value
     * 3. Return the maximum input power found
     * <p>
     * This allows the network to receive power from:
     * - Redstone torches
     * - Levers, buttons, pressure plates
     * - Repeaters and comparators
     * - Powered blocks (blocks activated by redstone)
     * - Any other redstone power source
     * <p>
     * The strongest signal found anywhere in the network becomes the network's power level.
     *
     * @return The maximum redstone power (0-15) from any external source
     */
    private int computeNetworkInputPower() {
        if (level == null) return 0;

        int maxInput = 0;
        for (BlockPos pos : network) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockState neighborState = level.getBlockState(neighborPos);
                Block block = neighborState.getBlock();

                if (block instanceof RedstoneChainBlock) continue;
                if (block instanceof RedStoneWireBlock) continue;

                int signal = level.getSignal(neighborPos, dir.getOpposite());
                if (signal > 0) {
                    maxInput = Math.max(maxInput, signal);
                }
            }
        }
        return maxInput;
    }

    /**
     * Applies a redstone signal strength to all blocks in the network.
     * <p>
     * This method updates the POWER property of every RedstoneChainBlock in the network
     * to the specified signal strength. This is how power gets distributed across
     * connected chain blocks.
     * <p>
     * How it works:
     * 1. For each position in the network:
     *    a. Get the current block state at that position
     *    b. Verify it's actually a RedstoneChainBlock (safety check)
     *    c. Get the current POWER value from the block state
     *    d. If the current power is different from the target signal:
     *       - Create a new block state with the updated POWER value
     *       - Set the block in the world (flag 3 = update neighbors & send to client)
     * 2. Only update blocks that actually need changing (performance optimization)
     * <p>
     * The flag value of 3 means:
     * - Bit 0 (1): Send the block update to clients (they see the visual change)
     * - Bit 1 (2): Update neighboring blocks (notifies redstone components)
     * <p>
     * This ensures that:
     * - All chain blocks in the network have the same power level
     * - Visual feedback (block state) is synchronized across server and client
     * - Neighboring redstone components are notified of power changes
     *
     * @param signal The redstone signal strength (0-15) to apply to all network blocks
     */
    private void applySignalToNetwork(int signal) {
        if (level == null) return;

        for (BlockPos pos : network) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof RedstoneChainBlock) {
                int old = state.getValue(RedstoneChainBlock.POWER);
                if (old != signal) {
                    level.setBlock(pos, state.setValue(RedstoneChainBlock.POWER, signal), 3);
                }
            }
        }
    }

    /**
     * Updates the redstone signal for the entire network.
     * <p>
     * This is the main method that orchestrates network power calculation and distribution.
     * It's called when:
     * - A neighbor block changes (via neighborChanged in RedstoneChainBlock)
     * - The network structure changes (connections added/removed)
     * - Periodic tick updates
     * <p>
     * The method includes several important features:
     * <p>
     * FEEDBACK LOOP PROTECTION:
     * - Uses the 'isUpdating' flag to prevent recursive calls
     * - If already updating, exits immediately
     * - Sets flag at start, clears in finally block (ensures cleanup even if exception occurs)
     * <p>
     * NETWORK REBUILD:
     * - Checks if network is dirty (needs rebuild)
     * - Calls rebuildNetwork() to discover all connected blocks
     * - Clears the dirty flag once rebuild is complete
     * <p>
     * SIGNAL CALCULATION:
     * - Calls computeNetworkInputPower() to find the strongest input signal
     * - If input > 0: Updates cached signal and resets the "no input" counter
     * - If input = 0: Increments the "no input" counter
     *   - After SIGNAL_LOSS_DELAY ticks without input, clears the cached signal
     *   - This prevents instant signal loss, providing stability
     * <p>
     * SIGNAL DISTRIBUTION:
     * - Calls applySignalToNetwork() with the cached signal
     * - All blocks in the network get updated to the same power level
     * <p>
     * This method ensures smooth, stable power distribution across the network
     * while preventing infinite loops and signal flickering.
     */
    public void updateSignalInNetwork() {
        if (isUpdating) {
            return;
        }
        isUpdating = true;
        try {
            if (networkDirty) {
                rebuildNetwork();
                networkDirty = false;
            }

            int input = computeNetworkInputPower();

            if (input > 0) {
                cachedInputSignal = input;
                ticksWithoutInput = 0;
            } else {
                ticksWithoutInput++;
                if (ticksWithoutInput >= SIGNAL_LOSS_DELAY) {
                    cachedInputSignal = 0;
                }
            }

            applySignalToNetwork(cachedInputSignal);

        } finally {
            isUpdating = false;
        }
    }

    /**
     * Merges this network with another network.
     * <p>
     * This is called when a new connection is added between two chain blocks that
     * may already be part of different networks. Instead of rebuilding both networks
     * from scratch, this efficiently merges them.
     * <p>
     * How the merge works:
     * 1. Get both network sets (this block's and the other block's)
     * 2. Determine which network is larger
     * 3. Start with the larger network as the base (optimization)
     * 4. Add all blocks from the smaller network to it
     * 5. For every block in the merged set:
     *    a. Get its block entity
     *    b. If it's a RedstoneChainEntity:
     *       - Replace its network set with the merged set
     *       - Mark it as NOT dirty (network is now up-to-date)
     * <p>
     * Why merge instead of rebuild?
     * - More efficient: O(n+m) instead of potentially O((n+m)²) for BFS rebuild
     * - Preserves existing network information
     * - Prevents unnecessary recalculation
     * <p>
     * After the merge, all blocks in both original networks now share the same
     * network set and will calculate/distribute power as one unified network.
     *
     * @param other The other RedstoneChainEntity whose network should be merged with this one
     */
    private void mergeWithOtherNetwork(RedstoneChainEntity other) {
        if (level == null) return;

        Set<BlockPos> network1 = new HashSet<>(this.network);
        Set<BlockPos> network2 = new HashSet<>(other.network);

        Set<BlockPos> larger = network1.size() >= network2.size() ? network1 : network2;
        Set<BlockPos> smaller = network1.size() < network2.size() ? network1 : network2;

        Set<BlockPos> merged = new HashSet<>(larger);
        merged.addAll(smaller);

        for (BlockPos pos : merged) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RedstoneChainEntity chain) {
                chain.network.clear();
                chain.network.addAll(merged);
                chain.networkDirty = false;
            }
        }
    }

    /**
     * Called every game tick (20 times per second) for this block entity.
     *
     * This is the ticker method that Minecraft calls automatically for block entities
     * that have registered a ticker (see RedstoneChainBlock.getTicker).
     *
     * What happens every tick:
     * 1. First, validate this is actually a RedstoneChainEntity (type safety)
     * 2. If on client side, return immediately (all logic is server-side only)
     * 3. Increment the tick counter (ticksSinceLastUpdate)
     * 4. When the counter reaches UPDATE_INTERVAL (20 ticks = 1 second):
     *    - Reset the counter to 0
     *    - Call updateSignalInNetwork() to recalculate and distribute power
     *
     * Why periodic updates instead of event-driven only?
     * - Catches edge cases where neighbor updates might be missed
     * - Ensures networks stay synchronized even if some updates are lost
     * - Provides a "heartbeat" for the network that can detect and fix issues
     * - Once per second is frequent enough for responsiveness but light on performance
     *
     * This combines with event-driven updates (neighborChanged) to create a robust
     * system that responds quickly to changes but also self-corrects periodically.
     *
     * @param level The world/level containing this block entity
     * @param pos The position of this block entity
     * @param state The block state of this block entity
     * @param be The block entity itself (type-checked to RedstoneChainEntity)
     */
    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (!(be instanceof RedstoneChainEntity chain)) return;
        if (level.isClientSide) return;

        chain.ticksSinceLastUpdate++;
        if (chain.ticksSinceLastUpdate >= UPDATE_INTERVAL) {
            chain.ticksSinceLastUpdate = 0;
            chain.updateSignalInNetwork();
        }
    }

    /**
     * Returns the current redstone signal strength of this chain block.
     *
     * This is a simple helper method that reads the POWER value from the block state.
     * It's used by RedstoneChainBlock.getSignal() to provide the signal strength
     * to neighboring blocks.
     *
     * The value returned is between 0-15:
     * - 0 means no power (unpowered)
     * - 15 means maximum power (fully powered)
     * - Values 1-14 are intermediate power levels
     *
     * @return The redstone power level (0-15) of this block
     */
    public int getSignal() {
        return getBlockState().getValue(RedstoneChainBlock.POWER);
    }

    /**
     * Synchronizes this block entity's data to connected clients.
     *
     * In Minecraft, the server and client maintain separate worlds. When data changes
     * on the server (like connection lists), the client needs to be notified so it can
     * update its rendering.
     *
     * This method:
     * 1. Checks if we're on the server (!level.isClientSide)
     *    - Client-side calls are ignored (no need to sync from client to server)
     * 2. Calls level.sendBlockUpdated() which:
     *    - Sends a packet to all clients watching this chunk
     *    - The packet contains the updated block entity data
     *    - Clients receive the packet and update their local copy
     *
     * The flag value of 3 means:
     * - Bit 0 (1): Send the update to clients
     * - Bit 1 (2): Cause a block update (re-render)
     *
     * This is crucial for:
     * - Rendering cables when connections change
     * - Updating visual power levels
     * - Keeping client and server in sync
     *
     * Without this, players wouldn't see cables appear/disappear or power changes.
     */
    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    // ===== SERIALIZATION =====
    // These methods handle saving and loading data to/from disk and syncing to clients

    /**
     * Saves this block entity's data to disk (NBT format).
     *
     * NBT (Named Binary Tag) is Minecraft's data storage format, similar to JSON but binary.
     * This method is called when:
     * - The chunk is saved to disk
     * - The world is closing
     * - The game is auto-saving
     *
     * What gets saved:
     * 1. First, call the parent class to save standard data (position, etc.)
     * 2. Create a ListTag to hold all connection positions
     * 3. For each connection:
     *    a. Create a CompoundTag (like a JSON object)
     *    b. Store the x, y, z coordinates as integers
     *    c. Add this tag to the list
     * 4. Store the list in the main tag under the key "Connections"
     *
     * Why save connections but not the network?
     * - Connections are the fundamental data (what we explicitly created)
     * - The network can be rebuilt from connections (it's derived data)
     * - This saves disk space and prevents stale network data
     *
     * When the world loads, loadAdditional() will read this data back.
     *
     * @param tag The NBT tag to write data into
     * @param registries Registry access for complex data types (not used here)
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (BlockPos pos : connections) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            list.add(posTag);
        }
        tag.put("Connections", list);
    }

    /**
     * Loads this block entity's data from disk (NBT format).
     *
     * This is the counterpart to saveAdditional(). It's called when:
     * - A chunk is loaded from disk
     * - The world is starting up
     * - The player enters a previously saved area
     *
     * What gets loaded:
     * 1. First, call the parent class to load standard data
     * 2. Clear the current connections list (start fresh)
     * 3. Get the "Connections" list from the NBT tag
     *    - TAG_COMPOUND (10) indicates we're reading CompoundTags from the list
     * 4. For each entry in the list:
     *    a. Cast it to CompoundTag
     *    b. Read the x, y, z integers
     *    c. Create a BlockPos from those coordinates
     *    d. Add it to the connections list
     * 5. Mark the network as dirty (needs rebuild)
     *    - The network isn't saved, so we need to rebuild it from connections
     *
     * After loading, the block entity has the same connections it had when saved,
     * and will rebuild its network on the next update.
     *
     * @param tag The NBT tag to read data from
     * @param registries Registry access for complex data types (not used here)
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        connections.clear();
        ListTag list = tag.getList("Connections", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag posTag = (CompoundTag) t;
            connections.add(new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")));
        }
        networkDirty = true;
    }

    /**
     * Creates a packet to sync this block entity's data to clients.
     *
     * This method is called when the client needs to be updated about changes to this
     * block entity. It's part of Minecraft's client-server synchronization system.
     *
     * The packet contains:
     * - The position of this block entity
     * - The data from getUpdateTag() (which includes our connections list)
     *
     * When the packet is sent:
     * 1. Server calls this method to create the packet
     * 2. Packet is sent to all clients watching this chunk
     * 3. Clients receive the packet and update their local copy
     * 4. The renderer uses the updated data to draw cables
     *
     * This is called automatically by syncToClient() via level.sendBlockUpdated().
     *
     * @return A packet containing this block entity's data for client sync
     */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Creates an NBT tag containing data for client synchronization.
     *
     * This method is called when the client needs a complete snapshot of this block
     * entity's data. It's used in conjunction with getUpdatePacket().
     *
     * What this method does:
     * 1. Creates a new empty CompoundTag
     * 2. Calls saveAdditional() to populate it with our data
     *    - This includes the connections list
     * 3. Returns the populated tag
     *
     * The difference between this and saveAdditional():
     * - saveAdditional() is for saving to disk (world save files)
     * - getUpdateTag() is for syncing to clients (network packets)
     * - We use the same data format for both (they need the same information)
     *
     * When a client joins or a chunk is loaded on the client:
     * 1. Server calls this method
     * 2. Returns a tag with all connection data
     * 3. Tag is sent to the client in a packet
     * 4. Client's copy of this block entity is updated
     * 5. Renderer draws cables based on the connections
     *
     * @param registries Registry access for complex data types
     * @return An NBT tag containing this block entity's data for client sync
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
}
