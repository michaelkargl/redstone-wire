package at.osa.minecraftplayground;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Defines the render type for redstone cables.
 * Handles shader, lighting, and face culling configuration.
 */
public class RedstoneRenderType {

    /**
     * Custom render type for cables.
     * - Uses TRIANGLES mode for 3D geometry
     * - Disables culling so cables are visible from all angles
     * - Enables lighting and overlays for proper integration with Minecraft's lighting
     */
    public static final RenderType CABLE_RENDERTYPE = RenderType.create(
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

    private RedstoneRenderType() {
    }
}
