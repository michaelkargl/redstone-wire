package at.osa.minecraftplayground;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Item used to connect RedstoneChainBlocks together with visible cables.
 * <p>
 * Usage:
 * 1. Shift-click on first chain block → Saves position to item
 * 2. Shift-click on second chain block → Creates bidirectional cable connection
 * 3. Shift-right-click in air → Clears saved position
 * <p>
 * The item stores the first clicked position in its NBT data (LINK_DATA component).
 */
public class RedstoneChainConnector extends Item {

    // ===== Configuration Constants =====
    /**
     * Maximum distance (in blocks) allowed between two connected chain blocks.
     * Connections beyond this distance will be rejected.
     */
    private static final int MAX_CONNECTION_DISTANCE = 20;

    /**
     * Maximum number of cable connections allowed per chain block.
     * Prevents visual clutter and performance issues.
     */
    private static final int MAX_CONNECTIONS_PER_CHAIN = 5;

    public RedstoneChainConnector(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getOrDefault(MinecraftPlayground.LINK_DATA, new CompoundTag());
                if (tag.contains("LinkX")) {
                    CompoundTag newTag = tag.copy();
                    newTag.remove("LinkX");
                    newTag.remove("LinkY");
                    newTag.remove("LinkZ");
                    stack.set(MinecraftPlayground.LINK_DATA, newTag.isEmpty() ? null : newTag);

                    player.displayClientMessage(
                            Component.translatable("item.minecraftplayground.chain_connector.cleared")
                                    .withStyle(ChatFormatting.YELLOW),
                            true
                    );
                }
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(clickedPos);
        if (!(be instanceof RedstoneChainEntity chain)) return InteractionResult.PASS;

        // Check for Ctrl+click to clear all connections on this block
        if (player.isCrouching()) {
            return handleShiftClick(level, player, clickedPos, chain, stack);
        }

        return InteractionResult.PASS;
    }

    /**
     * Handles shift-clicking on a chain block with the connector item.
     * <p>
     * Two-step process:
     * 1. First click: Saves the block position to the item
     * 2. Second click: Creates connection between saved position and clicked position
     */
    private InteractionResult handleShiftClick(Level level, Player player, BlockPos clickedPos,
                                              RedstoneChainEntity chain, ItemStack stack) {
        // Get saved position data from item (if any)
        CompoundTag savedData = stack.getOrDefault(MinecraftPlayground.LINK_DATA, new CompoundTag());

        // Check if this is the second click (completing a connection)
        if (hasSavedPosition(savedData)) {
            return handleSecondClick(level, player, clickedPos, chain, stack, savedData);
        } else {
            return handleFirstClick(level, player, clickedPos, chain, stack);
        }
    }

    /**
     * Checks if the item has a saved position stored.
     */
    private boolean hasSavedPosition(CompoundTag tag) {
        return tag.contains("LinkX");
    }

