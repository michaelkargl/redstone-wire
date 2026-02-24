package at.osa.redstonewire.connector;

import at.osa.redstonewire.output.RedstoneOutputBlock;
import at.osa.redstonewire.output.RedstoneOutputBlockEntity;
import at.osa.redstonewire.RedstoneWire;
import at.osa.redstonewire.RedstoneWireBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class RedstoneConnectorBlockEntity extends RedstoneWireBlockEntity {

    private final Set<BlockPos> reachableOutputs = new HashSet<>();
    private boolean cacheDirty = true;

    public RedstoneConnectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(RedstoneWire.REDSTONE_CONNECTOR_ENTITY.get(), pos, blockState);
    }

    @Override
    protected void connectionAdded(BlockPos pos) {
        this.markNetworkDirty(new HashSet<>());
    }

    @Override
    protected void connectionRemoved(BlockPos pos) {
        this.markNetworkDirty(new HashSet<>());
    }

    /**
     * Hot path — called up to 20×/sec by clock signals.
     * Rebuilds the output cache lazily (once per topology change), then sets
     * power on every reachable OutputBlock in one pass.
     */
    public void propagateSignal(int power, Level level) {
        if (cacheDirty) {
            rebuildOutputCache(level);
        }

        for (BlockPos outputPos : reachableOutputs) {
            BlockState state = level.getBlockState(outputPos);
            level.setBlock(outputPos, state.setValue(RedstoneOutputBlock.POWER, power), Block.UPDATE_ALL);
        }
    }

    /**
     * Entry point for cache rebuild. Clears the list, starts DFS, marks cache clean.
     */
    public void rebuildOutputCache(Level level) {
        reachableOutputs.clear();
        rebuildOutputCache(level, new HashSet<>(), reachableOutputs);
        cacheDirty = false;
    }

    /**
     * DFS worker — same structural shape as markNetworkDirty.
     * Visits every connector reachable via directConnections and appends any adjacent
     * OutputBlock positions to the shared accumulator.
     */
    public void rebuildOutputCache(Level level, Set<BlockPos> visited, Set<BlockPos> outOutputBlocks) {
        visited.add(this.getBlockPos());

        for (var direction : Direction.values()) {
            var neighbor = this.getBlockPos().relative(direction);
            var neighborState = level.getBlockState(neighbor);
            var block = neighborState.getBlock();
            if (block instanceof RedstoneOutputBlock) {
                outOutputBlocks.add(neighbor);
            }
        }

        for (var neighbor : directConnections) {
            if (visited.contains(neighbor)) {
                continue;
            }

            var blockEntity = level.getBlockEntity(neighbor);
            if (blockEntity instanceof RedstoneOutputBlockEntity) {
                outOutputBlocks.add(neighbor);
                continue;
            }

            if (blockEntity instanceof RedstoneConnectorBlockEntity connector) {
                connector.rebuildOutputCache(level, visited, outOutputBlocks);
            }
        }
    }

    public void markNetworkDirty(Set<BlockPos> visited) {
        if (!visited.add(this.getBlockPos())) return; // loop prevention
        this.cacheDirty = true;

        for (BlockPos neighbor : directConnections) {
            var entity = level.getBlockEntity(neighbor);
            if (entity instanceof RedstoneConnectorBlockEntity conn) {
                conn.markNetworkDirty(visited);
            }
        }
    }
}
