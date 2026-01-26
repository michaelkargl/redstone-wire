package at.osa.minecraftplayground;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameTestFramework {

    protected static void pullLever(GameTestHelper helper, BlockPos relativePosition) {
        var absolutePos = helper.absolutePos(relativePosition);
        var currentState = helper.getBlockState(relativePosition);

        var block = currentState.getBlock();
        if (!(block instanceof LeverBlock leverBlock)) {
            throw new IllegalStateException(String.format(String.format("Block at lever position %s is not a lever but a %s", relativePosition, block.getName())));
        }

        // triggers block state change (typically on the next game tick, not immediately)
        leverBlock.pull(currentState, helper.getLevel(), absolutePos, null);
    }

    protected static void assertLeverIsPowered(GameTestHelper helper, int x, int y, int z, boolean expected) {
        assertLeverIsPowered(helper, new BlockPos(x, y, z), expected);
    }

    protected static void assertLeverIsPowered(GameTestHelper helper, BlockPos leverPosition, boolean expected) {
        var blockState = helper.getBlockState(leverPosition);
        assertLeverIsPowered(helper, blockState, expected);
    }

    protected static void assertLeverIsPowered(GameTestHelper helper, BlockState leverBlockState, boolean expected) {
        var block = leverBlockState.getBlock();
        if (!(block instanceof LeverBlock leverBlock)) {
            helper.fail("Block at lever position is not a lever but a %s".formatted(block.getName()));
        }

        boolean isPowered = leverBlockState.getValue(LeverBlock.POWERED);
        if (expected) {
            helper.assertTrue(isPowered, "Lever is not powered");
        } else {
            helper.assertFalse(isPowered, "Lever is powered");
        }
    }

    protected static void assertRedstoneLampIsLit(GameTestHelper helper, BlockState redstoneLampState) {
        if (redstoneLampState.getBlock() instanceof RedstoneLampBlock redstoneLampBlock) {
            var isLit = redstoneLampState.getValue(RedstoneLampBlock.LIT);
            helper.assertTrue(isLit, "Redstone Lamp is not lit");
        } else {
            helper.fail("Block at redstone lamp position is not a redstone lamp");
        }
    }

    protected static void assertRedstoneLampIsLit(GameTestHelper helper, BlockPos position) {
        assertRedstoneLampIsLit(helper, helper.getBlockState(position));
    }

    protected static String getBlockNameAtPosition(GameTestHelper helper, int x, int y, int z) {
        var pos = new BlockPos(x, y, z);
        var state = helper.getBlockState(pos);
        var block = state.getBlock();
        return block.getName().getString();
    }

    protected static void assertBlockNameAtPosition(GameTestHelper helper, String expected, int x, int y, int z) {
        var blockName = getBlockNameAtPosition(helper, x, y, z);
        helper.assertTrue(blockName.equals(expected), "Block at position is not a %s but a %s".formatted(expected, blockName));
    }

    protected static void assertBlockNameAtPosition(GameTestHelper helper, String expected, BlockPos pos) {
        assertBlockNameAtPosition(helper, expected, pos.getX(), pos.getY(), pos.getZ());
    }

    protected static void validate2DXZGrid(GameTestHelper helper, String[][] expectedGrid, int startX, int startZ, int y) {
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
     * @param helper The GameTestHelper providing test context
     * @param blockPos The position of the block to interact with (relative coordinates)
     * @param itemStack The item stack the player is holding
     */
    protected static void useItemOn(GameTestHelper helper, BlockPos blockPos, ItemStack itemStack) {
        var absolutePos = helper.absolutePos(blockPos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        var hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.DOWN, absolutePos, false);
        var context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, itemStack, hitResult);

        itemStack.useOn(context);
    }

    protected static void ensureRedstoneWireBlock(Block block) {
        if (!(block instanceof RedStoneWireBlock)) {
            throw new IllegalArgumentException("Block is not a redstone wire but a " + block.getName());
        }
    }

    protected static void assertRedstoneWirePowered(GameTestHelper helper, BlockPos pos, boolean expectedState) {
        var blockState = helper.getBlockState(pos);
        var block = blockState.getBlock();
        ensureRedstoneWireBlock(block);

        var powerLevel = blockState.getValue(RedStoneWireBlock.POWER);
        if(expectedState) {
            helper.assertValueEqual(powerLevel, 15, "Redstone wire at is not powered");
        } else {
            helper.assertValueEqual(powerLevel, 0, "Redstone wire at is powered");
        }
    }
}
