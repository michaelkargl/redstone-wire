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
        BlockPos blockPos = entity.getBlockPos();
        int power = entity.getSignal();

        for (BlockPos connection : entity.getConnections()) {
            // Only render if this block's position is "less than" the connection
            // This prevents rendering the same cable twice from both ends
            if (blockPos.compareTo(connection) < 0) {
                Vec3 start = new Vec3(0.5, 0.5, 0.5);
                Vec3 end = Vec3.atCenterOf(connection).subtract(Vec3.atCenterOf(blockPos)).add(0.5, 0.5, 0.5);
                renderCurvedCuboid(stack, buffer, start, end, packedLight, packedOverlay, power);
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

        int segments = 10;
        float thickness = 0.03F;  // Increased from 0.015F for thicker cables

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // Power-based coloring
        float red = power > 0 ? 0.9f + (power / 15.0f) * 0.1f : 0.3f;
        float green = 0.0f;
        float blue = 0.0f;

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
        double curve = Math.sin(t * Math.PI) * -0.4; // sag downward
        return new Vec3(linear.x, linear.y + curve, linear.z);
    }

    private static void drawThickSegment(VertexConsumer builder, Matrix4f matrix, Matrix3f normal,
                                         Vec3 p1, Vec3 p2, float thickness, int light, int overlay,
                                         float r, float g, float b) {
        Vec3 dir = p2.subtract(p1).normalize();
        Vec3 up = Math.abs(dir.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize().scale(thickness);
        Vec3 forward = dir.cross(right).normalize().scale(thickness);

        // Square/rectangular box corners
        Vec3[] corners = new Vec3[]{
                p1.add(right).add(forward),      // 0: p1 +X +Z
                p1.add(right).subtract(forward), // 1: p1 +X -Z
                p1.subtract(right).subtract(forward), // 2: p1 -X -Z
                p1.subtract(right).add(forward), // 3: p1 -X +Z
                p2.add(right).add(forward),      // 4: p2 +X +Z
                p2.add(right).subtract(forward), // 5: p2 +X -Z
                p2.subtract(right).subtract(forward), // 6: p2 -X -Z
                p2.subtract(right).add(forward), // 7: p2 -X +Z
        };

        // Define faces with proper winding order (counter-clockwise when viewed from outside)
        int[][] faces = {
                {0, 1, 2, 3}, // bottom (p1 end)
                {7, 6, 5, 4}, // top (p2 end)
                {0, 4, 5, 1}, // right side
                {1, 5, 6, 2}, // front side
                {2, 6, 7, 3}, // left side
                {3, 7, 4, 0}, // back side
        };

        // Render each face with double-sided rendering
        for (int[] face : faces) {
            // Calculate face normal
            Vec3 v0 = corners[face[0]];
            Vec3 v1 = corners[face[1]];
            Vec3 v2 = corners[face[2]];
            Vec3 edge1 = v1.subtract(v0);
            Vec3 edge2 = v2.subtract(v0);
            Vec3 normalVec = edge1.cross(edge2).normalize();

            // === FRONT FACE (normal pointing outward) ===
            // First triangle (0, 1, 2)
            builder.addVertex(matrix, (float) corners[face[0]].x, (float) corners[face[0]].y, (float) corners[face[0]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) normalVec.x, (float) normalVec.y, (float) normalVec.z);

            builder.addVertex(matrix, (float) corners[face[1]].x, (float) corners[face[1]].y, (float) corners[face[1]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) normalVec.x, (float) normalVec.y, (float) normalVec.z);

            builder.addVertex(matrix, (float) corners[face[2]].x, (float) corners[face[2]].y, (float) corners[face[2]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) normalVec.x, (float) normalVec.y, (float) normalVec.z);

            // Second triangle (0, 2, 3)
            builder.addVertex(matrix, (float) corners[face[0]].x, (float) corners[face[0]].y, (float) corners[face[0]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) normalVec.x, (float) normalVec.y, (float) normalVec.z);

            builder.addVertex(matrix, (float) corners[face[2]].x, (float) corners[face[2]].y, (float) corners[face[2]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) normalVec.x, (float) normalVec.y, (float) normalVec.z);

            builder.addVertex(matrix, (float) corners[face[3]].x, (float) corners[face[3]].y, (float) corners[face[3]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) normalVec.x, (float) normalVec.y, (float) normalVec.z);

            // === BACK FACE (reversed winding order, inverted normal) ===
            // First triangle (0, 2, 1) - reversed
            builder.addVertex(matrix, (float) corners[face[0]].x, (float) corners[face[0]].y, (float) corners[face[0]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) -normalVec.x, (float) -normalVec.y, (float) -normalVec.z);

            builder.addVertex(matrix, (float) corners[face[2]].x, (float) corners[face[2]].y, (float) corners[face[2]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) -normalVec.x, (float) -normalVec.y, (float) -normalVec.z);

            builder.addVertex(matrix, (float) corners[face[1]].x, (float) corners[face[1]].y, (float) corners[face[1]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) -normalVec.x, (float) -normalVec.y, (float) -normalVec.z);

            // Second triangle (0, 3, 2) - reversed
            builder.addVertex(matrix, (float) corners[face[0]].x, (float) corners[face[0]].y, (float) corners[face[0]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) -normalVec.x, (float) -normalVec.y, (float) -normalVec.z);

            builder.addVertex(matrix, (float) corners[face[3]].x, (float) corners[face[3]].y, (float) corners[face[3]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) -normalVec.x, (float) -normalVec.y, (float) -normalVec.z);

            builder.addVertex(matrix, (float) corners[face[2]].x, (float) corners[face[2]].y, (float) corners[face[2]].z)
                    .setColor(r, g, b, 1f)
                    .setUv(0, 0)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal((float) -normalVec.x, (float) -normalVec.y, (float) -normalVec.z);
        }
    }
}
