package at.osa.redstonewire.output;

import at.osa.redstonewire.init.ModBlockEntityTypes;
import at.osa.redstonewire.RedstoneWireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores which ConnectorBlockEntity positions this OutputBlock is wired to.
 * Signal is pushed into this block by the connector's propagateSignal; this entity
 * only needs to persist the wired links for cable rendering.
 */
public class RedstoneOutputBlockEntity extends RedstoneWireBlockEntity {
    public RedstoneOutputBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.REDSTONE_OUTPUT_ENTITY.get(), pos, blockState);
    }
}
