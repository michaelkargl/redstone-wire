package at.osa.redstonewire.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Shared static utility for rendering sagging 3D cables between block positions.
 * Used by both RedstoneConnectorBlockEntityRenderer and RedstoneInputBlockEntityRenderer.
 *
 * @credit MaxLegend/OverheadRedstoneWires: https://github.com/MaxLegend/OverheadRedstoneWires/blob/e4f99f013abad4fc2b39a6c7a7f6620dfce052d6/src/main/java/ru/tesmio/redstonefication/redstonecable/RedstoneCableRenderer.java#L1
 */
public final class CableRenderer {

    public static final RenderType LIGHT_COLOR_RENDER = RenderType.create(
            "light_color_render",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.TRIANGLES,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorLightmapShader))
                    .setCullState(new RenderStateShard.CullStateShard(false)) // Отключаем culling
                    .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                    .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                    .createCompositeState(false)
    );

    private static final double cableThickness = 0.02F;
    private static final double cableSegments = 12;
    private static final double curveAmplitude = 0.4;

    /**
     * Renders a cable as segments with quad geometry between two points.
     * Breaks the cable into multiple segments and draws each as a small cylinder.
     */
    public static void renderCable(PoseStack stack, MultiBufferSource buffer, Vec3 from, Vec3 to,
                                   int power, int light, int overlay) {
        renderCurvedCuboid(stack, buffer, from, to, light, overlay);
    }

    private static void renderCurvedCuboid(PoseStack poseStack, MultiBufferSource buffer,
                                          Vec3 from, Vec3 to, int light, int overlay) {

        VertexConsumer builder = buffer.getBuffer(LIGHT_COLOR_RENDER);

        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i < cableSegments; i++) {
            var t1 = i / cableSegments;
            var t2 = (i + 1) / cableSegments;

            Vec3 p1 = interpolateCurved(from, to, t1);
            Vec3 p2 = interpolateCurved(from, to, t2);

            drawThickSegment(builder, matrix, p1, p2, cableThickness, light, overlay);
        }
    }

    private static Vec3 interpolateCurved(Vec3 from, Vec3 to, double t) {
        Vec3 linear = from.lerp(to, t);
        if (Math.abs(from.x - to.x) < 0.001 && Math.abs(from.z - to.z) < 0.001) {
            return linear;
        }
        double curve = Math.sin(t * Math.PI) * -curveAmplitude; // провисание вниз
        return new Vec3(linear.x, linear.y + curve, linear.z);
    }


    private static void drawThickSegment(VertexConsumer builder, Matrix4f matrix,
                                         Vec3 p1, Vec3 p2, double thickness, int light, int overlay) {
        // Вычисляем вектор направления
        Vec3 dir = p2.subtract(p1).normalize();
        //    Vec3 up = new Vec3(0, 1, 0);
        Vec3 up = Math.abs(dir.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize().scale(thickness);
        Vec3 forward = dir.cross(right).normalize().scale(thickness);

        // Вершины прямоугольного параллелепипеда
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
            Vec3 normalVec = corners[face[1]].subtract(corners[face[0]])
                    .cross(corners[face[2]].subtract(corners[face[1]]))
                    .normalize();
            float nx = (float) normalVec.x, ny = (float) normalVec.y, nz = (float) normalVec.z;

            // Each quad face is split into 2 triangles: [0,1,2] and [0,2,3]
            for (int idx : new int[]{face[0], face[1], face[2], face[0], face[2], face[3]}) {
                Vec3 v = corners[idx];
                builder.addVertex(matrix, (float) v.x, (float) v.y, (float) v.z)
                        .setColor(0.3f, 0, 0, 1f)
                        .setUv(0, 0)
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(nx, ny, nz);
            }
        }
    }
}
