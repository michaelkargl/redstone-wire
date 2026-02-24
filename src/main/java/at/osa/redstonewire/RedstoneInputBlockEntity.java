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


/**
 * Stores which ConnectorBlockEntity positions this InputBlock is wired to.
 * Simpler than RedstoneConnectorBlockEntity — no routing cache needed here;
 * signal propagation is delegated immediately to each linked connector.
 */
public class RedstoneInputBlockEntity extends RedstoneWireBlockEntity {
    public RedstoneInputBlockEntity(BlockPos pos, BlockState blockState) {
        super(RedstoneWire.REDSTONE_INPUT_ENTITY.get(), pos, blockState);
    }
}
