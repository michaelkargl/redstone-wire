package at.osa.redstonewire.connector;

import at.osa.redstonewire.CableRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Renders cables between connected RedstoneConnector blocks.
 * Cables sag realistically and are colored based on power level.
 *
 * @credit Create Crafts & Additions: https://github.com/mrh0/createaddition
 * @credit Overhead Redstone Wires: https://github.com/MaxLegend/OverheadRedstoneWires
 */
public class RedstoneConnectorBlockEntityRenderer implements BlockEntityRenderer<RedstoneConnectorBlockEntity> {

    public RedstoneConnectorBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        super();
    }

    @Override
    public void render(RedstoneConnectorBlockEntity entity, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockPos blockPos = entity.getBlockPos();
        int power = entity.getSignal();

        for (BlockPos connection : entity.getConnections()) {
            // Only render cable once per connection (compareTo ensures A->B is only rendered from A)
            if (blockPos.compareTo(connection) < 0) {
                Vec3 start = new Vec3(0.5, 1.0, 0.5);
                Vec3 end = Vec3.atCenterOf(connection)
                        .subtract(Vec3.atCenterOf(blockPos))
                        .add(0.5, 1.0, 0.5);
                CableRenderer.renderCable(stack, buffer, start, end, power, packedLight, packedOverlay);
            }
        }
    }
}
