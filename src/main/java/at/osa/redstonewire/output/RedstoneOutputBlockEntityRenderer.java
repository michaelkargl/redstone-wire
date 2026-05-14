package at.osa.redstonewire.output;

import at.osa.redstonewire.RedstoneWireBlock;
import at.osa.redstonewire.renderer.CableRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Renders cables from a RedstoneOutputBlock to each of its linked ConnectorBlocks.
 */
public class RedstoneOutputBlockEntityRenderer implements BlockEntityRenderer<RedstoneOutputBlockEntity> {

    // Distance from block center to the antennae X-axis center in the NORTH-facing model (12/16 - 0.5)
    private static final double ANTENNAE_X_OFFSET = 0.25;

    public RedstoneOutputBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        super();
    }

    @Override
    public void render(RedstoneOutputBlockEntity entity, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockPos blockPos = entity.getBlockPos();
        int power = entity.getBlockState().getValue(RedstoneOutputBlock.POWER);
        var facing = entity.getBlockState().getValue(RedstoneWireBlock.FACING);

        double startX = 0.5, startZ = 0.5;
        switch (facing) {
            case NORTH -> startX = 0.5 + ANTENNAE_X_OFFSET;
            case SOUTH -> startX = 0.5 - ANTENNAE_X_OFFSET;
            case EAST  -> startZ = 0.5 + ANTENNAE_X_OFFSET;
            case WEST  -> startZ = 0.5 - ANTENNAE_X_OFFSET;
        }

        for (BlockPos connection : entity.getConnections()) {
            Vec3 start = new Vec3(startX, RedstoneWireBlock.ANTENNA_TIP_Y, startZ);
            Vec3 end = Vec3.atCenterOf(connection)
                    .subtract(Vec3.atCenterOf(blockPos))
                    .add(0.5, RedstoneWireBlock.ANTENNA_TIP_Y, 0.5);
            CableRenderer.renderCable(stack, buffer, start, end, power, packedLight, packedOverlay);
        }
    }
}