    /**
     * Handles the first click: saves the block position to the item.
     */
    private InteractionResult handleFirstClick(Level level, Player player, BlockPos clickedPos,
                                              RedstoneChainEntity chain, ItemStack stack) {
        // Check if chain already has max connections
        if (chain.getConnections().size() >= MAX_CONNECTIONS_PER_CHAIN) {
            showMaxConnectionsError(level, player);
            return InteractionResult.FAIL;
        }

        // Save position to item (server-side only)
        if (!level.isClientSide) {
            savePositionToItem(stack, clickedPos);
            showSavedMessage(player, clickedPos);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Handles the second click: creates connection between saved position and clicked position.
     */
    private InteractionResult handleSecondClick(Level level, Player player, BlockPos clickedPos,
                                               RedstoneChainEntity chain, ItemStack stack,
                                               CompoundTag savedData) {
        // Read saved position
        BlockPos startPos = readPositionFromTag(savedData);

        // Clear the saved position from item
        clearSavedPosition(stack);

        // Validate connection
        ConnectionValidation validation = validateConnection(level, player, startPos, clickedPos, chain);
        if (!validation.isValid()) {
            return InteractionResult.FAIL;
        }

        // Create the connection (server-side only)
        if (!level.isClientSide) {
            createBidirectionalConnection(level, player, startPos, clickedPos, chain, stack);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Reads a BlockPos from NBT tag.
     */
    private BlockPos readPositionFromTag(CompoundTag tag) {
        return new BlockPos(
                tag.getInt("LinkX"),
                tag.getInt("LinkY"),
                tag.getInt("LinkZ")
        );
    }

    /**
     * Saves a position to the item's data component.
     */
    private void savePositionToItem(ItemStack stack, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("LinkX", pos.getX());
        tag.putInt("LinkY", pos.getY());
        tag.putInt("LinkZ", pos.getZ());
        stack.set(MinecraftPlayground.LINK_DATA, tag);
    }

    /**
     * Clears the saved position from the item.
     */
    private void clearSavedPosition(ItemStack stack) {
        stack.set(MinecraftPlayground.LINK_DATA, null);
    }

    /**
     * Validates that a connection can be created between two positions.
     */
    private ConnectionValidation validateConnection(Level level, Player player,
                                                    BlockPos startPos, BlockPos clickedPos,
                                                    RedstoneChainEntity targetChain) {
        // Can't connect to self
        if (startPos.equals(clickedPos)) {
            return ConnectionValidation.invalid();
        }

        // Check distance
        double distanceSq = startPos.distSqr(clickedPos);
        double maxDistSq = MAX_CONNECTION_DISTANCE * MAX_CONNECTION_DISTANCE;
        if (distanceSq > maxDistSq) {
            if (!level.isClientSide) {
                showDistanceError(player, distanceSq);
            }
            return ConnectionValidation.invalid();
        }

        // Check if target has max connections
        if (targetChain.getConnections().size() >= MAX_CONNECTIONS_PER_CHAIN) {
            showMaxConnectionsError(level, player);
            return ConnectionValidation.invalid();
        }

        return ConnectionValidation.valid(distanceSq);
    }

    /**
     * Creates bidirectional cable connection between two chain blocks.
     */
    private void createBidirectionalConnection(Level level, Player player,
                                              BlockPos startPos, BlockPos endPos,
                                              RedstoneChainEntity endChain, ItemStack stack) {
        // Get the starting chain's block entity
        BlockEntity startBe = level.getBlockEntity(startPos);
        if (!(startBe instanceof RedstoneChainEntity startChain)) {
            return;
        }

        // Create connections in both directions
        startChain.addConnection(endPos);
        startChain.setChanged();
        level.sendBlockUpdated(startPos, startChain.getBlockState(), startChain.getBlockState(), 3);

        endChain.addConnection(startPos);
        endChain.setChanged();
        level.sendBlockUpdated(endPos, endChain.getBlockState(), endChain.getBlockState(), 3);

        // Show success message
        double distance = Math.sqrt(startPos.distSqr(endPos));
        showConnectionSuccessMessage(player, startPos, endPos, (int) distance);

        // Consume one connector item (unless in creative mode)
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    // ===== User Feedback Messages =====

    private void showMaxConnectionsError(Level level, Player player) {
        if (!level.isClientSide) {
            player.displayClientMessage(
                    Component.translatable("item.minecraftplayground.chain_connector.max_connections")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    private void showDistanceError(Player player, double distanceSq) {
        int actualDistance = (int) Math.sqrt(distanceSq);
        player.displayClientMessage(
                Component.translatable("item.minecraftplayground.chain_connector.too_far",
                        MAX_CONNECTION_DISTANCE, actualDistance)
                        .withStyle(ChatFormatting.RED),
                true
        );
    }

    private void showSavedMessage(Player player, BlockPos pos) {
        player.displayClientMessage(
                Component.translatable("item.minecraftplayground.chain_connector.saved",
                        formatBlockPos(pos))
                        .withStyle(ChatFormatting.AQUA),
                true
        );
    }

    private void showConnectionSuccessMessage(Player player, BlockPos start, BlockPos end, int distance) {
        player.displayClientMessage(
                Component.translatable("item.minecraftplayground.chain_connector.connected",
                        formatBlockPos(start), formatBlockPos(end), distance)
                        .withStyle(ChatFormatting.GREEN),
                true
        );
    }

    /**
     * Simple validation result holder.
     */
    private record ConnectionValidation(boolean isValid, double distance) {
        static ConnectionValidation valid(double distance) {
            return new ConnectionValidation(true, distance);
        }

        static ConnectionValidation invalid() {
            return new ConnectionValidation(false, 0);
        }
    }

    private static Component formatBlockPos(BlockPos pos) {
        return Component.literal("(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
    }

    /**
     * Adds tooltip text that appears when the player hovers over this item in their inventory.
     *
     * Purpose: Shows the player helpful information about the connector's current state
     *
     * What happens inside:
     * 1. Retrieves the NBT data from the item stack (where saved position is stored)
     * 2. Checks if there's a saved position (LinkX exists in the data):
     *
     *    IF POSITION IS SAVED:
     *    a. Reads the X, Y, Z coordinates from the NBT data
     *    b. Creates a BlockPos from those coordinates
     *    c. Adds a gray tooltip line showing the saved position in short format
     *
     *    IF NO POSITION IS SAVED:
     *    a. Adds a dark gray tooltip line saying "No saved point"
     *
     * 3. Always adds a usage instruction line at the end
     *
     * The tooltip helps players remember:
     * - Whether they've started a connection (first click done)
     * - Where the first point was located
     * - How to use the item
     *
     * @param stack The item stack being hovered over
     * @param context Additional context (world, etc.) - usually not needed for simple tooltips
     * @param tooltip The list of tooltip lines to add to (method adds to this list)
     * @param flag Flags indicating when to show the tooltip (normal vs advanced)
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(MinecraftPlayground.LINK_DATA, new CompoundTag());
        if (tag.contains("LinkX")) {
            BlockPos pos = new BlockPos(tag.getInt("LinkX"), tag.getInt("LinkY"), tag.getInt("LinkZ"));
            tooltip.add(Component.translatable("item.minecraftplayground.chain_connector.saved_point",
                    pos.toShortString()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.minecraftplayground.chain_connector.no_saved_point")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("item.minecraftplayground.chain_connector.usage")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
