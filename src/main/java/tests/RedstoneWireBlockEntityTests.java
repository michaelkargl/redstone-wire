package tests;

import at.osa.redstonewire.RedstoneWireBlockEntity;
import at.osa.redstonewire.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

public class RedstoneWireBlockEntityTests {
    private static final BlockPos FIRST_CONNECTOR = new BlockPos(0, 0, 0);
    private static final BlockPos SECOND_CONNECTOR = new BlockPos(1, 0, 0);

    // Registered in RedstoneWire.TEST_FUNCTIONS and described by a test_instance JSON file.
    //
    // NOTE: this test borrows structuretests.teststructurecomposition as a bounded sandbox
    // and overwrites cells (0,0,0) and (1,0,0) with connectors. That NBT is therefore shared
    // with StructureTests - it must stay at least 2 blocks wide on X. See StructureTests.
    public static void breakingBlockRemovesReverseConnection(GameTestHelper helper) {
        helper.setBlock(FIRST_CONNECTOR, ModBlocks.REDSTONE_CONNECTOR_BLOCK.get());
        helper.setBlock(SECOND_CONNECTOR, ModBlocks.REDSTONE_CONNECTOR_BLOCK.get());

        var firstConnector = helper.getBlockEntity(FIRST_CONNECTOR, RedstoneWireBlockEntity.class);
        var secondConnector = helper.getBlockEntity(SECOND_CONNECTOR, RedstoneWireBlockEntity.class);
        var firstWorldPosition = helper.absolutePos(FIRST_CONNECTOR);
        var secondWorldPosition = helper.absolutePos(SECOND_CONNECTOR);

        firstConnector.createBidirectionalConnection(
                helper.getLevel(), firstWorldPosition, secondWorldPosition, null);

        helper.assertTrue(
                firstConnector.getConnections().contains(secondWorldPosition),
                "The first connector should point to the second connector before breaking");
        helper.assertTrue(
                secondConnector.getConnections().contains(firstWorldPosition),
                "The second connector should point back to the first connector before breaking");

        helper.destroyBlock(FIRST_CONNECTOR);

        helper.assertTrue(
                helper.getLevel().getBlockEntity(firstWorldPosition) == null,
                "Breaking the first connector should remove its block entity");
        helper.assertTrue(
                secondConnector.getConnections().isEmpty(),
                "Breaking one connector should remove its reverse connection from the survivor");
        helper.succeed();
    }
}
