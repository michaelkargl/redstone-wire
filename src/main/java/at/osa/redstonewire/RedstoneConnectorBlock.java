package at.osa.redstonewire;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RedstoneConnectorBlock extends Block implements EntityBlock {
    public RedstoneConnectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new RedstoneConnectorBlockEntity(blockPos, blockState);
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
        var firstClickConnectorLinkData = wireItemStack.getOrDefault(RedstoneWire.CONNECTOR_LINK_DATA, new CompoundTag());
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
        itemStack.set(RedstoneWire.CONNECTOR_LINK_DATA, positionTag);
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

        createBidirectionalConnection(level, startPosition, clickedBlockPosition, player);
    }

    private void createBidirectionalConnection(Level level, BlockPos startPos, BlockPos endPos, Player player) {
        var startBlockEntity = level.getBlockEntity(startPos);
        var endBlockEntity = level.getBlockEntity(endPos);

        if (startBlockEntity instanceof RedstoneConnectorBlockEntity startConnector
                && endBlockEntity instanceof RedstoneConnectorBlockEntity endConnector) {
            createConnection(startConnector, endConnector, player);
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
        stack.set(RedstoneWire.CONNECTOR_LINK_DATA, null);
    }

    /**
     * Creates a bidirectional connection between two connector blocks.
     */
    private void createConnection(
            RedstoneConnectorBlockEntity connectorSource,
            RedstoneConnectorBlockEntity connectorTarget,
            Player player
    ) {
        connectorSource.addConnection(connectorTarget.getBlockPos());
        connectorTarget.addConnection(connectorSource.getBlockPos());

        player.displayClientMessage(
                Component.literal("Connected " + connectorSource.getBlockPos().toShortString() + " to " + connectorTarget.getBlockPos().toShortString()).withStyle(ChatFormatting.GREEN),
                true);
    }
}