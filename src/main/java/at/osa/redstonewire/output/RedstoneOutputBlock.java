package at.osa.redstonewire.output;

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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block that outputs redstone signals received from the wire network.
 * Linked to ConnectorBlocks via the two-click REDSTONE item pattern.
 */
public class RedstoneOutputBlock extends RedstoneWireBlock {

    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public RedstoneOutputBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWER, 0)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneOutputBlockEntity(pos, state);
    }

    /**
     * Registers block state properties.
     * "super" must be called first. StateDefinition.Builder assigns numeric IDs to property combinations based on
     * insertion order. Changing that order after a world was saved would corrupt any blocks already placed.
     * @param builder
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(POWER) > 0;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }

    /**
     * Called when a player right-clicks this block while holding REDSTONE.
     * Implements Step 2 of the two-click linking flow:
     * Step 1: player clicks a ConnectorBlock → saves connector pos to item data
     * Step 2: player clicks this OutputBlock → reads saved pos, creates link
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!heldItem.is(Items.REDSTONE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        var be = level.getBlockEntity(pos);
        if (!(be instanceof RedstoneOutputBlockEntity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

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

        return ItemInteractionResult.SUCCESS;
    }
}
