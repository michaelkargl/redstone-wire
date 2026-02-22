package at.osa.redstonewire;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A block that can be used to output redstone signals from the wire network.
 */
public class RedstoneOutputBlock extends Block {

    /**
     * Block state property storing redstone power level (0-15).
     * 0 = unpowered, 15 = maximum power.
     */
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public RedstoneOutputBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
    }

    /**
     * Defines which properties this block's state can have.
     * @param builder The builder object used to register state properties
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    /**
     * Called when a neighboring block changes (placed, broken, or updated).
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        RedstoneWire.LOGGER.info("Neighbor changed for RedstoneOutputBlock at pos: {}", pos);

        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(POWER) > 0;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }
}
