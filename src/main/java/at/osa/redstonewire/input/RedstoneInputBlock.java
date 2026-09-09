package at.osa.redstonewire.input;

import at.osa.redstonewire.RedstoneWireBlock;
import at.osa.redstonewire.connector.RedstoneConnectorBlockEntity;
import at.osa.redstonewire.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block that can be used to input redstone signals into the wire network.
 * Linked to ConnectorBlocks via the two-click REDSTONE item pattern.
 */
public class RedstoneInputBlock extends RedstoneWireBlock {

    /**
     * Block state property storing redstone power level (0-15).
     * 0 = unpowered, 15 = maximum power.
     */
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public RedstoneInputBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWER, 0)
                .setValue(FACING, Direction.NORTH));
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
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    /**
     * Tells if this block has an analog output signal.
     * Analog signals are used for things like redstone comparators.
     */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
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

    /**
     * Called when a neighboring block changes (placed, broken, or updated).
     * Reads the new power level, short-circuits if unchanged, updates our own state,
     * then pushes the signal into every linked ConnectorBlockEntity.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean isMoving) {
        if (level.isClientSide()) {
            return;
        }

        int newPower = level.getBestNeighborSignal(pos);
        int currentPower = state.getValue(POWER);
        if (newPower == currentPower) {
            return;
        }

        var be = level.getBlockEntity(pos);
        if (!(be instanceof RedstoneInputBlockEntity inputBE)) return;

        // UPDATE_ALL notifies adjacent blocks to update their redstone signal, which is necessary for things like redstone lamps or comparators
        level.setBlock(pos, state.setValue(POWER, newPower), Block.UPDATE_ALL);

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
    protected InteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!heldItem.is(Items.REDSTONE)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        var be = level.getBlockEntity(pos);
        if (!(be instanceof RedstoneInputBlockEntity)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!level.isClientSide()) {
            var linkData = heldItem.getOrDefault(ModDataComponents.CONNECTOR_LINK_DATA, new CompoundTag());
            if (!hasSavedPosition(linkData)) {
                player.displayClientMessage(
                        Component.literal("Right-click a ConnectorBlock first to select it").withStyle(ChatFormatting.YELLOW),
                        true);
            } else {
                var connectorPos = readPositionFromTag(linkData);
                clearSavedPosition(heldItem);

                var connectorBE = level.getBlockEntity(connectorPos);
                if (connectorBE instanceof RedstoneConnectorBlockEntity connector) {
                    connector.createBidirectionalConnection(level, connectorPos, pos, player);
                } else {
                    player.displayClientMessage(
                            Component.literal("Saved position is not a ConnectorBlock").withStyle(ChatFormatting.RED),
                            true);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }
}
