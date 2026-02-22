package tests;

import at.osa.redstonewire.RedstoneChainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TestHelpers {

    public static void pullLever(GameTestHelper helper, BlockPos relativePosition) {
        var absolutePos = helper.absolutePos(relativePosition);
        var currentState = helper.getBlockState(relativePosition);

        var block = currentState.getBlock();
        if (!(block instanceof LeverBlock leverBlock)) {
            throw new IllegalStateException(String.format(String.format("Block at lever position %s is not a lever but a %s", relativePosition, block.getName())));
        }

        // triggers block state change (typically on the next game tick, not immediately)
        leverBlock.pull(currentState, helper.getLevel(), absolutePos, null);
    }

    public static void assertLeverIsPowered(GameTestHelper helper, int x, int y, int z, boolean expected) {
        assertLeverIsPowered(helper, new BlockPos(x, y, z), expected);
    }

    public static void assertLeverIsPowered(GameTestHelper helper, BlockPos leverPosition, boolean expected) {
        var state = helper.getBlockState(leverPosition);
        var block = state.getBlock();

        if (!(block instanceof LeverBlock leverBlock)) {
            helper.fail("Block at lever position %s is not a lever but a %s".formatted(leverPosition, block.getName()));
        }

        boolean isPowered = state.getValue(LeverBlock.POWERED);
        helper.assertTrue(
                isPowered == expected,
                "Expected lever at position %s to be %s but it is %s.".formatted(leverPosition, expected ? "powered" : "unpowered", isPowered ? "powered" : "unpowered"));
    }

    public static void assertRedstoneLampIsLit(GameTestHelper helper, BlockPos position, boolean isLit) {
        var state = helper.getBlockState(position);
        var block = state.getBlock();

        if (!(block instanceof RedstoneLampBlock)) {
            helper.fail("Block at position %s is not a redstone lamp but a %s".formatted(position, block.getName()));
        }

        var actualIsLit = state.getValue(RedstoneLampBlock.LIT);
        helper.assertValueEqual(
                actualIsLit,
                isLit,
                "Expected redstone lamp to be %s but it is %s".formatted(isLit ? "lit" : "unlit", actualIsLit ? "lit" : "unlit"));
    }

    public static void assertRedstoneLampIsLit(GameTestHelper helper, int x, int y, int z, boolean isLit) {
        assertRedstoneLampIsLit(helper, new BlockPos(x, y, z), isLit);
    }

    public static String getBlockNameAtPosition(GameTestHelper helper, int x, int y, int z) {
        var pos = new BlockPos(x, y, z);
        var state = helper.getBlockState(pos);
        var block = state.getBlock();
        return block.getName().getString();
    }

    public static void assertBlockNameAtPosition(GameTestHelper helper, String expected, int x, int y, int z) {
        var blockName = getBlockNameAtPosition(helper, x, y, z);
        helper.assertTrue(blockName.equals(expected), "Block at position is not a %s but a %s".formatted(expected, blockName));
    }

    public static void assertBlockNameAtPosition(GameTestHelper helper, String expected, BlockPos pos) {
        assertBlockNameAtPosition(helper, expected, pos.getX(), pos.getY(), pos.getZ());
    }

    public static void validate2DXZGrid(GameTestHelper helper, String[][] expectedGrid, int startX, int startZ, int y) {
        for (int z = startZ; z < expectedGrid.length; z++) {
            for (int x = startX; x < expectedGrid[z].length; x++) {
                assertBlockNameAtPosition(helper, expectedGrid[z][x], x, y, z);
            }
        }
    }

    /**
     * Simulates a player crouch-clicking on a block with an item.
     * This is used to test interactions that require shift-clicking (sneaking + right-click).
     *
     * @param helper    The GameTestHelper providing test context
     * @param blockPos  The position of the block to interact with (relative coordinates)
     * @param itemStack The item stack the player is holding
     */
    public static void useItemOn(GameTestHelper helper, BlockPos blockPos, ItemStack itemStack) {
        var absolutePos = helper.absolutePos(blockPos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        var hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.DOWN, absolutePos, false);
        var context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, itemStack, hitResult);

        itemStack.useOn(context);
    }

    public static void ensureRedstoneWireBlock(Block block) {
        if (!(block instanceof RedStoneWireBlock)) {
            throw new IllegalArgumentException("Block is not a redstone wire but a " + block.getName());
        }
    }

    public static void assertRedstoneWirePowered(GameTestHelper helper, BlockPos pos, boolean expectedState) {
        var blockState = helper.getBlockState(pos);
        var block = blockState.getBlock();
        ensureRedstoneWireBlock(block);

        var powerLevel = blockState.getValue(RedStoneWireBlock.POWER);
        if (expectedState) {
            helper.assertValueEqual(powerLevel, 15, "Redstone wire at is not powered");
        } else {
            helper.assertValueEqual(powerLevel, 0, "Redstone wire at is powered");
        }
    }

    /**
     * Asserts that two RedstoneChain blocks are connected to each other.
     * Verifies bidirectional connection: A→B and B→A.
     *
     * @param helper The GameTestHelper
     * @param pos1   First chain block position
     * @param pos2   Second chain block position
     */
    public static void assertChainBlocksAreConnected(GameTestHelper helper, BlockPos pos1, BlockPos pos2) {
        var absolutePos1 = helper.absolutePos(pos1);
        var absolutePos2 = helper.absolutePos(pos2);

        BlockEntity be1 = helper.getLevel().getBlockEntity(absolutePos1);
        BlockEntity be2 = helper.getLevel().getBlockEntity(absolutePos2);

        if (!(be1 instanceof RedstoneChainEntity chain1)) {
            helper.fail("Block at " + pos1 + " is not a RedstoneChainEntity");
            return;
        }

        if (!(be2 instanceof RedstoneChainEntity chain2)) {
            helper.fail("Block at " + pos2 + " is not a RedstoneChainEntity");
            return;
        }

        // Check if chain1 has a connection to pos2
        boolean chain1HasConnectionToChain2 = chain1.getConnections().contains(absolutePos2);
        helper.assertTrue(chain1HasConnectionToChain2,
                "Chain at " + pos1 + " does not have a connection to " + pos2);

        // Check if chain2 has a connection to pos1
        boolean chain2HasConnectionToChain1 = chain2.getConnections().contains(absolutePos1);
        helper.assertTrue(chain2HasConnectionToChain1,
                "Chain at " + pos2 + " does not have a connection to " + pos1);
    }
}
