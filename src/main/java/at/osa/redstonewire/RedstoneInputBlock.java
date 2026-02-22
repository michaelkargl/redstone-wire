package at.osa.redstonewire;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A block that can be used to input redstone signals into the wire network.
 */
public class RedstoneInputBlock extends Block {

    /**
     * Block state property storing redstone power level (0-15).
     * 0 = unpowered, 15 = maximum power.
     */
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public RedstoneInputBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
    }

    /**
     * Defines which properties this block's state can have.
     * In Minecraft, blocks can have various "states" (like whether a door is open or closed).
     * @param builder The builder object used to register state properties
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    /**
     * Called when a neighboring block changes (placed, broken, or updated).
     *  Whenever a block next to this one changes, Minecraft calls this method to let us react.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        // if neighbor has a power signal
        // update the network with the new signal strength
        RedstoneWire.LOGGER.info("Neighbor changed for RedstoneInputBlock at pos: {}", pos);

        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }
}
