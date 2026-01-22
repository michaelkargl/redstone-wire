package at.osa.minecraftplayground;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;


// structure name is <classname>.<testname>.nbt (all lowercase)
@GameTestHolder("minecraftplayground")
public class SimpleGameTests {

    @GameTest
    public static void simpleMathTest(GameTestHelper helper) {
        int result = 1 + 2;
        if (result == 3) {
            helper.succeed();
        } else {
            helper.fail("Expected 1+2 to equal 3, but got " + result);
        }
    }

    @GameTest
    public static void leverActionTest(GameTestHelper helper) {
        //   x
        //   * ---->
        // z |\
        //   v y
        //
        // 1
        // 2   3   4   5   6
        //         7
        //         8
        assertBlockNameAtPosition(helper, "Air", 0, 0, 1); // 0
        assertBlockNameAtPosition(helper, "Structure Block", 0, 0, 0); // 1
        assertBlockNameAtPosition(helper, "Blue Wool", 0, 1, 0);  // 2
        assertBlockNameAtPosition(helper, "Blue Concrete", 1, 1, 0); // 3
        assertBlockNameAtPosition(helper, "Light Blue Wool", 2, 1, 0); // 4
        assertBlockNameAtPosition(helper, "Purple Terracotta", 3, 1, 0); // 5
        assertBlockNameAtPosition(helper, "Magenta Terracotta", 4, 1, 0); // 6
        assertBlockNameAtPosition(helper, "Cyan Wool", 2, 1, 1); // 8
        assertBlockNameAtPosition(helper, "Purple Wool", 2, 1, 2); // 8

        var redstoneLampPosition = new BlockPos(2, 2, 2);
        var leverBlockPosition = new BlockPos(2, 3, 2);
        assertBlockNameAtPosition(helper, "Redstone Lamp", redstoneLampPosition); // 9
        assertBlockNameAtPosition(helper, "Lever", leverBlockPosition); // 10


        var leverBlockState = helper.getBlockState(leverBlockPosition);
        if (leverBlockState.getBlock() instanceof LeverBlock leverBlock) {
            // run the lever action at tick 1
            helper.runAtTickTime(1, () -> pullLever(helper, leverBlockPosition));

            // Schedule assertions after the lever action completes
            helper.runAtTickTime(2, () -> {
                // Re-fetch block state since BlockState is immutable
                var currentLeverState = helper.getBlockState(leverBlockPosition);
                assertLeverIsPowered(helper, currentLeverState);
                assertRedstoneLampIsLit(helper, redstoneLampPosition);
                helper.succeed();
            });
        } else {
            helper.fail("Block at lever position is not a lever");
        }
    }

    private static void pullLever(GameTestHelper helper, BlockPos relativePosition) {
        var absolutePos = helper.absolutePos(relativePosition);
        var currentState = helper.getBlockState(relativePosition);

        var block = currentState.getBlock();
        if (!(block instanceof LeverBlock leverBlock)) {
            throw new IllegalStateException(String.format(String.format("Block at lever position %s is not a lever but a %s", relativePosition, block.getName())));
        }

        // triggers block state change (typically on the next game tick, not immediately)
        leverBlock.pull(currentState, helper.getLevel(), absolutePos, null);
    }

    private static void assertLeverIsPowered(GameTestHelper helper, BlockState leverBlockState) {
        if (leverBlockState.getBlock() instanceof LeverBlock leverBlock) {
            var isPowered = leverBlockState.getValue(LeverBlock.POWERED);
            helper.assertTrue(isPowered.booleanValue(), "Lever is not powered");
        } else {
            helper.fail("Block at lever position is not a lever");
        }
    }

    private static void assertRedstoneLampIsLit(GameTestHelper helper, BlockState redstoneLampState) {
        if (redstoneLampState.getBlock() instanceof RedstoneLampBlock redstoneLampBlock) {
            var isLit = redstoneLampState.getValue(RedstoneLampBlock.LIT);
            helper.assertTrue(isLit, "Redstone Lamp is not lit");
        } else {
            helper.fail("Block at redstone lamp position is not a redstone lamp");
        }
    }

    private static void assertRedstoneLampIsLit(GameTestHelper helper, BlockPos position) {
        assertRedstoneLampIsLit(helper, helper.getBlockState(position));
    }

    private static String getBlockNameAtPosition(GameTestHelper helper, int x, int y, int z) {
        var pos = new BlockPos(x, y, z);
        var state = helper.getBlockState(pos);
        var block = state.getBlock();
        return block.getName().getString();
    }

    private static void assertBlockNameAtPosition(GameTestHelper helper, String expected, int x, int y, int z) {
        var blockName = getBlockNameAtPosition(helper, x, y, z);
        helper.assertTrue(blockName.equals(expected), "Block at position is not a %s but a %s".formatted(expected, blockName));
    }

    private static void assertBlockNameAtPosition(GameTestHelper helper, String expected, BlockPos pos) {
        assertBlockNameAtPosition(helper, expected, pos.getX(), pos.getY(), pos.getZ());
    }
}
