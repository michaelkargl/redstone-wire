package at.osa.redstonewire.connector;

import at.osa.redstonewire.ModDataComponents;
import at.osa.redstonewire.RedstoneWire;
import at.osa.redstonewire.RedstoneWireBlockEntity;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class RedstoneConnectorBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

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


    public RedstoneConnectorBlock(Properties properties) {

        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new RedstoneConnectorBlockEntity(blockPos, blockState);
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
     * Called when a player right-clicks this block while holding an item.
     * We only care about string — all other items fall through to default behavior.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        RedstoneWire.LOGGER.info("useItemOn called, item: {}", heldItem.getItem());

        if (!heldItem.is(Items.REDSTONE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        var be = level.getBlockEntity(pos);
        if (!(be instanceof RedstoneConnectorBlockEntity connector)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            handleWireItemUse(level, pos, connector, player, heldItem);
        }

        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Handles the two-step connection flow when a player uses string on this block.
     * <p>
     * Step 1 (no saved position on string): save this block's position onto the string item.
     * Step 2 (saved position exists on string): create a connection between the saved block
     * and this block, then clear the saved position from the string.
     * <p>
     * The saved position is stored in the string item's LINK_DATA component as a CompoundTag
     * with keys "LinkX", "LinkY", "LinkZ".
     * <p>
     * Use RedstoneChainConnector.java as a reference — the handleClick / handleFirstClick /
     * handleSecondClick methods show the exact same pattern.
     */
    private void handleWireItemUse(Level level, BlockPos clickedBlockPos, RedstoneConnectorBlockEntity connector,
                                   Player player, ItemStack wireItemStack) {
        // does our string item contain any saved position data from the previous click?
        var firstClickConnectorLinkData = wireItemStack.getOrDefault(ModDataComponents.CONNECTOR_LINK_DATA, new CompoundTag());
        if (hasSavedPosition(firstClickConnectorLinkData)) {
            handleSecondClick(level, wireItemStack, firstClickConnectorLinkData, clickedBlockPos, player);
        } else {
            handleFirstClick(level, wireItemStack, clickedBlockPos, player);
        }
    }

    private void handleFirstClick(Level level, ItemStack itemStack, BlockPos clickedBlockPosition, Player player) {
        if (level.isClientSide) {
            return;
        }

        savePositionToItem(itemStack, clickedBlockPosition);

        player.displayClientMessage(
                Component.literal("Selected " + clickedBlockPosition.toShortString() + " as source for connection").withStyle(ChatFormatting.GREEN),
                true);
    }

    private void savePositionToItem(ItemStack itemStack, BlockPos position) {
        var positionTag = new CompoundTag();
        positionTag.putInt("x", position.getX());
        positionTag.putInt("y", position.getY());
        positionTag.putInt("z", position.getZ());
        itemStack.set(ModDataComponents.CONNECTOR_LINK_DATA, positionTag);
    }

    private void handleSecondClick(
            Level level,
            ItemStack itemStack,
            CompoundTag connectorLinkData,
            BlockPos clickedBlockPosition,
            Player player) {

        if (level.isClientSide) {
            return;
        }

        var startPosition = readPositionFromTag(connectorLinkData);

        clearSavedPosition(itemStack);

        // TODO kami: validate if connection is valid

        var blockEntity = level.getBlockEntity(startPosition);
        if (blockEntity instanceof RedstoneWireBlockEntity wireNodeEntity) {
            wireNodeEntity.createBidirectionalConnection(level, startPosition, clickedBlockPosition, player);
        }
    }

    private boolean hasSavedPosition(CompoundTag tag) {
        return tag.contains("x") && tag.contains("y") && tag.contains("z");
    }

    private BlockPos readPositionFromTag(CompoundTag tag) {
        return new BlockPos(
                tag.getInt("x"),
                tag.getInt("y"),
                tag.getInt("z")
        );
    }

    private void clearSavedPosition(ItemStack stack) {
        stack.set(ModDataComponents.CONNECTOR_LINK_DATA, null);
    }


}