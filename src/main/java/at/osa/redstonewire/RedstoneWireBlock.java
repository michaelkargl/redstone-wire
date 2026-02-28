package at.osa.redstonewire;

import at.osa.redstonewire.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared base class for all three redstone wire mod blocks (Input, Output, Connector).
 * <p>
 * Centralises:
 * <ul>
 *   <li>The antenna {@link VoxelShape} (slab + ring + shaft)</li>
 *   <li>The {@link #FACING} block-state property</li>
 *   <li>Connection cleanup on block removal</li>
 *   <li>Position-tag helpers for the two-click REDSTONE linking flow</li>
 * </ul>
 * Subclasses that add extra state (e.g. {@code POWER}) must call
 * {@code super.createBlockStateDefinition(builder)} before adding their own properties.
 */
public abstract class RedstoneWireBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Y coordinate of the antenna shaft tip in model space (block pixels / 16).
     * Used by block entity renderers to attach cable endpoints at the correct height.
     */
    public static final float ANTENNA_TIP_Y = 11.0f / 16.0f;

    // Antenna shape — slab + base ring + shaft.
    //
    //    ││   ← shaft (2px wide, Y 5-11)
    //  │    │  ← base ring (6px wide, Y 2-5)
    // │──────│  ← flat slab (16px wide, Y 0-2)
    private static final VoxelShape ANTENNA_SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),  // flat base slab
            Block.box(5, 2, 5, 11, 5, 11),  // antenna base ring
            Block.box(7, 5, 7, 9, 11, 9)    // antenna shaft (tip at Y=11)
    );

    protected RedstoneWireBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ANTENNA_SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    /**
     * Registers {@link #FACING} as a block state property.
     * Subclasses that add more properties (e.g. {@code POWER}) must call
     * {@code super.createBlockStateDefinition(builder)} first.
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Tears down all wire connections when this block is replaced.
     * <p>
     * Must call super last — the block entity must still be accessible
     * during cleanup, and super.onRemove() is what removes it.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RedstoneWireBlockEntity wireEntity) {
                // player is null when broken by piston/explosion — removeBidirectionalConnections
                // already null-checks before sending feedback messages.
                wireEntity.removeBidirectionalConnections(level, null, pos);
            }
        }

        // Must be last. Even when the block is not replaced (only a state change),
        // super.onRemove must still run its own cleanup. Both guards are independent.
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ── Position-tag helpers for the two-click REDSTONE linking flow ─────────
    // The held REDSTONE item carries a CONNECTOR_LINK_DATA component (CompoundTag)
    // with keys "x", "y", "z" to persist the first-clicked block position.

    protected static boolean hasSavedPosition(CompoundTag tag) {
        return tag.contains("x") && tag.contains("y") && tag.contains("z");
    }

    protected static BlockPos readPositionFromTag(CompoundTag tag) {
        return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    protected static void clearSavedPosition(ItemStack stack) {
        stack.set(ModDataComponents.CONNECTOR_LINK_DATA, null);
    }

    protected static void savePositionToItem(ItemStack itemStack, BlockPos position) {
        var tag = new CompoundTag();
        tag.putInt("x", position.getX());
        tag.putInt("y", position.getY());
        tag.putInt("z", position.getZ());
        itemStack.set(ModDataComponents.CONNECTOR_LINK_DATA, tag);
    }
}
