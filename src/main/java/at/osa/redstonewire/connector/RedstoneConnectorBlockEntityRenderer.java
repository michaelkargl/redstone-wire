package at.osa.redstonewire.connector;

import at.osa.redstonewire.renderer.CableRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import at.osa.redstonewire.renderer.CableRenderer.CableBlockEntityRenderState;

/**
 * Renders cables between connected RedstoneConnector blocks.
 * Cables sag realistically and are colored based on power level.
 *
 * @credit Create Crafts & Additions: https://github.com/mrh0/createaddition
 * @credit Overhead Redstone Wires: https://github.com/MaxLegend/OverheadRedstoneWires
 */
public class RedstoneConnectorBlockEntityRenderer
        implements BlockEntityRenderer<RedstoneConnectorBlockEntity, CableBlockEntityRenderState> {

    public RedstoneConnectorBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        super();
    }

    @Override
    public CableBlockEntityRenderState createRenderState() {
        return new CableBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(RedstoneConnectorBlockEntity entity, CableBlockEntityRenderState state,
                                   float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        state.connections = entity.getConnections(); // already an immutable snapshot
        state.power = entity.getSignal();
    }

    @Override
    public void submit(CableBlockEntityRenderState state, PoseStack stack,
                       SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BlockPos blockPos = state.blockPos;
         var antennaeAttachmentPointY = 11.0 / 16.0; // Blockheight=16

        for (BlockPos connection : state.connections) {
            // Only render cable once per connection (compareTo ensures A->B is only rendered from A)
            if (blockPos.compareTo(connection) < 0) {
                Vec3 start = new Vec3(0.5, antennaeAttachmentPointY, 0.5);
                Vec3 end = Vec3.atCenterOf(connection)
                        .subtract(Vec3.atCenterOf(blockPos))
                        .add(0.5, antennaeAttachmentPointY, 0.5);
                CableRenderer.renderCable(stack, nodeCollector, start, end, state.power, state.lightCoords);
            }
        }
    }
}
