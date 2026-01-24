package at.osa.minecraftplayground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;


abstract class KamiGameTestFramework {
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

    protected static void assertLeverIsPowered(GameTestHelper helper, BlockState leverBlockState) {
        if (leverBlockState.getBlock() instanceof LeverBlock leverBlock) {
            var isPowered = leverBlockState.getValue(LeverBlock.POWERED);
            helper.assertTrue(isPowered.booleanValue(), "Lever is not powered");
        } else {
            helper.fail("Block at lever position is not a lever");
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
    protected static void crouchClickBlock(GameTestHelper helper, BlockPos blockPos, ItemStack itemStack) {
        // API method: ServerPlayer player = helper.makeMockPlayer(GameType.SURVIVAL)
        //
        // Steps to implement (8-10 lines):
        // 1. Convert blockPos to absolute: var absolutePos = helper.absolutePos(blockPos)
        // 2. Create mock player: var player = helper.makeMockPlayer(GameType.SURVIVAL)
        // 3. Set crouching: player.setShiftKeyDown(true)
        // 4. Give item: player.setItemInHand(InteractionHand.MAIN_HAND, itemStack)
        // 5. Create BlockHitResult:
        //    var hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false)
        // 6. Create UseOnContext:
        //    var context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, itemStack, hitResult)
        // 7. Execute interaction: itemStack.useOn(context)
        //
        // Design choices you can customize:
        // - Direction.UP means clicking the top face (try Direction.NORTH, SOUTH, etc. for sides)
        // - Vec3.atCenterOf(absolutePos) centers the hit (or use .atBottomCenterOf(), or custom offsets)

        var absolutePos = helper.absolutePos(blockPos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        var hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.DOWN, absolutePos, false);
        var context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, itemStack, hitResult);

        itemStack.useOn(context);
    }

    /**
     * Asserts that two RedstoneChain blocks are connected to each other.
     * Verifies bidirectional connection: A→B and B→A.
 *
     * @param helper The GameTestHelper
     * @param pos1 First chain block position
     * @param pos2 Second chain block position
     */
    protected static void assertChainBlocksAreConnected(GameTestHelper helper, BlockPos pos1, BlockPos pos2) {
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

// structure name is <classname>.<testname>.nbt (all lowercase)
// create a structure by
// 1. creating a @GameTest annotated test function
// 2. running the dev server
// 3. connecting via dev client
// 4. run /test create <classname>.<testname>
@GameTestHolder("minecraftplayground")
public class SimpleGameTests extends KamiGameTestFramework  {

    @GameTest
    public static void coordinatesTest(GameTestHelper helper) {
        //
        //   x-axis: horizontal (west ← → east)
        //   y-axis: vertical (down ↓ ↑ up)
        //   z-axis: depth (north ↑ ↓ south)
        //
        // Visual representation from above (bird's eye view):
        //        x →
        //      0   1   2   3   4
        //    ┌─────────────────────
        // z 0│ BW  LBW  DB  RW  BrW
        // ↓ 1│ LBW  BW  DB  BrW  RW
        //   2│ ISB ISB  BlW RNB RNB
        //   3│ YW  OW   Br  GW  LW
        //   4│ OW  YW   Br  LW  GW
        //
        // Legend: BW=Blue Wool, LBW=Light Blue Wool, DB=Deepslate Bricks,
        //         RW=Red Wool, BrW=Brown Wool, ISB=Infested Stone Bricks,
        //         BlW=Black Wool, RNB=Red Nether Bricks, YW=Yellow Wool,
        //         OW=Orange Wool, Br=Bricks, GW=Green Wool, LW=Lime Wool

        // Test origin and reference points
        assertBlockNameAtPosition(helper, "Structure Block", 0, 0, 0); // (0,0,0) - structure origin
        assertBlockNameAtPosition(helper, "Air", 0, 2, 0);  // (0,2,0) - two blocks above origin

        String[][] expectedGrid = {
            {"Blue Wool", "Light Blue Wool", "Deepslate Bricks", "Red Wool", "Brown Wool"},                    // z=0
            {"Light Blue Wool", "Blue Wool", "Deepslate Bricks", "Brown Wool", "Red Wool"},                    // z=1
            {"Infested Stone Bricks", "Infested Stone Bricks", "Black Wool", "Red Nether Bricks", "Red Nether Bricks"}, // z=2
            {"Yellow Wool", "Orange Wool", "Bricks", "Green Wool", "Lime Wool"},                               // z=3
            {"Orange Wool", "Yellow Wool", "Bricks", "Lime Wool", "Green Wool"}                                // z=4
        };

        validate2DXZGrid(helper, expectedGrid, 0, 0, 1);

        helper.succeed();
    }

    @GameTest
    public static void leverActionTest(GameTestHelper helper) {
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

    @GameTest(template="leverinputoutputtest")
    public static void leverinputoutputtest(GameTestHelper helper) {
        var inputChainBlockPosition = new BlockPos(1, 2, 1);
        var outputChainBlockPosition = new BlockPos(3, 2, 3);

        // Verify the structure has the correct blocks
        assertBlockNameAtPosition(helper, "Redstone Chain", inputChainBlockPosition);
        assertBlockNameAtPosition(helper, "Redstone Chain", outputChainBlockPosition);

        // Create a chain connector item
        var connectorItem = new ItemStack(MinecraftPlayground.REDSTONE_CHAIN_CONNECTOR.get());

        // Schedule the connection process
        helper.runAtTickTime(1, () -> {
            // First click: crouch-click the first chain block to save its position
            crouchClickBlock(helper, inputChainBlockPosition, connectorItem);
        });

        helper.runAtTickTime(3, () -> {
            // Second click: crouch-click the second chain block to create the connection
            crouchClickBlock(helper, outputChainBlockPosition, connectorItem);
        });

        // Verify the connection was created (after both clicks complete)
        helper.runAtTickTime(5, () -> {
            assertChainBlocksAreConnected(helper, inputChainBlockPosition, outputChainBlockPosition);
            helper.succeed();
        });
    }
}
