package at.osa.redstonewire.renderer;

import at.osa.redstonewire.RedstoneWire;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.List;

/**
 * Shared static utility for rendering sagging 3D cables between block positions.
 * Used by both RedstoneConnectorBlockEntityRenderer and RedstoneInputBlockEntityRenderer.
 *
 * @credit MaxLegend/OverheadRedstoneWires: https://github.com/MaxLegend/OverheadRedstoneWires/blob/e4f99f013abad4fc2b39a6c7a7f6620dfce052d6/src/main/java/ru/tesmio/redstonefication/redstonecable/RedstoneCableRenderer.java#L1
 */
public final class CableRenderer {

    /** Values copied from a block entity for one frame of cable rendering. */
    public static class CableBlockEntityRenderState extends BlockEntityRenderState {
        public List<BlockPos> connections = List.of();
        // Captured, but deliberately not used for cable colour yet: that is a separate change.
        // Note connectors have no stored power at all - RedstoneWireBlockEntity.getSignal()
        // returns 0 and RedstoneConnectorBlockEntity does not override it.
        public int power;
        public Direction facing = Direction.NORTH;
    }

    /**
     * Rendering is pipeline-based as of Minecraft 1.21.9. This pipeline accepts
     * position, color, and light-map data and draws groups of four vertices as quads.
     */
    public static final RenderPipeline CABLE_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(RedstoneWire.MODID, "pipeline/cable"))
            .withVertexShader("core/rendertype_leash")
            .withFragmentShader("core/rendertype_leash")
            .withSampler("Sampler2")
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderType LIGHT_COLOR_RENDER = RenderType.create(
            "redstone_wire_cable",
            RenderSetup.builder(CABLE_PIPELINE)
                    .useLightmap()
                    .createRenderSetup());

    private static final double cableThickness = 0.02F;
    private static final double cableSegments = 12;
    /**
     * Sag per block of horizontal distance.
     * @example a value of 0.15 means that a 10-block cable sags 1.5 blocks at its midpoint
     */
    private static final double sagFactorInBlocks = 0.10;

    /**
     * Maximum allowed sag in Blocks.
     * @example a value of 1.5 means that at cable midpoint the cable sags 1.5 blocks
     */
    private static final double maxSagAmountInBlocks = 1.0;

    /**
     * Renders a cable as segments with quad geometry between two points.
     * Breaks the cable into multiple segments and draws each as a small cylinder.
     */
    public static void registerRenderPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CABLE_PIPELINE);
    }

    public static void renderCable(PoseStack stack, SubmitNodeCollector nodeCollector, Vec3 from, Vec3 to,
                                   int power, int light) {
        nodeCollector.submitCustomGeometry(
                stack,
                LIGHT_COLOR_RENDER,
                (pose, builder) -> renderCurvedCuboid(pose, builder, from, to, light));
    }

    private static void renderCurvedCuboid(PoseStack.Pose pose, VertexConsumer builder,
                                           Vec3 from, Vec3 to, int light) {
        for (int i = 0; i < cableSegments; i++) {
            var t1 = i / cableSegments;
            var t2 = (i + 1) / cableSegments;

            Vec3 p1 = interpolateCurved(from, to, t1);
            Vec3 p2 = interpolateCurved(from, to, t2);

            drawThickSegment(builder, pose, p1, p2, cableThickness, light);
        }
    }

    private static Vec3 interpolateCurved(Vec3 from, Vec3 to, double t) {
        var linear = from.lerp(to, t);
        var distanceX = Math.abs(from.x - to.x);
        var distanceZ =  Math.abs(from.z - to.z);

        if (distanceX < 0.001 && distanceZ < 0.001) {
            return linear;
        }

        var horizontalDistance = Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);
        var sagAmplitudeInBlocks = Math.min(sagFactorInBlocks * horizontalDistance, maxSagAmountInBlocks);
        var curve = Math.sin(t * Math.PI) * -sagAmplitudeInBlocks; // negative = sags downward
        return new Vec3(linear.x, linear.y + curve, linear.z);
    }


    private static void drawThickSegment(VertexConsumer builder, PoseStack.Pose pose,
                                         Vec3 p1, Vec3 p2, double thickness, int light) {
        Vec3 dir = p2.subtract(p1).normalize();
        // When dir is nearly vertical, use X as up to avoid a degenerate cross product
        Vec3 up = Math.abs(dir.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize().scale(thickness);
        Vec3 forward = dir.cross(right).normalize().scale(thickness);

        Vec3[] corners = new Vec3[]{
                p1.add(right).add(forward),
                p1.add(right).subtract(forward),
                p1.subtract(right).subtract(forward),
                p1.subtract(right).add(forward),

                p2.add(right).add(forward),
                p2.add(right).subtract(forward),
                p2.subtract(right).subtract(forward),
                p2.subtract(right).add(forward),
        };

        int[][] faces = {
                {0, 1, 2, 3}, // bottom
                {7, 6, 5, 4}, // top
                {0, 4, 5, 1}, // right
                {1, 5, 6, 2}, // front
                {2, 6, 7, 3}, // left
                {3, 7, 4, 0}, // back
        };

        for (int[] face : faces) {
            for (int idx : face) {
                Vec3 v = corners[idx];
                builder.addVertex(pose, (float) v.x, (float) v.y, (float) v.z)
                        .setColor(0.3f, 0, 0, 1f)
                        .setLight(light);
            }
        }
    }
}
