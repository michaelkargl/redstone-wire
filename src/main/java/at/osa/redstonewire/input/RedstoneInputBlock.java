package at.osa.redstonewire.input;


import at.osa.redstonewire.init.ModDataComponents;
import at.osa.redstonewire.RedstoneWireBlockEntity;
import at.osa.redstonewire.connector.RedstoneConnectorBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * A block that can be used to input redstone signals into the wire network.
 * Linked to ConnectorBlocks via the two-click REDSTONE item pattern.
 */
public class RedstoneInputBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Block state property storing redstone power level (0-15).
     * 0 = unpowered, 15 = maximum power.
     */
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    // Defines the collision/hitbox. It uses pixel coordinates where
    // 0,0,0 is the bottom left corner of the block
    // 16,16,16 is the top right corner of the block
    // 8,8,8 is the center of the block
    //
    //    ││   ← shaft (2px wide, Y 5-11)
    //  │    │  ← base ring (6px wide, Y 2-5)
    // │──────│  ← flat slab (16px wide, Y 0-2)
    private static final VoxelShape SHAPE = Shapes.or(
            // flat base slab
            // Spans full width and depth with a height of 2
            Block.box(0, 0, 0, 16, 2, 16),  // flat base slab
            // base ring
            Block.box(5, 2, 5, 11, 5, 11),  // antenna base ring
            // shaft
            Block.box(7, 5, 7, 9, 11, 9)    // antenna shaft
    );

    public RedstoneInputBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWER, 0)
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @NotNull BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneInputBlockEntity(pos, state);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return 0;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return 0;
    }

    /**
     * Tells if this block has an analog output signal.
     * Analog signals are used for things like redstone lamps and comparators.
     *
     * @param state
     * @return
     */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(POWER);
    }

    /**
     * Defines which properties this block's state can have.
     * In Minecraft, blocks can have various "states" (like whether a door is open or closed).
     *
     * @param builder The builder object used to register state properties
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
        builder.add(FACING);
    }

    /**
     * Called when a block is destroyed (newState is Blocks.AIR)
     * or whenever certain block properties change (FACING changes from nord to east)
     *
     * @param state
     * @param level
     * @param pos
     * @param newState
     * @param movedByPiston
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean blockReplaced = !state.is(newState.getBlock());
        if (blockReplaced) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RedstoneWireBlockEntity wireEntity) {
                var player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16, false);
                wireEntity.removeBidirectionalConnections(level, player, pos);
            }
        }

        // must be last, triggers the actual removal
        // Why not inside the if guard? Even if the block has not been replaced, there is still a removal triggered for
        // this block that must run its course. Our guard protects our cleanup logic, their guard protects their cleanup
        // logic. Both must run for proper cleanup.
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Called when a neighboring block changes (placed, broken, or updated).
     * Reads the new power level, short-circuits if unchanged, updates our own state,
     * then pushes the signal into every linked ConnectorBlockEntity.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        int newPower = level.getBestNeighborSignal(pos);
        int currentPower = state.getValue(POWER);
        if (newPower == currentPower) {
            return;
        }

        // UPDATE_ALL notifies adjacent blocks to update their redstone signal, which is necessary for things like redstone lamps or comparators
        level.setBlock(pos, state.setValue(POWER, newPower), Block.UPDATE_ALL);

        var be = level.getBlockEntity(pos);
        if (!(be instanceof RedstoneInputBlockEntity inputBE)) return;

        for (BlockPos connectorPos : inputBE.getConnections()) {
            var connectorBE = level.getBlockEntity(connectorPos);
            if (connectorBE instanceof RedstoneConnectorBlockEntity connector) {
                connector.propagateSignal(newPower, level);
            }
        }
    }

    /**
     * Called when a player right-clicks this block while holding REDSTONE.
     * Implements Step 2 of the two-click linking flow:
     * Step 1: player clicks a ConnectorBlock with REDSTONE → saves connector pos to item data
     * Step 2: player clicks this InputBlock with REDSTONE → reads saved pos, creates link
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!heldItem.is(Items.REDSTONE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        var be = level.getBlockEntity(pos);
        if (!(be instanceof RedstoneInputBlockEntity inputBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            var linkData = heldItem.getOrDefault(ModDataComponents.CONNECTOR_LINK_DATA, new CompoundTag());
            if (!hasSavedPosition(linkData)) {
                player.displayClientMessage(
                        Component.literal("Right-click a ConnectorBlock first to select it").withStyle(ChatFormatting.YELLOW),
                        true);
            } else {
                var connectorPos = readPositionFromTag(linkData);
                clearSavedPosition(heldItem);

                var connectorBE = level.getBlockEntity(connectorPos);
                if (connectorBE instanceof RedstoneWireBlockEntity connector) {
                    connector.createBidirectionalConnection(level, connectorPos, pos, player);
                } else {
                    player.displayClientMessage(
                            Component.literal("Saved position is not a ConnectorBlock").withStyle(ChatFormatting.RED),
                            true);
                }
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    private boolean hasSavedPosition(CompoundTag tag) {
        return tag.contains("x") && tag.contains("y") && tag.contains("z");
    }

    private BlockPos readPositionFromTag(CompoundTag tag) {
        return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    private void clearSavedPosition(ItemStack stack) {
        stack.set(ModDataComponents.CONNECTOR_LINK_DATA, null);
    }
}
