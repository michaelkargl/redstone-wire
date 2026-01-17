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
 * Item used to connect RedstoneChainBlocks together.
 * Shift-click on first chain to start connection, shift-click on second chain to complete.
 * Shift-right-click in air to clear saved position.
 */
public class RedstoneChainConnector extends Item {

    private static final int MAX_CONNECTION_DISTANCE = 20;

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

    private InteractionResult handleShiftClick(Level level, Player player, BlockPos clickedPos, RedstoneChainEntity chain, ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(MinecraftPlayground.LINK_DATA, new CompoundTag());

        if (!level.isClientSide && chain.getConnections().size() >= 3) {
            player.displayClientMessage(
                    Component.translatable("item.minecraftplayground.chain_connector.max_connections")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (tag.contains("LinkX")) {
            BlockPos startPos = new BlockPos(tag.getInt("LinkX"), tag.getInt("LinkY"), tag.getInt("LinkZ"));

            // Clear the saved position
            CompoundTag newTag = new CompoundTag();
            stack.set(MinecraftPlayground.LINK_DATA, null);

            if (!startPos.equals(clickedPos)) {
                double distanceSq = startPos.distSqr(clickedPos);
                if (distanceSq > MAX_CONNECTION_DISTANCE * MAX_CONNECTION_DISTANCE) {
                    if (!level.isClientSide) {
                        player.displayClientMessage(
                                Component.translatable("item.minecraftplayground.chain_connector.too_far",
                                        MAX_CONNECTION_DISTANCE, (int) Math.sqrt(distanceSq))
                                        .withStyle(ChatFormatting.RED),
                                true
                        );
                    }
                    return InteractionResult.FAIL;
                }

                BlockEntity startBe = level.getBlockEntity(startPos);
                if (startBe instanceof RedstoneChainEntity startChain) {
                    if (!level.isClientSide) {
                        // Create bidirectional connection
                        startChain.addConnection(clickedPos);
                        startChain.setChanged();
                        level.sendBlockUpdated(startPos, startChain.getBlockState(), startChain.getBlockState(), 3);

                        chain.addConnection(startPos);
                        chain.setChanged();
                        level.sendBlockUpdated(clickedPos, chain.getBlockState(), chain.getBlockState(), 3);

                        player.displayClientMessage(
                                Component.translatable("item.minecraftplayground.chain_connector.connected",
                                        formatBlockPos(startPos), formatBlockPos(clickedPos),
                                        (int) Math.sqrt(distanceSq))
                                        .withStyle(ChatFormatting.GREEN),
                                true
                        );

                        // Consume one connector item
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        } else {
            // Check if this chain already has max connections
            if (chain.getConnections().size() >= 3) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("item.minecraftplayground.chain_connector.max_connections")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                }
                return InteractionResult.FAIL;
            }

            // Save the first position
            if (!level.isClientSide) {
                CompoundTag newTag = new CompoundTag();
                newTag.putInt("LinkX", clickedPos.getX());
                newTag.putInt("LinkY", clickedPos.getY());
                newTag.putInt("LinkZ", clickedPos.getZ());
                stack.set(MinecraftPlayground.LINK_DATA, newTag);

                player.displayClientMessage(
                        Component.translatable("item.minecraftplayground.chain_connector.saved",
                                formatBlockPos(clickedPos))
                                .withStyle(ChatFormatting.AQUA),
                        true
                );
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    private static Component formatBlockPos(BlockPos pos) {
        return Component.literal("(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
    }

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
