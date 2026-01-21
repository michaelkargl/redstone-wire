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

    // Rendering configuration constants
    private static final int CABLE_SEGMENTS = Config.CABLE_SEGMENTS.getAsInt();
    private static final double CABLE_THICKNESS = Config.CABLE_THICKNESS_IN_BLOCKS.getAsDouble();
    private static final double CABLE_SAG_AMOUNT = Config.CABLE_SAG_AMOUNT.getAsDouble();
    private static final int MAX_RENDER_DISTANCE = Config.MAX_RENDER_DISTANCE.getAsInt();

    // Color configuration for powered/unpowered cables
    private static final double UNPOWERED_RED = Config.UNPOWERED_RED.getAsDouble();
    private static final double POWERED_RED_BASE = Config.POWERED_RED_BASE.getAsDouble();
    private static final double POWERED_RED_BONUS = Config.POWERED_RED_BONUS.getAsDouble();
    private static final double GREEN_VALUE = Config.GREEN_VALUE.getAsDouble();
    private static final double BLUE_VALUE = Config.BLUE_VALUE.getAsDouble();

    /**
     * Defines how the cable should be rendered.
     * - Uses TRIANGLES mode for 3D geometry
     * - Disables culling so cables are visible from all angles
     * - Enables lighting and overlays for proper integration with Minecraft's lighting
     */
    public static final RenderType CABLE_RENDER_TYPE = RenderType.create(
            "redstone_cable_render",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.TRIANGLES,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorLightmapShader))
                    .setCullState(new RenderStateShard.CullStateShard(false)) // Disable culling so cables visible from both sides
                    .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                    .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                    .createCompositeState(false)
    );

    public RedstoneChainRenderer(BlockEntityRendererProvider.Context ctx) {
        super();
    }

    @Override
    public int getViewDistance() {
        return MAX_RENDER_DISTANCE;
    }

    /**
     * Called every frame to render all cables connected to this chain block.
     * Only renders each cable once by checking if this block's position is "less than" the connection.
     */
    @Override
    public void render(RedstoneChainEntity entity, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockPos blockPos = entity.getBlockPos();
        int power = entity.getSignal();

        // Render cables to all connected blocks
        for (BlockPos connection : entity.getConnections()) {
            if (shouldRenderCableTo(blockPos, connection)) {
                renderCableToConnection(blockPos, connection, power, stack, buffer, packedLight, packedOverlay);
            }
        }
    }

    /**
     * Determines if this block should render the cable to the given connection.
     * Only renders if this block's position is "less than" the connection to prevent
     * rendering the same cable twice (once from each end).
     */
    private boolean shouldRenderCableTo(BlockPos from, BlockPos to) {
        return from.compareTo(to) < 0;
    }

    /**
     * Renders a single cable from this block to a connected block.
     */
    private void renderCableToConnection(BlockPos from, BlockPos to, int power,
                                         PoseStack stack, MultiBufferSource buffer,
                                         int packedLight, int packedOverlay) {
        // Start point is center of this block (in local coordinates)
        Vec3 start = new Vec3(0.5, 0.5, 0.5);

        // End point is center of connected block (converted to local coordinates)
        Vec3 end = calculateLocalEndPoint(from, to);

        renderCurvedCuboid(stack, buffer, start, end, packedLight, packedOverlay, power);
    }

    /**
     * Converts world coordinates of connected block to local coordinates relative to this block.
     */
    private Vec3 calculateLocalEndPoint(BlockPos from, BlockPos to) {
        return Vec3.atCenterOf(to)
                .subtract(Vec3.atCenterOf(from))
                .add(0.5, 0.5, 0.5);
    }

    /**
     * Renders a curved cable between two points.
     * The cable is divided into segments and rendered as a 3D tube with realistic sag.
     */
    public static void renderCurvedCuboid(PoseStack poseStack, MultiBufferSource buffer,
                                          Vec3 from, Vec3 to, int light, int overlay, int power) {
        VertexConsumer builder = buffer.getBuffer(CABLE_RENDER_TYPE);

        // Get transformation matrices from pose stack
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // Calculate cable color based on power level
        CableColor color = calculateCableColor(power);

        // Render cable as multiple connected segments
        for (int i = 0; i < CABLE_SEGMENTS; i++) {
            renderCableSegment(builder, matrix, normal, from, to, i, light, overlay, color);
        }
    }

    /**
     * Renders a single segment of the cable.
     * Each segment connects two points along the curved path.
     */
    private static void renderCableSegment(VertexConsumer builder, Matrix4f matrix, Matrix3f normal,
                                           Vec3 from, Vec3 to, int segmentIndex,
                                           int light, int overlay, CableColor color) {


        // Calculate start and end parameters (0.0 to 1.0 along the cable)
        float t1 = segmentIndex / (float) CABLE_SEGMENTS;
        float t2 = (segmentIndex + 1) / (float) CABLE_SEGMENTS;

        // Get curved positions for this segment
        Vec3 p1 = interpolateCurved(from, to, t1);
        Vec3 p2 = interpolateCurved(from, to, t2);

        // Draw this segment as a 3D rectangular tube
        drawThickSegment(builder, matrix, normal, p1, p2, CABLE_THICKNESS,
                light, overlay, color.red, color.green, color.blue);
    }

    /**
     * Calculates cable color based on redstone power level.
     * Unpowered cables are dark red, powered cables are bright red.
     */
    private static CableColor calculateCableColor(int power) {
        double red;
        if (power > 0) {
            // Powered: bright red (0.9 to 1.0 based on power level)
            red = POWERED_RED_BASE + (power / 15.0f) * POWERED_RED_BONUS;
        } else {
            // Unpowered: dark red
            red = UNPOWERED_RED;
        }
        return new CableColor(red, GREEN_VALUE, BLUE_VALUE);
    }

    /**
     * Simple data class to hold RGB color values.
     */
    private record CableColor(double red, double green, double blue) {
    }

    /**
     * Interpolates a point along a curved path between two endpoints.
     * Creates a realistic sagging effect like a hanging cable.
     *
     * @param from Starting point
     * @param to   Ending point
     * @param t    Parameter from 0.0 (start) to 1.0 (end)
     * @return Point along the curved path
     */
    private static Vec3 interpolateCurved(Vec3 from, Vec3 to, float t) {
        // First calculate linear interpolation (straight line)
        Vec3 linear = from.lerp(to, t);

        // Check if cable is vertical (same X and Z coordinates)
        // Vertical cables don't sag
        if (isVerticalCable(from, to)) {
            return linear;
        }

        // Apply sagging curve using sine wave
        // Maximum sag occurs at t=0.5 (middle of cable)
        double curve = Math.sin(t * Math.PI) * CABLE_SAG_AMOUNT;

        // Add vertical sag to the linear position
        return new Vec3(linear.x, linear.y + curve, linear.z);
    }

    /**
     * Checks if a cable is perfectly vertical (no horizontal distance).
     */
    private static boolean isVerticalCable(Vec3 from, Vec3 to) {
        double horizontalDist = Math.abs(from.x - to.x) + Math.abs(from.z - to.z);
        return horizontalDist < 0.001;
    }

    /**
     * Draws a cable segment as a 3D rectangular tube.
     * The tube is rendered with double-sided faces so it's visible from all angles.
     *
     * @param builder   Vertex consumer to add geometry to
     * @param matrix    Transformation matrix for positioning
     * @param normal    Normal matrix for lighting
     * @param p1        Start point of segment
     * @param p2        End point of segment
     * @param thickness Thickness of the tube in blocks
     * @param light     Packed light value (from Minecraft's lighting system)
     * @param overlay   Packed overlay value (for damage/hurt effects)
     * @param r         Red color component (0.0 - 1.0)
     * @param g         Green color component (0.0 - 1.0)
     * @param b         Blue color component (0.0 - 1.0)
     */
    private static void drawThickSegment(VertexConsumer builder, Matrix4f matrix, Matrix3f normal,
                                         Vec3 p1, Vec3 p2, double thickness, int light, int overlay,
                                         double r, double g, double b) {
        // Step 1: Calculate orientation vectors for the tube
        TubeOrientation orientation = calculateTubeOrientation(p1, p2, thickness);

        // Step 2: Create 8 corners of the rectangular tube
        Vec3[] corners = createTubeCorners(p1, p2, orientation);

        // Step 3: Define and render all 6 faces of the tube
        renderTubeFaces(builder, matrix, normal, corners, light, overlay, r, g, b);
    }

    /**
     * Calculates the orientation vectors needed to construct a tube.
     * Uses cross products to create a coordinate system aligned with the tube direction.
     */
    private static TubeOrientation calculateTubeOrientation(Vec3 p1, Vec3 p2, double thickness) {
        // Direction vector from p1 to p2
        Vec3 dir = p2.subtract(p1).normalize();

        // Choose an "up" vector that's not parallel to direction
        // If tube is vertical (dir.y near 1.0), use X-axis instead of Y-axis
        Vec3 up = Math.abs(dir.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);

        // Calculate perpendicular vectors using cross product
        // "right" vector points to the side of the tube
        Vec3 right = dir.cross(up).normalize().scale(thickness);

        // "forward" vector completes the coordinate system
        Vec3 forward = dir.cross(right).normalize().scale(thickness);

        return new TubeOrientation(right, forward);
    }

    /**
     * Data class holding orientation vectors for tube construction.
     */
    private record TubeOrientation(Vec3 right, Vec3 forward) {
    }

    /**
     * Creates 8 corner vertices for a rectangular tube.
     * Returns corners in a specific order needed for face rendering.
     */
    private static Vec3[] createTubeCorners(Vec3 p1, Vec3 p2, TubeOrientation orientation) {
        Vec3 right = orientation.right;
        Vec3 forward = orientation.forward;

        // Create 8 corners: 4 at p1 end, 4 at p2 end
        return new Vec3[]{
                // Bottom face (p1 end) - corners 0-3
                p1.add(right).add(forward),           // 0: p1 +right +forward
                p1.add(right).subtract(forward),      // 1: p1 +right -forward
                p1.subtract(right).subtract(forward), // 2: p1 -right -forward
                p1.subtract(right).add(forward),      // 3: p1 -right +forward

                // Top face (p2 end) - corners 4-7
                p2.add(right).add(forward),           // 4: p2 +right +forward
                p2.add(right).subtract(forward),      // 5: p2 +right -forward
                p2.subtract(right).subtract(forward), // 6: p2 -right -forward
                p2.subtract(right).add(forward),      // 7: p2 -right +forward
        };
    }

    /**
     * Renders all 6 faces of the rectangular tube.
     * Each face is rendered double-sided (front and back) for visibility from all angles.
     */
    private static void renderTubeFaces(VertexConsumer builder, Matrix4f matrix, Matrix3f normal,
                                        Vec3[] corners, int light, int overlay, double r, double g, double b) {
        // Define the 6 faces of the rectangular tube
        // Each face is defined by 4 corner indices in counter-clockwise order (when viewed from outside)
        int[][] faces = {
                {0, 1, 2, 3}, // Bottom face (p1 end)
                {7, 6, 5, 4}, // Top face (p2 end)
                {0, 4, 5, 1}, // Right side
                {1, 5, 6, 2}, // Front side
                {2, 6, 7, 3}, // Left side
                {3, 7, 4, 0}, // Back side
        };

        // Render each face double-sided (front and back)
        for (int[] face : faces) {
            renderDoubleSidedQuad(builder, matrix, corners, face, light, overlay, r, g, b);
        }
    }

    /**
     * Renders a quad (4-sided face) with double-sided rendering.
     * The quad is split into two triangles and rendered from both sides.
     *
     * @param face Array of 4 corner indices defining the quad
     */
    private static void renderDoubleSidedQuad(VertexConsumer builder, Matrix4f matrix,
                                              Vec3[] corners, int[] face,
                                              int light, int overlay, double r, double g, double b) {
        // Calculate normal vector for this face (perpendicular to surface)
        Vec3 normalVec = calculateFaceNormal(corners, face);

        // Render front face (normal pointing outward)
        renderQuadSide(builder, matrix, corners, face, normalVec, light, overlay, r, g, b, false);

        // Render back face (normal pointing inward, reversed winding)
        renderQuadSide(builder, matrix, corners, face, normalVec, light, overlay, r, g, b, true);
    }

    /**
     * Calculates the normal vector for a quad face.
     * The normal is perpendicular to the surface and points outward.
     */
    private static Vec3 calculateFaceNormal(Vec3[] corners, int[] face) {
        Vec3 v0 = corners[face[0]];
        Vec3 v1 = corners[face[1]];
        Vec3 v2 = corners[face[2]];

        // Calculate two edge vectors
        Vec3 edge1 = v1.subtract(v0);
        Vec3 edge2 = v2.subtract(v0);

        // Cross product gives perpendicular vector
        return edge1.cross(edge2).normalize();
    }

    /**
     * Renders one side of a quad (split into two triangles).
     *
     * @param reversed If true, reverses winding order and inverts normal (for back face)
     */
    private static void renderQuadSide(VertexConsumer builder, Matrix4f matrix,
                                       Vec3[] corners, int[] face, Vec3 normalVec,
                                       int light, int overlay, double r, double g, double b,
                                       boolean reversed) {
        // Choose normal direction based on which side we're rendering
        float nx = (float) (reversed ? -normalVec.x : normalVec.x);
        float ny = (float) (reversed ? -normalVec.y : normalVec.y);
        float nz = (float) (reversed ? -normalVec.z : normalVec.z);

        if (reversed) {
            // Back face: reversed winding order (0, 2, 1) and (0, 3, 2)
            addTriangle(builder, matrix, corners, face[0], face[2], face[1], nx, ny, nz, light, overlay, r, g, b);
            addTriangle(builder, matrix, corners, face[0], face[3], face[2], nx, ny, nz, light, overlay, r, g, b);
        } else {
            // Front face: normal winding order (0, 1, 2) and (0, 2, 3)
            addTriangle(builder, matrix, corners, face[0], face[1], face[2], nx, ny, nz, light, overlay, r, g, b);
            addTriangle(builder, matrix, corners, face[0], face[2], face[3], nx, ny, nz, light, overlay, r, g, b);
        }
    }

    /**
     * Adds a single triangle (3 vertices) to the vertex builder.
     */
    private static void addTriangle(VertexConsumer builder, Matrix4f matrix, Vec3[] corners,
                                    int i0, int i1, int i2,
                                    float nx, float ny, float nz,
                                    int light, int overlay, double r, double g, double b) {
        // Add first vertex
        addVertex(builder, matrix, corners[i0], nx, ny, nz, light, overlay, r, g, b);
        // Add second vertex
        addVertex(builder, matrix, corners[i1], nx, ny, nz, light, overlay, r, g, b);
        // Add third vertex
        addVertex(builder, matrix, corners[i2], nx, ny, nz, light, overlay, r, g, b);
    }

    /**
     * Adds a single vertex to the builder with all required attributes.
     */
    private static void addVertex(VertexConsumer builder, Matrix4f matrix, Vec3 pos,
                                  float nx, float ny, float nz,
                                  int light, int overlay, double r, double g, double b) {
        builder.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor((float) r, (float) g, (float) b, 1f)           // RGBA color (alpha=1.0 = fully opaque)
                .setUv(0, 0)                     // Texture coordinates (not used for solid color)
                .setOverlay(overlay)              // Overlay for damage/hurt effects
                .setLight(light)                  // Lighting from Minecraft's lighting system
                .setNormal(nx, ny, nz);          // Normal for lighting calculations
    }
}
