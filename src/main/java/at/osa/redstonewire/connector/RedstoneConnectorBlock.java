package at.osa.redstonewire.connector;

import at.osa.redstonewire.RedstoneWireBlock;
import at.osa.redstonewire.RedstoneWireBlockEntity;
import at.osa.redstonewire.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RedstoneConnectorBlock extends RedstoneWireBlock {

    public RedstoneConnectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new RedstoneConnectorBlockEntity(blockPos, blockState);
    }

    /**
     * Called when a player right-clicks this block while holding REDSTONE.
     * We only care about redstone — all other items fall through to default behavior.
     */
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

    /**
     * Handles the two-step connection flow when a player uses redstone on this block.
     * <p>
     * Step 1 (no saved position on redstone): save this block's position onto the item.
     * Step 2 (saved position exists): create a connection between the saved block
     * and this block, then clear the saved position.
     * <p>
     * The saved position is stored in the held item's CONNECTOR_LINK_DATA component as a
     * CompoundTag with keys "x", "y", "z".
     */
    private void handleWireItemUse(Level level, BlockPos clickedBlockPos, RedstoneConnectorBlockEntity connector,
                                   Player player, ItemStack wireItemStack) {
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
}
