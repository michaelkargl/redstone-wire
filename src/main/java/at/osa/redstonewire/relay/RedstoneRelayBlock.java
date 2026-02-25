package at.osa.redstonewire.relay;

import at.osa.redstonewire.RedstoneWire;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RedstoneRelayBlock extends Block implements EntityBlock {

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

    public RedstoneRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneConnectorBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
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

    private void handleWireItemUse(Level level, BlockPos clickedBlockPos, RedstoneConnectorBlockEntity connector,
                                   Player player, ItemStack wireItemStack) {
        var linkData = wireItemStack.getOrDefault(RedstoneWire.CONNECTOR_LINK_DATA, new CompoundTag());
        if (hasSavedPosition(linkData)) {
            handleSecondClick(level, wireItemStack, linkData, clickedBlockPos, player);
        } else {
            handleFirstClick(level, wireItemStack, clickedBlockPos, player);
        }
    }

    private void handleFirstClick(Level level, ItemStack itemStack, BlockPos clickedBlockPosition, Player player) {
        if (level.isClientSide) return;

        savePositionToItem(itemStack, clickedBlockPosition);
        player.displayClientMessage(
                Component.literal("Selected " + clickedBlockPosition.toShortString() + " as source for connection").withStyle(ChatFormatting.GREEN),
                true);
    }

    private void handleSecondClick(Level level, ItemStack itemStack, CompoundTag linkData,
                                   BlockPos clickedBlockPosition, Player player) {
        if (level.isClientSide) return;

        var startPosition = readPositionFromTag(linkData);
        clearSavedPosition(itemStack);
        createBidirectionalConnection(level, startPosition, clickedBlockPosition, player);
    }

    private void createBidirectionalConnection(Level level, BlockPos startPos, BlockPos endPos, Player player) {
        var startBE = level.getBlockEntity(startPos);
        var endBE = level.getBlockEntity(endPos);

        if (startBE instanceof RedstoneConnectorBlockEntity startConnector
                && endBE instanceof RedstoneConnectorBlockEntity endConnector) {
            startConnector.addConnection(endConnector.getBlockPos());
            endConnector.addConnection(startConnector.getBlockPos());
            player.displayClientMessage(
                    Component.literal("Connected " + startConnector.getBlockPos().toShortString() + " to " + endConnector.getBlockPos().toShortString()).withStyle(ChatFormatting.GREEN),
                    true);
        }
    }

    private boolean hasSavedPosition(CompoundTag tag) {
        return tag.contains("x") && tag.contains("y") && tag.contains("z");
    }

    private BlockPos readPositionFromTag(CompoundTag tag) {
        return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    private void savePositionToItem(ItemStack itemStack, BlockPos position) {
        var positionTag = new CompoundTag();
        positionTag.putInt("x", position.getX());
        positionTag.putInt("y", position.getY());
        positionTag.putInt("z", position.getZ());
        itemStack.set(RedstoneWire.CONNECTOR_LINK_DATA, positionTag);
    }

    private void clearSavedPosition(ItemStack stack) {
        stack.set(RedstoneWire.CONNECTOR_LINK_DATA, null);
    }
}
