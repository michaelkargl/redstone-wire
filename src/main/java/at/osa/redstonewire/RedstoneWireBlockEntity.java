package at.osa.redstonewire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public abstract class RedstoneWireBlockEntity extends BlockEntity {
    protected final List<BlockPos> directConnections = new ArrayList<>();

    public RedstoneWireBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        var list = new ListTag();
        var origin = this.getBlockPos();
        for (var position : directConnections) {
            var posTag = new CompoundTag();
            posTag.putInt("x", position.getX() - origin.getX());
            posTag.putInt("y", position.getY() - origin.getY());
            posTag.putInt("z", position.getZ() - origin.getZ());
            list.add(posTag);
        }

        tag.put("Connections", list);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        var connections =
                tag.getList("Connections", ListTag.TAG_COMPOUND)
                        .stream()
                        .filter(t -> t instanceof CompoundTag)
                        .map(t -> (CompoundTag) t)
                        .map(positionTag -> this.getBlockPos().offset(
                                positionTag.getInt("x"),
                                positionTag.getInt("y"),
                                positionTag.getInt("z")))
                        .toList();

        this.directConnections.clear();
        this.directConnections.addAll(connections);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    protected void connectionAdded(BlockPos pos) {
    }

    protected void connectionRemoved(BlockPos pos) {
    }

    public void addConnection(BlockPos pos) {
        if (!directConnections.contains(pos)) {
            directConnections.add(pos);
            connectionAdded(pos);
            setChanged();
            syncToClient();
        }
    }

    public void removeConnection(BlockPos pos) {
        directConnections.remove(pos);
        connectionRemoved(pos);
        this.setChanged();
        this.syncToClient();
    }

    public List<BlockPos> getConnections() {
        return directConnections;
    }

    public int getSignal() {
        return 0;
    }

    private void syncToClient() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        var sendClientUpdateFlag = 0x10;
        var scheduleBlockRerenderFlag = 0x01;

        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), sendClientUpdateFlag | scheduleBlockRerenderFlag);
    }
}
