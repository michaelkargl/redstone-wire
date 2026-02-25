package at.osa.redstonewire;

import at.osa.redstonewire.connector.RedstoneConnectorBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Shared static utility for rendering sagging 3D cables between block positions.
 * Used by both RedstoneConnectorBlockEntityRenderer and RedstoneInputBlockEntityRenderer.
 */
public final class CableRenderer {

    private CableRenderer() {}

    /**
     * Returns the Y coordinate (in local block space) where cables attach to a block.
     * Non-full-cube blocks like the relay have a lower attachment point matching their geometry.
     */
    public static double attachY(Block block) {
        return block instanceof RedstoneConnectorBlock ? 11.0 / 16.0 : 1.0;
    }

    /**
     * Renders a cable as segments with quad geometry between two points.
     * Breaks the cable into multiple segments and draws each as a small cylinder.
     */
    public static void renderCable(PoseStack stack, MultiBufferSource buffer, Vec3 from, Vec3 to,
                                   int power, int light, int overlay) {
        VertexConsumer builder = buffer.getBuffer(RedstoneRenderType.CABLE_RENDERTYPE);
        Matrix4f matrix = stack.last().pose();

        int segments = Config.getCableSegments();
        for (int i = 0; i < segments; i++) {
            float t1 = i / (float) segments;
            float t2 = (i + 1) / (float) segments;

            Vec3 p1 = interpolateCurved(from, to, t1);
            Vec3 p2 = interpolateCurved(from, to, t2);

            float r, g, b;
            if (i % 2 == 0) {
                r = (float) getColorComponent(power, Config.getUnpoweredRed(), Config.getPoweredRedBase(), Config.getPoweredRedBonus(), true);
                g = (float) Config.getGreenValue();
                b = (float) Config.getBlueValue();
            } else {
                r = (float) getColorComponent(power, Config.getUnpoweredRedAlt(), Config.getPoweredRedBaseAlt(), Config.getPoweredRedBonusAlt(), true);
                g = (float) Config.getGreenValueAlt();
                b = (float) Config.getBlueValueAlt();
            }

            drawSegment(builder, matrix, p1, p2, r, g, b, light, overlay);
        }
    }

    private static void drawSegment(VertexConsumer builder, Matrix4f matrix, Vec3 p1, Vec3 p2,
                                    float r, float g, float b, int light, int overlay) {
        Vec3 direction = p2.subtract(p1).normalize();
        Vec3 up = Math.abs(direction.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 basePerp = direction.cross(up).normalize().scale(Config.getCableThickness());

        int sides = Config.getCableSides();
        Vec3[] perpVectors = new Vec3[sides];
        for (int i = 0; i < sides; i++) {
            double angle = (i / (double) sides) * 2 * Math.PI;
            perpVectors[i] = rotateAroundAxis(basePerp, direction, Math.toDegrees(angle));
        }

        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            Vec3 p1c = p1.add(perpVectors[i]);
            Vec3 p1n = p1.add(perpVectors[next]);
            Vec3 p2c = p2.add(perpVectors[i]);
            Vec3 p2n = p2.add(perpVectors[next]);
            drawQuad(builder, matrix, p1c, p1n, p2n, p2c, r, g, b, light, overlay);
        }
    }

    private static Vec3 rotateAroundAxis(Vec3 v, Vec3 k, double angleDegrees) {
        double angleRad = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double oneMinusCos = 1.0 - cos;
        Vec3 term1 = v.scale(cos);
        Vec3 term2 = k.cross(v).scale(sin);
        Vec3 term3 = k.scale(k.dot(v) * oneMinusCos);
        return term1.add(term2).add(term3);
    }

    private static void drawQuad(VertexConsumer builder, Matrix4f matrix,
                                  Vec3 c0, Vec3 c1, Vec3 c2, Vec3 c3,
                                  float r, float g, float b, int light, int overlay) {
        Vec3 edge1 = c1.subtract(c0);
        Vec3 edge2 = c2.subtract(c1);
        Vec3 normal = edge1.cross(edge2).normalize();

        addVertex(builder, matrix, c0, r, g, b, light, overlay, normal);
        addVertex(builder, matrix, c1, r, g, b, light, overlay, normal);
        addVertex(builder, matrix, c2, r, g, b, light, overlay, normal);

        addVertex(builder, matrix, c0, r, g, b, light, overlay, normal);
        addVertex(builder, matrix, c2, r, g, b, light, overlay, normal);
        addVertex(builder, matrix, c3, r, g, b, light, overlay, normal);
    }

    private static void addVertex(VertexConsumer builder, Matrix4f matrix, Vec3 pos,
                                   float r, float g, float b, int light, int overlay, Vec3 normal) {
        builder.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, 1f)
                .setUv(0, 0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static Vec3 interpolateCurved(Vec3 from, Vec3 to, float t) {
        Vec3 linear = from.lerp(to, t);
        double horizontalDist = Math.abs(from.x - to.x) + Math.abs(from.z - to.z);
        if (horizontalDist < 0.001) return linear;
        double sag = Math.sin(t * Math.PI) * Config.getCableSagAmount();
        return new Vec3(linear.x, linear.y + sag, linear.z);
    }

    private static double getColorComponent(int power, double unpowered, double base, double bonus, boolean isRed) {
        if (isRed && power > 0) {
            return base + (power / 15.0f) * bonus;
        }
        return isRed ? unpowered : (isRed ? 0 : Config.getGreenValue());
    }
}
