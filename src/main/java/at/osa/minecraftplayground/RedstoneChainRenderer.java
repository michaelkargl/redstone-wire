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

    /**
     * Called by Minecraft's rendering system every frame to render this block entity.
     * This is the entry point for all cable rendering.
     *
     * @param entity The RedstoneChainEntity being rendered
     * @param partialTicks How far between ticks the current frame is (for smooth animation)
     * @param stack The transformation matrix stack - used to position/rotate/scale objects
     * @param buffer The vertex buffer source - where we submit geometry for rendering
     * @param packedLight Light level encoded from sky and block light (0-15 for each)
     * @param packedOverlay Used for overlays like damage or enchantment effects
     */
    @Override
    public void render(RedstoneChainEntity entity, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockPos blockPos = entity.getBlockPos();
        int power = entity.getSignal();

        // Iterate through all connected blocks
        for (BlockPos connection : entity.getConnections()) {
            // Only render cable once per connection (compareTo ensures A->B is only rendered from A)
            // This avoids rendering the same cable twice
            if (blockPos.compareTo(connection) < 0) {
                // Start point is the center of current block (0.5, 0.5, 0.5)
                Vec3 start = new Vec3(0.5, 0.5, 0.5);

                // End point is the center of connected block, relative to current block
                Vec3 end = Vec3.atCenterOf(connection)
                        .subtract(Vec3.atCenterOf(blockPos))
                        .add(0.5, 0.5, 0.5);

                renderCable(stack, buffer, start, end, power, packedLight, packedOverlay);
            }
        }
    }

    /**
     * Renders a cable as segments with quad geometry.
     * Breaks the cable into multiple segments and calls drawSegment for each.
     * This creates the illusion of a smooth, curved cable by chaining many small cylinders.
     *
     * @param stack Transformation matrix stack
     * @param buffer Vertex buffer source
     * @param from Starting position of cable
     * @param to Ending position of cable
     * @param power Signal strength (0-15) - used to determine cable color
     * @param light Packed light value
     * @param overlay Packed overlay value
     */
    private void renderCable(PoseStack stack, MultiBufferSource buffer, Vec3 from, Vec3 to,
                            int power, int light, int overlay) {
        // Get the vertex consumer for our custom render type (handles shader, blend mode, etc.)
        VertexConsumer builder = buffer.getBuffer(RedstoneRenderType.CABLE_RENDERTYPE);

        // Get current transformation matrix from the stack (converts local coords to world coords)
        Matrix4f matrix = stack.last().pose();

        // Break cable into many small segments - more segments = smoother curve
        int segments = Config.getCableSegments();
        for (int i = 0; i < segments; i++) {
            // t is a value from 0 to 1 representing position along the cable
            // t1 = start of this segment, t2 = end of this segment
            float t1 = i / (float) segments;
            float t2 = (i + 1) / (float) segments;

            // Get actual 3D positions using curved interpolation (includes sagging effect)
            Vec3 p1 = interpolateCurved(from, to, t1);
            Vec3 p2 = interpolateCurved(from, to, t2);

            // Determine color for this segment based on power level and segment index
            float r, g, b;
            if (i % 2 == 0) {
                // Even segments: use primary color configuration
                r = (float) getColorComponent(power, Config.getUnpoweredRed(), Config.getPoweredRedBase(), Config.getPoweredRedBonus(), true);
                g = (float) Config.getGreenValue();
                b = (float) Config.getBlueValue();
            } else {
                // Odd segments: use alternate color (creates visual striping pattern)
                r = (float) getColorComponent(power, Config.getUnpoweredRedAlt(), Config.getPoweredRedBaseAlt(), Config.getPoweredRedBonusAlt(), true);
                g = (float) Config.getGreenValueAlt();
                b = (float) Config.getBlueValueAlt();
            }

            // Draw this segment as a cylinder
            drawSegment(builder, matrix, p1, p2, r, g, b, light, overlay);
        }
    }

    /**
     * Draw a single cable segment as a rounded cylinder with many radial faces.
     * Creates a rope-like appearance by drawing a cylinder made of multiple quad faces.
     *
     * Key concept: A cylinder is created by generating points in a circle around the cable's
     * direction, then connecting corresponding points at the start and end of the segment.
     *
     * @param builder The vertex consumer to submit geometry to
     * @param matrix Current transformation matrix
     * @param p1 Start point of this cable segment
     * @param p2 End point of this cable segment
     * @param r Red color component (0-1)
     * @param g Green color component (0-1)
     * @param b Blue color component (0-1)
     * @param light Packed light value
     * @param overlay Packed overlay value
     */
    private void drawSegment(VertexConsumer builder, Matrix4f matrix, Vec3 p1, Vec3 p2,
                            float r, float g, float b, int light, int overlay) {
        // Calculate the direction the cable points - this is the "spine" of our cylinder
        Vec3 direction = p2.subtract(p1).normalize();

        // To create a perpendicular vector, we need a reference "up" vector that isn't parallel to direction
        // If cable points straight up/down, use X as up; otherwise use Y
        Vec3 up = Math.abs(direction.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);

        // Create one perpendicular vector to the cable by crossing direction × up
        // This gives us a vector pointing "outward" from the cable
        // Then normalize and scale it to the cable thickness
        Vec3 basePerp = direction.cross(up).normalize().scale(Config.getCableThickness());

        // Create multiple perpendicular vectors arranged in a circle around the cable
        // This is like points on the edge of a circular cross-section
        // Example: if sides=12, we rotate basePerp 12 times around the cable in a full circle (360°)
        int sides = Config.getCableSides();
        Vec3[] perpVectors = new Vec3[sides];
        for (int i = 0; i < sides; i++) {
            // Calculate angle: i goes 0 to sides-1, so angle goes 0 to 360°
            double angle = (i / (double) sides) * 2 * Math.PI;
            // Rotate basePerp around the direction vector by this angle
            perpVectors[i] = rotateAroundAxis(basePerp, direction, Math.toDegrees(angle));
        }

        // Now we have a ring of points around p1 and a ring of points around p2
        // Connect each pair of consecutive rings with a quad face to form the cylinder surface
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;  // Next point in the ring (wraps around to 0 at the end)

            // The four corners of our quad face:
            // - p1_current and p1_next: two adjacent points on the ring around p1
            // - p2_current and p2_next: two adjacent points on the ring around p2
            Vec3 p1_current = p1.add(perpVectors[i]);
            Vec3 p1_next = p1.add(perpVectors[next]);
            Vec3 p2_current = p2.add(perpVectors[i]);
            Vec3 p2_next = p2.add(perpVectors[next]);

            // Draw quad connecting these 4 points (order matters for lighting - determines which way normal points)
            drawQuad(builder, matrix, p1_current, p1_next, p2_next, p2_current, r, g, b, light, overlay);
        }
    }

    /**
     * Rotates a vector around an arbitrary axis using Rodrigues' rotation formula.
     * This is a mathematical formula that rotates any vector around any axis by any angle.
     *
     * We use this to create the ring of points around the cable cylinder.
     * By rotating a base perpendicular vector multiple times around the cable direction,
     * we generate evenly-spaced points in a circle.
     *
     * @param v The vector to rotate
     * @param k The axis to rotate around (should be normalized)
     * @param angleDegrees The rotation angle in degrees
     * @return The rotated vector
     */
    private Vec3 rotateAroundAxis(Vec3 v, Vec3 k, double angleDegrees) {
        // Convert angle to radians (what the trig functions use)
        double angleRad = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double oneMinusCos = 1.0 - cos;

        // Rodrigues' formula has three terms that we add together:
        // term1: the component parallel to k stays roughly the same (scaled by cos)
        Vec3 term1 = v.scale(cos);

        // term2: perpendicular component rotates (k cross v scaled by sin)
        Vec3 term2 = k.cross(v).scale(sin);

        // term3: correction term to make the formula work (ensures result has same magnitude)
        double dotProduct = k.dot(v);
        Vec3 term3 = k.scale(dotProduct * oneMinusCos);

        return term1.add(term2).add(term3);
    }

    /**
     * Draw a single quad face (4 corners) as 2 triangles.
     *
     * In 3D graphics, quads must be split into triangles for rendering.
     * We also calculate the surface normal (direction the face is pointing).
     * The normal is used by the lighting system to determine how bright this face should be.
     *
     * Vertex order matters: vertices should be in counter-clockwise order when viewed from outside,
     * so the normal points outward and lighting looks correct.
     *
     * @param builder The vertex consumer to submit to
     * @param matrix Transformation matrix
     * @param c0 First corner
     * @param c1 Second corner
     * @param c2 Third corner
     * @param c3 Fourth corner
     * @param r Red color
     * @param g Green color
     * @param b Blue color
     * @param light Packed light value
     * @param overlay Packed overlay value
     */
    private void drawQuad(VertexConsumer builder, Matrix4f matrix,
                         Vec3 c0, Vec3 c1, Vec3 c2, Vec3 c3,
                         float r, float g, float b, int light, int overlay) {
        // Calculate the surface normal (perpendicular direction to the face)
        // We use the cross product of two edges: if they're perpendicular, the cross product
        // points perpendicular to both (which is perpendicular to the face)
        Vec3 edge1 = c1.subtract(c0);
        Vec3 edge2 = c2.subtract(c1);
        Vec3 normal = edge1.cross(edge2).normalize();

        // Minecraft requires triangles, not quads. So we split this quad into 2 triangles
        // Triangle 1: c0, c1, c2
        addVertex(builder, matrix, c0, r, g, b, light, overlay, normal);
        addVertex(builder, matrix, c1, r, g, b, light, overlay, normal);
        addVertex(builder, matrix, c2, r, g, b, light, overlay, normal);

        // Triangle 2: c0, c2, c3
        addVertex(builder, matrix, c0, r, g, b, light, overlay, normal);
        addVertex(builder, matrix, c2, r, g, b, light, overlay, normal);
        addVertex(builder, matrix, c3, r, g, b, light, overlay, normal);
    }

    /**
     * Calculates a color component value based on the signal power level.
     * This creates the effect where cables get brighter red as they carry more power.
     *
     * Redstone power ranges from 0-15. When power is 0, cable is dim (unpowered color).
     * As power increases, the red channel brightens from base to (base + bonus).
     *
     * @param power Signal strength (0-15)
     * @param unpowered Color value when power is 0
     * @param base Base color value when powered
     * @param bonus Additional color added per unit of power
     * @param isRed Whether this is the red channel (affects formula)
     * @return Color component value (0-1)
     */
    private double getColorComponent(int power, double unpowered, double base, double bonus, boolean isRed) {
        // If this is the red channel and cable has power
        if (isRed && power > 0) {
            // Linear interpolation: base + (power/15) * bonus
            // At power 0: returns base
            // At power 15: returns base + bonus
            return base + (power / 15.0f) * bonus;
        }
        // If power is 0, use the unpowered color
        return isRed ? unpowered : (isRed ? 0 : Config.getGreenValue());
    }

    /**
     * Interpolates along a cable path with a realistic sagging effect.
     * Real cables hang down due to gravity, so we add a downward curve.
     *
     * The curve uses a sine wave: it starts at 0, goes down in the middle, and returns to 0 at the end.
     * This mimics how a real cable would sag.
     *
     * @param from Starting point of cable
     * @param to Ending point of cable
     * @param t Position along cable (0 = start, 1 = end)
     * @return Interpolated position with sag applied
     */
    private static Vec3 interpolateCurved(Vec3 from, Vec3 to, float t) {
        // First, do a linear interpolation (straight line) between from and to
        Vec3 linear = from.lerp(to, t);

        // Check if cable is mostly vertical (no sag for vertical cables)
        double horizontalDist = Math.abs(from.x - to.x) + Math.abs(from.z - to.z);

        // If horizontal distance is very small, cable is vertical - don't apply sag
        if (horizontalDist < 0.001) {
            return linear;
        }

        // Apply downward sag using sine wave:
        // At t=0: sin(0) = 0, no sag
        // At t=0.5: sin(π*0.5) = 1, maximum sag (downward)
        // At t=1: sin(π) = 0, no sag
        // Multiply by config value to control how much the cable sags
        double sag = Math.sin(t * Math.PI) * Config.getCableSagAmount();

        // Return the linear position moved down by sag amount
        return new Vec3(linear.x, linear.y + sag, linear.z);
    }

    /**
     * Adds a single vertex to the vertex buffer with all required properties.
     * This is the lowest-level method that actually submits geometry data to Minecraft's rendering system.
     *
     * A vertex contains:
     * - Position: 3D coordinates (x, y, z)
     * - Color: RGB values for coloring
     * - Normal: Direction the surface faces (affects lighting)
     * - Light: Brightness level
     * - Overlay: For damage/enchantment effects
     *
     * @param builder The vertex consumer (handles batch submission)
     * @param matrix Transformation matrix to position the vertex
     * @param pos The 3D position of this vertex
     * @param r Red color (0-1)
     * @param g Green color (0-1)
     * @param b Blue color (0-1)
     * @param light Packed light value (0-255)
     * @param overlay Packed overlay value (0-255)
     * @param normal Surface normal direction (affects shading/lighting)
     */
    private void addVertex(VertexConsumer builder, Matrix4f matrix, Vec3 pos,
                          float r, float g, float b, int light, int overlay, Vec3 normal) {
        // Start building a vertex at this position, transformed by the matrix
        builder.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                // Set the vertex color (cables are red, or configured alternate colors)
                .setColor(r, g, b, 1f)
                // Set UV coordinates (texture mapping - we use 0,0 since we don't have a texture)
                .setUv(0, 0)
                // Set overlay for visual effects like damage or enchantment
                .setOverlay(overlay)
                // Set light level for brightness calculation
                .setLight(light)
                // Set surface normal (direction the face is pointing) for proper lighting
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }
}
