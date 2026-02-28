package at.osa.redstonewire.input;

import at.osa.redstonewire.init.ModBlockEntityTypes;
import at.osa.redstonewire.RedstoneWireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;


/**
 * Stores which ConnectorBlockEntity positions this InputBlock is wired to.
 * Simpler than RedstoneConnectorBlockEntity — no routing cache needed here;
 * signal propagation is delegated immediately to each linked connector.
 */
public class RedstoneInputBlockEntity extends RedstoneWireBlockEntity {
    public RedstoneInputBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.REDSTONE_INPUT_ENTITY.get(), pos, blockState);
    }
}
