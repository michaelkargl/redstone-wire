package at.osa.minecraftplayground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * A redstone chain block that can transmit redstone signals.
 * Supports both adjacent block connections (traditional) and long-range wire connections via RedstoneChainEntity.
 */
public class RedstoneChainBlock extends Block implements EntityBlock {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    // Small voxel shape for the chain block (like a chain segment)
    private static final VoxelShape SHAPE = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);

    public RedstoneChainBlock(Properties properties) {
        super(properties.noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(POWER) > 0;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Check if we have a block entity with wire connections
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneChainEntity chain) {
            return chain.getSignal();
        }
        return state.getValue(POWER);
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && !(neighborBlock instanceof RedstoneChainBlock)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RedstoneChainEntity chain) {
                // Use the network update for wire-connected blocks
                chain.updateSignalInNetwork();
            } else {
                // Fallback to traditional adjacent block behavior
                level.scheduleTick(pos, this, 1);
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Check if we have a block entity - if so, let it handle updates
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneChainEntity) {
            return; // Let the entity handle network updates
        }

        // Traditional adjacent block network
        Set<BlockPos> network = findNetwork(level, pos);
        int power = findPower(level, network);

        boolean changed = false;
        for (BlockPos p : network) {
            BlockState s = level.getBlockState(p);
            if (s.getValue(POWER) != power) {
                level.setBlock(p, s.setValue(POWER, power), 2);
                changed = true;
            }
        }

        if (changed) {
            for (BlockPos p : network) {
                level.updateNeighborsAt(p, this);
            }
        }
    }

    private Set<BlockPos> findNetwork(Level level, BlockPos start) {
        Set<BlockPos> network = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (network.contains(p)) continue;
            if (!(level.getBlockState(p).getBlock() instanceof RedstoneChainBlock)) continue;
            network.add(p);
            for (Direction d : Direction.values()) {
                queue.add(p.relative(d));
            }
        }
        return network;
    }

    private int findPower(Level level, Set<BlockPos> network) {
        int max = 0;
        for (BlockPos p : network) {
            for (Direction d : Direction.values()) {
                BlockPos neighbor = p.relative(d);
                if (network.contains(neighbor)) continue;
                max = Math.max(max, level.getSignal(neighbor, d.getOpposite()));
                if (max >= 15) return 15;
            }
        }
        return max;
    }

    // ===== EntityBlock implementation =====

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneChainEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == MinecraftPlayground.REDSTONE_CHAIN_ENTITY.get() ? RedstoneChainEntity::tick : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RedstoneChainEntity chain) {
                // Remove connections from other chains that point to this one
                for (BlockPos otherPos : new ArrayList<>(chain.getConnections())) {
                    BlockEntity otherBe = level.getBlockEntity(otherPos);
                    if (otherBe instanceof RedstoneChainEntity otherChain) {
                        otherChain.removeConnection(pos);
                    }
                }
                chain.clearConnections();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Enable analog comparator output
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(POWER);
    }
}
