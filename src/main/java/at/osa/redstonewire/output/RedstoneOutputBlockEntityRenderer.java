package at.osa.redstonewire.output;

import at.osa.redstonewire.CableRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Renders cables from a RedstoneOutputBlock to each of its linked ConnectorBlocks.
 */
public class RedstoneOutputBlockEntityRenderer implements BlockEntityRenderer<RedstoneOutputBlockEntity> {

    public RedstoneOutputBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        super();
    }

    @Override
    public void render(RedstoneOutputBlockEntity entity, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockPos blockPos = entity.getBlockPos();
        int power = entity.getBlockState().getValue(RedstoneOutputBlock.POWER);

        for (BlockPos connection : entity.getConnections()) {
            Vec3 start = new Vec3(0.5, 1.0, 0.5);
            Vec3 end = Vec3.atCenterOf(connection)
                    .subtract(Vec3.atCenterOf(blockPos))
                    .add(0.5, 1.0, 0.5);
            CableRenderer.renderCable(stack, buffer, start, end, power, packedLight, packedOverlay);
        }
    }
}
