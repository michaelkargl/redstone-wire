package at.osa.minecraftplayground;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders cables between connected RedstoneChainEntity blocks.
 * Based on OverheadRedstoneWires cable rendering with power-based coloring.
 * The cables sag realistically like overhead wires would under gravity.
 */
public class RedstoneChainRenderer implements BlockEntityRenderer<RedstoneChainEntity> {

    public static final RenderType CABLE_RENDER_TYPE = RenderType.create(
            "redstone_cable_render",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.TRIANGLES,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorLightmapShader))
                    .setCullState(new RenderStateShard.CullStateShard(false)) // Disable culling
                    .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                    .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                    .createCompositeState(false)
    );

    public RedstoneChainRenderer(BlockEntityRendererProvider.Context ctx) {
        super();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public void render(RedstoneChainEntity entity, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Vec3 center = Vec3.atCenterOf(entity.getBlockPos()).subtract(Vec3.atCenterOf(entity.getBlockPos()));
        BlockPos blockPos = entity.getBlockPos();

        int power = entity.getSignal();

        for (BlockPos connection : entity.getConnections()) {
            // Only render if this block's position is "less than" the connection
            // This prevents rendering the same cable twice from both ends
            if (blockPos.compareTo(connection) < 0) {
                Vec3 end = Vec3.atCenterOf(connection).subtract(Vec3.atCenterOf(blockPos));
                renderCurvedCuboid(stack, buffer, center.add(0.5, 0.5, 0.5), end.add(0.5, 0.5, 0.5),
                        packedLight, packedOverlay, power);
            }
        }
    }

    /**
     * Renders a curved cable between two points.
     * Enhanced with power-based coloring.
     */
    public static void renderCurvedCuboid(PoseStack poseStack, MultiBufferSource buffer,
                                          Vec3 from, Vec3 to, int light, int overlay, int power) {
        VertexConsumer builder = buffer.getBuffer(CABLE_RENDER_TYPE);

        int segments = 30;
        float thickness = 0.03F;

        PoseStack.Pose pose = poseStack.last();y
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // Power-based coloring
        float red, green, blue;
        if (power > 0) {
            red = 0.9f + (power / 15.0f) * 0.1f;
            green = 0.0f;
            blue = 0.0f;
        } else {
            red = 0.3f;
            green = 0.0f;
            blue = 0.0f;
        }

        for (int i = 0; i < segments; i++) {
            float t1 = i / (float) segments;
            float t2 = (i + 1) / (float) segments;

            Vec3 p1 = interpolateCurved(from, to, t1);
            Vec3 p2 = interpolateCurved(from, to, t2);

            drawThickSegment(builder, matrix, normal, p1, p2, thickness, light, overlay, red, green, blue);
        }
    }

    private static Vec3 interpolateCurved(Vec3 from, Vec3 to, float t) {
        Vec3 linear = from.lerp(to, t);
        if (Math.abs(from.x - to.x) < 0.001 && Math.abs(from.z - to.z) < 0.001) {
            return linear;
        }
        double curveAmplitude = 0.4;
        double curve = Math.sin(t * Math.PI) * -curveAmplitude;
        return new Vec3(linear.x, linear.y + curve, linear.z);
    }

    private static void drawThickSegment(VertexConsumer builder, Matrix4f matrix, Matrix3f normal,
                                         Vec3 p1, Vec3 p2, float thickness, int light, int overlay,
                                         float r, float g, float b) {
        Vec3 dir = p2.subtract(p1).normalize();
        Vec3 up = Math.abs(dir.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize().scale(thickness);
        Vec3 forward = dir.cross(right).normalize().scale(thickness);

        // Rectangular box corners
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
                Vec3 normalVec = corners[face[1]].subtract(corners[face[0]])
                        .cross(corners[face[2]].subtract(corners[face[1]]))
                        .normalize();
                Vec3 v = corners[idx];
                builder.addVertex(matrix, (float) v.x, (float) v.y, (float) v.z)
                        .setColor(r, g, b, 1f)
                        .setUv(0, 0)
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal((float) normalVec.x, (float) normalVec.y, (float) normalVec.z);
            }
        }
    }
}
