package at.osa.minecraftplayground;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders cables between connected RedstoneChainEntity blocks.
 * Cables sag realistically and are colored based on power level.
 *
 * @credit Create Crafts & Additions: https://github.com/mrh0/createaddition
 * @credit Overhead Redstone Wires: https://github.com/MaxLegend/OverheadRedstoneWires
 */
public class RedstoneChainRenderer implements BlockEntityRenderer<RedstoneChainEntity> {

    public RedstoneChainRenderer(BlockEntityRendererProvider.Context ctx) {
        super();
    }

    @Override
    public int getViewDistance() {
        return Config.MAX_RENDER_DISTANCE.getAsInt();
    }

    private static int getCableSegments() {
        return Config.CABLE_SEGMENTS.getAsInt();
    }

    private static double getCableThickness() {
        return Config.CABLE_THICKNESS_IN_BLOCKS.getAsDouble();
    }

    private static double getCableSagAmount() {
        return Config.CABLE_SAG_AMOUNT.getAsDouble();
    }

    private static double getUnpoweredRed() {
        return Config.UNPOWERED_RED.getAsDouble();
    }

    private static double getPoweredRedBase() {
        return Config.POWERED_RED_BASE.getAsDouble();
    }

    private static double getPoweredRedBonus() {
        return Config.POWERED_RED_BONUS.getAsDouble();
    }

    private static double getGreenValue() {
        return Config.GREEN_VALUE.getAsDouble();
    }

    private static double getBlueValue() {
        return Config.BLUE_VALUE.getAsDouble();
    }

    @Override
    public void render(RedstoneChainEntity entity, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockPos blockPos = entity.getBlockPos();
        int power = entity.getSignal();

        for (BlockPos connection : entity.getConnections()) {
            if (blockPos.compareTo(connection) < 0) {
                Vec3 start = new Vec3(0.5, 0.5, 0.5);
                Vec3 end = Vec3.atCenterOf(connection)
                        .subtract(Vec3.atCenterOf(blockPos))
                        .add(0.5, 0.5, 0.5);

                renderCable(stack, buffer, start, end, power, packedLight, packedOverlay);
            }
        }
    }

    /**
     * Renders a cable as segments with quad geometry.
     */
    private void renderCable(PoseStack stack, MultiBufferSource buffer, Vec3 from, Vec3 to,
                            int power, int light, int overlay) {
        VertexConsumer builder = buffer.getBuffer(RedstoneRenderType.CABLE);
        Matrix4f matrix = stack.last().pose();

        float r = (float) getColorComponent(power, getUnpoweredRed(), getPoweredRedBase(), getPoweredRedBonus(), true);
        float g = (float) getGreenValue();
        float b = (float) getBlueValue();

        int segments = getCableSegments();
        for (int i = 0; i < segments; i++) {
            float t1 = i / (float) segments;
            float t2 = (i + 1) / (float) segments;

            Vec3 p1 = interpolateCurved(from, to, t1);
            Vec3 p2 = interpolateCurved(from, to, t2);

            drawSegment(builder, matrix, p1, p2, r, g, b, light, overlay);
        }
    }

    /**
     * Draw a single cable segment as two triangles (a quad).
     */
    private void drawSegment(VertexConsumer builder, Matrix4f matrix, Vec3 p1, Vec3 p2,
                            float r, float g, float b, int light, int overlay) {
        Vec3 direction = p2.subtract(p1).normalize();

        // Create perpendicular vector for thickness
        Vec3 up = Math.abs(direction.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 perp = direction.cross(up).normalize().scale(getCableThickness());

        // Four corners of the quad
        Vec3 p1a = p1.add(perp);
        Vec3 p1b = p1.subtract(perp);
        Vec3 p2a = p2.add(perp);
        Vec3 p2b = p2.subtract(perp);

        // First triangle
        addVertex(builder, matrix, p1a, r, g, b, light, overlay);
        addVertex(builder, matrix, p1b, r, g, b, light, overlay);
        addVertex(builder, matrix, p2a, r, g, b, light, overlay);

        // Second triangle
        addVertex(builder, matrix, p1b, r, g, b, light, overlay);
        addVertex(builder, matrix, p2b, r, g, b, light, overlay);
        addVertex(builder, matrix, p2a, r, g, b, light, overlay);
    }

    /**
     * Gets the color component based on power level.
     */
    private double getColorComponent(int power, double unpowered, double base, double bonus, boolean isRed) {
        if (isRed && power > 0) {
            return base + (power / 15.0f) * bonus;
        }
        return isRed ? unpowered : (isRed ? 0 : getGreenValue());
    }

    /**
     * Interpolates along a curved cable path with sagging effect.
     */
    private static Vec3 interpolateCurved(Vec3 from, Vec3 to, float t) {
        Vec3 linear = from.lerp(to, t);
        double horizontalDist = Math.abs(from.x - to.x) + Math.abs(from.z - to.z);

        if (horizontalDist < 0.001) {
            return linear;
        }

        double sag = Math.sin(t * Math.PI) * getCableSagAmount();
        return new Vec3(linear.x, linear.y + sag, linear.z);
    }

    /**
     * Adds a vertex to the builder.
     */
    private void addVertex(VertexConsumer builder, Matrix4f matrix, Vec3 pos,
                          float r, float g, float b, int light, int overlay) {
        builder.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, 1f)
                .setUv(0, 0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(0, 1, 0);
    }
}
