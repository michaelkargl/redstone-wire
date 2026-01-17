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

public class RedstoneChainEntity extends BlockEntity {

    private final List<BlockPos> connections = new ArrayList<>();
    private final Set<BlockPos> network = new HashSet<>();
    private boolean networkDirty = true;
    private static final int MAX_CONNECTION_DISTANCE = 24;
    private static final int UPDATE_INTERVAL = 20;
    private int ticksSinceLastUpdate = 0;
    private boolean isUpdating = false;
    private int ticksWithoutInput = 0;
    private static final int SIGNAL_LOSS_DELAY = 1;
    private int cachedInputSignal = 0;

    public RedstoneChainEntity(BlockPos pos, BlockState state) {
        super(MinecraftPlayground.REDSTONE_CHAIN_ENTITY.get(), pos, state);
    }

    public List<BlockPos> getConnections() {
        return connections;
    }

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

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (!(be instanceof RedstoneChainEntity chain)) return;
        if (level.isClientSide) return;

        chain.ticksSinceLastUpdate++;
        if (chain.ticksSinceLastUpdate >= UPDATE_INTERVAL) {
            chain.ticksSinceLastUpdate = 0;
            chain.updateSignalInNetwork();
        }
    }

    public int getSignal() {
        return getBlockState().getValue(RedstoneChainBlock.POWER);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    // ===== SERIALIZATION =====

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

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
}
