package at.osa.redstonewire.output;

import at.osa.redstonewire.RedstoneWireBlock;
import at.osa.redstonewire.renderer.CableRenderer;
import at.osa.redstonewire.renderer.CableRenderer.CableBlockEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renders cables from a RedstoneOutputBlock to each of its linked ConnectorBlocks.
 */
public class RedstoneOutputBlockEntityRenderer
        implements BlockEntityRenderer<RedstoneOutputBlockEntity, CableBlockEntityRenderState> {

    // Distance from block center to the antennae X-axis center in the NORTH-facing model (12/16 - 0.5)
    private static final double ANTENNAE_X_OFFSET = 0.25;

    public RedstoneOutputBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        super();
    }

    @Override
    public CableBlockEntityRenderState createRenderState() {
        return new CableBlockEntityRenderState();
    }

    /** Cables reach well past this block; the default unit cube would cull them off-screen. */
    @Override
    public AABB getRenderBoundingBox(RedstoneOutputBlockEntity entity) {
        return CableRenderer.cableBounds(entity);
    }

    @Override
    public int getViewDistance() {
        return CableRenderer.CABLE_VIEW_DISTANCE;
    }

    @Override
    public void extractRenderState(RedstoneOutputBlockEntity entity, CableBlockEntityRenderState state,
                                   float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        state.connections = entity.getConnections(); // already an immutable snapshot
        state.power = entity.getBlockState().getValue(RedstoneOutputBlock.POWER);
        state.facing = entity.getBlockState().getValue(RedstoneWireBlock.FACING);
    }

    @Override
    public void submit(CableBlockEntityRenderState state, PoseStack stack,
                       SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BlockPos blockPos = state.blockPos;
        Direction facing = state.facing;

        double startX = 0.5, startZ = 0.5;
        switch (facing) {
            case NORTH -> startX = 0.5 + ANTENNAE_X_OFFSET;
            case SOUTH -> startX = 0.5 - ANTENNAE_X_OFFSET;
            case EAST  -> startZ = 0.5 + ANTENNAE_X_OFFSET;
            case WEST  -> startZ = 0.5 - ANTENNAE_X_OFFSET;
        }

        for (BlockPos connection : state.connections) {
            Vec3 start = new Vec3(startX, RedstoneWireBlock.ANTENNA_TIP_Y, startZ);
            Vec3 end = Vec3.atCenterOf(connection)
                    .subtract(Vec3.atCenterOf(blockPos))
                    .add(0.5, RedstoneWireBlock.ANTENNA_TIP_Y, 0.5);
            CableRenderer.renderCable(stack, nodeCollector, start, end, state.power, state.lightCoords);
        }
    }
}
