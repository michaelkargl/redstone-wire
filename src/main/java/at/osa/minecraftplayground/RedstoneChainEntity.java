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

    // ===== Connection Management =====
    /**
     * Direct connections from this block to other chain blocks.
     * Maximum of MAX_CONNECTIONS per block.
     */
    private final List<BlockPos> connections = new ArrayList<>();

    // ===== Network Management =====
    /**
     * Cached set of all blocks in this network (includes this block and all transitively connected blocks).
     * Rebuilt when networkDirty is true.
     */
    private final Set<BlockPos> network = new HashSet<>();

    /**
     * Flag indicating the network cache is stale and needs rebuilding.
     * Set to true when connections change or network structure is invalidated.
     */
    private boolean networkDirty = true;

    /**
     * Counts ticks since the last periodic update.
     * Reset to 0 when UPDATE_INTERVAL is reached.
     */
    private int ticksSinceLastUpdate = 0;

    // ===== Feedback Loop Protection =====
    /**
     * Prevents recursive update calls that could cause infinite loops.
     * Set to true during updateSignalInNetwork(), cleared in finally block.
     */
    private boolean isUpdating = false;

    // ===== Signal Management =====
    /**
     * Number of ticks that have passed with no external power input.
     * Used to delay signal loss and prevent flickering.
     */
    private int ticksWithoutInput = 0;

    /**
     * The last known input signal strength from external sources.
     * Cached to maintain signal briefly after input is lost.
     */
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
     * connected to via visible cables.
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
    /**
     * Adds a cable connection from this block to another chain block.
     * <p>
     * Performs validation checks:
     * - Rejects duplicate connections
     * - Rejects if already at max connections
     * - Rejects if target is too far away
     * <p>
     * After adding connection:
     * - Marks network as dirty (needs rebuild)
     * - Saves to disk and syncs to client
     * - Merges networks if target is part of another network
     *
     * @param target Position of the chain block to connect to
     */
    public void addConnection(BlockPos target) {
        // Validation: Check if already connected
        if (isAlreadyConnectedTo(target)) {
            return;
        }

        // Validation: Check connection limit
        if (isAtMaxConnections()) {
            return;
        }

        // Validation: Check distance
        if (isTooFarAway(target)) {
            return;
        }

        // Add the connection
        connections.add(target);

        // Update state
        markNetworkDirty();
        saveAndSync();

        // Merge networks if target is part of another network
        mergeNetworkWithTarget(target);
    }

    private boolean isAlreadyConnectedTo(BlockPos target) {
        // we treat the entire network as a single entity for connection purposes
        // if it is connected to any block in the target's network, it is connected to all
        return connections.contains(target);
    }

    private boolean isAtMaxConnections() {
        return connections.size() >= Config.MAX_CONNECTIONS_PER_CHAIN.getAsInt();
    }

    /**
     * Checks if a target position is too far away to create a connection.
     * <p>
     * === UNDERSTANDING DISTANCE IN 3D SPACE ===
     * <p>
     * Imagine you want to measure the straight-line distance between two points in Minecraft
     * (like measuring "as the crow flies"). In 3D space, you need to consider three dimensions:
     * - X axis: east/west
     * - Y axis: up/down
     * - Z axis: north/south
     * <p>
     * If THIS block is at position (x1, y1, z1) and TARGET is at (x2, y2, z2):
     * <pre>
     * Step 1: Find the differences in each dimension
     *   dx = x2 - x1  (how far east/west)
     *   dy = y2 - y1  (how far up/down)
     *   dz = z2 - z1  (how far north/south)
     *
     * Step 2: Calculate straight-line distance using Pythagorean theorem in 3D
     *   distance = √(dx² + dy² + dz²)
     *
     *   ● THIS (x1,y1,z1)
     *    \
     *     \ distance
     *      \
     *       ● TARGET (x2,y2,z2)
     *
     * Example: THIS at (0,0,0), TARGET at (3,4,0)
     *   dx = 3, dy = 4, dz = 0
     *   distance = √(3² + 4² + 0²) = √(9 + 16 + 0) = √25 = 5 blocks
     *   If 5 > MAX_DISTANCE, it's too far
     * </pre>
     * <p>
     * === THE OPTIMIZATION TRICK ===
     * <p>
     * We want to check: "Is distance > MAX_DISTANCE?"
     * Normally: √(dx² + dy² + dz²) > MAX_DISTANCE
     * <p>
     * But square root (√) is SLOW for computers to calculate. Here's the trick:
     * If we square BOTH sides of the comparison, the inequality stays true!
     * <p>
     * Square both sides:
     *   [√(dx² + dy² + dz²)]² > [MAX_DISTANCE]²
     *
     * The √ and ² cancel out on the left:
     *   dx² + dy² + dz² > MAX_DISTANCE²
     * <p>
     * Now we can compare WITHOUT calculating the square root!
     * This is what distSqr() returns: the "squared distance" = dx² + dy² + dz²
     * <p>
     * === CONCRETE EXAMPLE ===
     * <p>
     * If MAX_CONNECTION_DISTANCE = 24 blocks:
     *   maxDistSqr = 24 × 24 = 576
     * <p>
     * Example 1: Target at offset (10, 0, 10) from THIS block
     *   dx=10, dy=0, dz=10
     *   distSqr = 10² + 0² + 10² = 100 + 0 + 100 = 200
     *   Is 200 > 576? NO → Connection ALLOWED ✓
     *   (Actual distance would be √200 ≈ 14.14 blocks, which is less than 24)
     * <p>
     * Example 2: Target at offset (20, 20, 20) from THIS block
     *   dx=20, dy=20, dz=20
     *   distSqr = 20² + 20² + 20² = 400 + 400 + 400 = 1200
     *   Is 1200 > 576? YES → Connection REJECTED ✗
     *   (Actual distance would be √1200 ≈ 34.64 blocks, which is more than 24)
     *
     * @param target The position we want to connect to
     * @return true if target is beyond MAX_CONNECTION_DISTANCE, false if within range
     */
    private boolean isTooFarAway(BlockPos target) {
        // Calculate the maximum allowed squared distance
        // (We square the max distance so we can compare without using square root)
        double maxDistSqr = Config.MAX_CONNECTION_DISTANCE.getAsInt() * Config.MAX_CONNECTION_DISTANCE.getAsInt();

        // distSqr returns: dx²+dy²+dz²
        // If this squared distance is greater than our max squared distance, it's too far
        return worldPosition.distSqr(target) > maxDistSqr;
    }

    private void markNetworkDirty() {
        networkDirty = true;
    }

    private void saveAndSync() {
        setChanged();      // Marks for saving to disk
        syncToClient();    // Sends update to client for rendering
    }

    private void mergeNetworkWithTarget(BlockPos target) {
        if (level == null) {
            return;
        }

        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof RedstoneChainEntity other) {
            mergeWithOtherNetwork(other);
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
     * - This forces both networks to rebuild, as they may have been split
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
     * a. Skip if already visited
     * b. Mark it as visited
     * c. If it's a RedstoneChainEntity:
     * - Set its networkDirty flag to true
     * - Save the change
     * - Add all its connections to the queue for processing
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
     * - Invalidate its network too
     * - This ensures all affected networks rebuild properly
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
     * a. Take a position from the front of the queue
     * b. If we've already visited this position, skip it
     * c. Mark this position as visited
     * d. If there's a RedstoneChainEntity at this position:
     * - Get all its valid chain connections
     * - Add any unvisited connections to the queue
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
    /**
     * Computes the maximum redstone input power for the entire network.
     * <p>
     * Scans all blocks in the network and checks all their neighbors for redstone signals.
     * Returns the strongest signal found from any external source (non-network block).
     * <p>
     * Ignores:
     * - Other chain blocks in the network (to avoid counting internal signals)
     * - Vanilla redstone wire (to prevent feedback loops)
     *
     * @return Maximum power level (0-15) from any external redstone source
     */
    private int computeNetworkInputPower() {
        if (level == null) return 0;

        int maxInput = 0;

        // Check every block in the network for external power
        for (BlockPos pos : network) {
            int powerAtBlock = findMaxPowerAroundBlock(pos);
            maxInput = Math.max(maxInput, powerAtBlock);
        }

        return maxInput;
    }

    /**
     * Finds the maximum redstone power from neighbors of a single block.
     * Checks all 6 directions (up, down, north, south, east, west).
     *
     * @param pos Position to check around
     * @return Maximum power from neighbors (0-15)
     */
    private int findMaxPowerAroundBlock(BlockPos pos) {
        int maxPower = 0;

        // Check all 6 directions
        for (Direction dir : Direction.values()) {
            int powerFromDirection = getPowerFromDirection(pos, dir);
            maxPower = Math.max(maxPower, powerFromDirection);
        }

        return maxPower;
    }

    /**
     * Gets redstone power from a specific direction of a block.
     * Returns 0 if the neighbor should be ignored (chain block or vanilla redstone).
     *
     * @param pos The block position we're checking from
     * @param dir The direction to check
     * @return Power level from that direction (0-15)
     */
    private int getPowerFromDirection(BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        Block block = neighborState.getBlock();

        // Skip other chain blocks in network (internal connections)
        if (block instanceof RedstoneChainBlock) {
            return 0;
        }

        // Skip vanilla redstone wire (prevents feedback loops)
        if (block instanceof RedStoneWireBlock) {
            return 0;
        }

        // Get signal from this neighbor
        // Note: dir.getOpposite() is used due to Minecraft's backwards direction API
        return level.getSignal(neighborPos, dir.getOpposite());
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
     * a. Get the current block state at that position
     * b. Verify it's actually a RedstoneChainBlock (safety check)
     * c. Get the current POWER value from the block state
     * d. If the current power is different from the target signal:
     * - Create a new block state with the updated POWER value
     * - Set the block in the world (flag 3 = update neighbors & send to client)
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
    /**
     * Updates the redstone signal for the entire network.
     * <p>
     * This is the main coordination method that:
     * 1. Prevents feedback loops using isUpdating flag
     * 2. Rebuilds network if structure changed
     * 3. Checks for external power input
     * 4. Updates cached signal with delay to prevent flickering
     * 5. Distributes signal to all blocks in network
     * <p>
     * Called by:
     * - neighborChanged() when adjacent blocks change
     * - Periodic ticker every UPDATE_INTERVAL ticks
     * - When connections are added/removed
     */
    public void updateSignalInNetwork() {
        // Prevent infinite recursion (feedback loop protection)
        if (isUpdating) {
            return;
        }

        isUpdating = true;
        try {
            // Step 1: Ensure network is up-to-date
            ensureNetworkIsBuilt();

            // Step 2: Check for external power input
            int currentInput = computeNetworkInputPower();

            // Step 3: Update cached signal with delay (prevents flickering)
            updateCachedSignal(currentInput);

            // Step 4: Distribute the signal to all blocks
            applySignalToNetwork(cachedInputSignal);

        } finally {
            // Always clear the updating flag, even if an exception occurs
            isUpdating = false;
        }
    }

    /**
     * Rebuilds the network if it's marked as dirty.
     * After rebuilding, clears the dirty flag.
     */
    private void ensureNetworkIsBuilt() {
        if (networkDirty) {
            rebuildNetwork();
            networkDirty = false;
        }
    }

    /**
     * Updates the cached signal based on current input.
     * Implements a delay before clearing signal to prevent flickering.
     *
     * @param currentInput The current power level from external sources
     */
    private void updateCachedSignal(int currentInput) {
        if (currentInput > 0) {
            // Power detected - update cache immediately and reset delay counter
            cachedInputSignal = currentInput;
            ticksWithoutInput = 0;
        } else {
            // No power - increment delay counter
            ticksWithoutInput++;

            // Only clear signal after delay period has passed
            if (ticksWithoutInput >= Config.SIGNAL_LOSS_DELAY_TICKS.getAsInt()) {
                cachedInputSignal = 0;
            }
            // Otherwise keep the cached signal (prevents flickering)
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
     * a. Get its block entity
     * b. If it's a RedstoneChainEntity:
     * - Replace its network set with the merged set
     * - Mark it as NOT dirty (network is now up-to-date)
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
     * <p>
     * This is the ticker method that Minecraft calls automatically for block entities
     * that have registered a ticker (see RedstoneChainBlock.getTicker).
     * <p>
     * What happens every tick:
     * 1. First, validate this is actually a RedstoneChainEntity (type safety)
     * 2. If on client side, return immediately (all logic is server-side only)
     * 3. Increment the tick counter (ticksSinceLastUpdate)
     * 4. When the counter reaches UPDATE_INTERVAL (20 ticks = 1 second):
     * - Reset the counter to 0
     * - Call updateSignalInNetwork() to recalculate and distribute power
     * <p>
     * Why periodic updates instead of event-driven only?
     * - Catches edge cases where neighbor updates might be missed
     * - Ensures networks stay synchronized even if some updates are lost
     * - Provides a "heartbeat" for the network that can detect and fix issues
     * - Once per second is frequent enough for responsiveness but light on performance
     * - If someone breaks an intermittent connection, the periodic update will detect it and fix it (turn off power)
     * <p>
     * This combines with event-driven updates (neighborChanged) to create a robust
     * system that responds quickly to changes but also self-corrects periodically.
     *
     * @param level The world/level containing this block entity
     * @param pos   The position of this block entity
     * @param state The block state of this block entity
     * @param be    The block entity itself (type-checked to RedstoneChainEntity)
     */
    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (!(be instanceof RedstoneChainEntity chain)) return;
        if (level.isClientSide) return;

        chain.ticksSinceLastUpdate++;
        if (chain.ticksSinceLastUpdate >= Config.UPDATE_INTERVAL_TICKS.getAsInt()) {
            chain.ticksSinceLastUpdate = 0;
            chain.updateSignalInNetwork();
        }
    }

    /**
     * Returns the current redstone signal strength of this chain block.
     * <p>
     * This is a simple helper method that reads the POWER value from the block state.
     * It's used by RedstoneChainBlock.getSignal() to provide the signal strength
     * to neighboring blocks.
     * <p>
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
     * <p>
     * In Minecraft, the server and client maintain separate worlds. When data changes
     * on the server (like connection lists), the client needs to be notified so it can
     * update its rendering.
     * <p>
     * This method:
     * 1. Checks if we're on the server (!level.isClientSide)
     * - Client-side calls are ignored (no need to sync from client to server)
     * 2. Calls level.sendBlockUpdated() which:
     * - Sends a packet to all clients watching this chunk
     * - The packet contains the updated block entity data
     * - Clients receive the packet and update their local copy
     * <p>
     * The flag value of 3 means:
     * - Bit 0 (1): Send the update to clients
     * - Bit 1 (2): Cause a block update (re-render)
     * <p>
     * This is crucial for:
     * - Rendering cables when connections change
     * - Updating visual power levels
     * - Keeping client and server in sync
     * <p>
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
     * <p>
     * NBT (Named Binary Tag) is Minecraft's data storage format, similar to JSON but binary.
     * This method is called when:
     * - The chunk is saved to disk
     * - The world is closing
     * - The game is auto-saving
     * <p>
     * What gets saved:
     * 1. First, call the parent class to save standard data (position, etc.)
     * 2. Create a ListTag to hold all connection positions
     * 3. For each connection:
     * a. Create a CompoundTag (like a JSON object)
     * b. Store the x, y, z coordinates as integers
     * c. Add this tag to the list
     * 4. Store the list in the main tag under the key "Connections"
     * <p>
     * Why save connections but not the network?
     * - Connections are the fundamental data (what we explicitly created)
     * - The network can be rebuilt from connections (it's derived data)
     * - This saves disk space and prevents stale network data
     * <p>
     * When the world loads, loadAdditional() will read this data back.
     *
     * @param tag        The NBT tag to write data into
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
     * <p>
     * This is the counterpart to saveAdditional(). It's called when:
     * - A chunk is loaded from disk
     * - The world is starting up
     * - The player enters a previously saved area
     * <p>
     * What gets loaded:
     * 1. First, call the parent class to load standard data
     * 2. Clear the current connections list (start fresh)
     * 3. Get the "Connections" list from the NBT tag
     * - TAG_COMPOUND (10) indicates we're reading CompoundTags from the list
     * 4. For each entry in the list:
     * a. Cast it to CompoundTag
     * b. Read the x, y, z integers
     * c. Create a BlockPos from those coordinates
     * d. Add it to the connections list
     * 5. Mark the network as dirty (needs rebuild)
     * - The network isn't saved, so we need to rebuild it from connections
     * <p>
     * After loading, the block entity has the same connections it had when saved,
     * and will rebuild its network on the next update.
     *
     * @param tag        The NBT tag to read data from
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
     * <p>
     * This method is called when the client needs to be updated about changes to this
     * block entity. It's part of Minecraft's client-server synchronization system.
     * <p>
     * The packet contains:
     * - The position of this block entity
     * - The data from getUpdateTag() (which includes our connections list)
     * <p>
     * When the packet is sent:
     * 1. Server calls this method to create the packet
     * 2. Packet is sent to all clients watching this chunk
     * 3. Clients receive the packet and update their local copy
     * 4. The renderer uses the updated data to draw cables
     * <p>
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
     * <p>
     * This method is called when the client needs a complete snapshot of this block
     * entity's data. It's used in conjunction with getUpdatePacket().
     * <p>
     * What this method does:
     * 1. Creates a new empty CompoundTag
     * 2. Calls saveAdditional() to populate it with our data
     * - This includes the connections list
     * 3. Returns the populated tag
     * <p>
     * The difference between this and saveAdditional():
     * - saveAdditional() is for saving to disk (world save files)
     * - getUpdateTag() is for syncing to clients (network packets)
     * - We use the same data format for both (they need the same information)
     * <p>
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
