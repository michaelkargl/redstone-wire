package at.osa.redstonewire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedstoneConnectorBlockEntity extends BlockEntity {

    private final List<BlockPos> directConnections = new ArrayList<>();

    // In-memory routing cache — not persisted, rebuilt lazily after topology changes or world load.
    // Set prevents duplicate entries when two connector paths reach the same OutputBlock.
    private final Set<BlockPos> reachableOutputs = new HashSet<>();
    private boolean cacheDirty = true;

    public RedstoneConnectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(RedstoneWire.REDSTONE_CONNECTOR_ENTITY.get(), pos, blockState);
    }

    /**
     * Saves this block entity's data to disk (NBT format).
     * NBT (Named Binary Tag) is Minecraft's data storage format, similar to JSON but binary.
     * * This method is called when:
     * * - The chunk is saved to disk
     * * - The world is closing
     * * - The game is auto-saving
     * CompoundTag is the NBT version of a JSON object
     *
     * @param tag
     * @param registries
     */
    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        var list = new ListTag();
        for (var position : directConnections) {
            var posTag = new CompoundTag();
            posTag.putInt("x", position.getX());
            posTag.putInt("y", position.getY());
            posTag.putInt("z", position.getZ());
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
     *
     * @param tag
     * @param registries
     */
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // parse list of NBT positions back to connections
        var connections =
                tag.getList("Connections", ListTag.TAG_COMPOUND)
                        .stream()
                        .filter(t -> t instanceof CompoundTag)
                        .map(t -> (CompoundTag) t)
                        .map(positionTag -> new BlockPos(
                                positionTag.getInt("x"),
                                positionTag.getInt("y"),
                                positionTag.getInt("z")))
                        .toList();

        this.directConnections.clear();
        this.directConnections.addAll(connections);

        // Cache is never persisted — must be rebuilt after every world load.
        this.cacheDirty = true;
    }

    /**
     * Creates a packet to sync this block entity's data to clients.
     * <p>
     * This method is called when the client needs to be updated about changes to this
     * block entity. It's part of Minecraft's client-server synchronization system.
     * </p>
     * <p>
     * The packet contains:
     * - The position of this block entity
     * - The data from getUpdateTag()
     * </p>
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
     * </p><p>
     * The difference between this and saveAdditional():
     * - saveAdditional() is for saving to disk (world save files)
     * - getUpdateTag() is for syncing to clients (network packets)
     * - We use the same data format for both (they need the same information)
     * </p>
     * @param registries
     * @return
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    /**
     * Hot path — called up to 20×/sec by clock signals.
     * Rebuilds the output cache lazily (once per topology change), then sets
     * power on every reachable OutputBlock in one pass.
     */
    public void propagateSignal(int power, Level level) {
        if (cacheDirty) {
            rebuildOutputCache(level);
        }

        for (BlockPos outputPos : reachableOutputs) {
            BlockState state = level.getBlockState(outputPos);
            level.setBlock(outputPos, state.setValue(RedstoneOutputBlock.POWER, power), Block.UPDATE_ALL);
        }
    }

    /**
     * Entry point for cache rebuild. Clears the list, starts DFS, marks cache clean.
     */
    void rebuildOutputCache(Level level) {
        reachableOutputs.clear();
        rebuildOutputCache(level, new HashSet<>(), reachableOutputs);
        cacheDirty = false;
    }

    /**
     * DFS worker — same structural shape as markNetworkDirty.
     * Visits every connector reachable via directConnections and appends any adjacent
     * OutputBlock positions to the shared accumulator.
     *
     * TODO(human): implement this method body.
     *   Steps:
     *   1. Loop prevention: if this position is already in visited, return immediately.
     *   2. For each of the 6 Directions: if the adjacent block is a RedstoneOutputBlock,
     *      add that position to the accumulator.
     *   3. For each pos in directConnections: look up the block entity; if it's a
     *      RedstoneConnectorBlockEntity, recurse passing level, visited, accumulator.
     */
    void rebuildOutputCache(Level level, Set<BlockPos> visited, Set<BlockPos> accumulator) {
        // TODO(human): implement
    }

    /**
     * Propagates a dirty-cache flag across the entire connected network.
     * Uses recursive DFS with a visited set to avoid infinite loops in cycles.
     */
    public void markNetworkDirty(Set<BlockPos> visited) {
        if (!visited.add(this.getBlockPos())) return; // loop prevention
        this.cacheDirty = true;
        for (BlockPos neighbor : directConnections) {
            var be = level.getBlockEntity(neighbor);
            if (be instanceof RedstoneConnectorBlockEntity conn) {
                conn.markNetworkDirty(visited);
            }
        }
    }

    public void addConnection(BlockPos pos) {
        var alreadyConnected = directConnections.contains(pos);
        if (alreadyConnected) {
            return;
        }

        // TODO kami: Add max connection check
        // TODO kami: Add distance check

        directConnections.add(pos);
        markNetworkDirty(new HashSet<>());

        // Tells Minecraft "this blocks connections have changed, include them next time the chunk is saved to disk
        this.setChanged();
        // Makes the client aware of changes in this entity (see getUpdatePacket() and getUpdateTag())
        this.syncToClient();
    }

    /**
     * Removes a direct connection to the given position and invalidates the entire
     * network's cache so the now-split graph recalculates correctly.
     */
    public void removeConnection(BlockPos pos) {
        directConnections.remove(pos);
        markNetworkDirty(new HashSet<>());
        this.setChanged();
        this.syncToClient();
    }

    public List<BlockPos> getConnections() {
        return this.directConnections;
    }

    public int getSignal() {
        return 0;
    }

    /**
     * Synchronizes this block entity's data to connected clients.
     * In Minecraft, the server and client maintain separate worlds. When data changes
     * on the server (like connection lists), the client needs to be notified so it can
     * update its rendering.
     */
    private void syncToClient() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        var sendClientUpdateFlag = 0x10;
        var scheduleBlockRerenderFlag = 0x01;

        // Sends a packet to all clients watching this chunk
        // The packet contains the updated block entity date
        // Clients receive the packet and update their local copy
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), sendClientUpdateFlag | scheduleBlockRerenderFlag);
    }
}
