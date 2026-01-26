package at.osa.minecraftplayground;

import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;

// structure name is <classname>.<testname>.nbt (all lowercase)
// create a structure by
// 1. creating a @GameTest annotated test function
// 2. running the dev server
// 3. connecting via dev client
// 4. run /test create <classname>.<testname>
@GameTestHolder("minecraftplayground")
public class SimpleGameTests extends GameTestFramework {

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
                assertLeverIsPowered(helper, currentLeverState, true);
                assertRedstoneLampIsLit(helper, redstoneLampPosition);
                helper.succeed();
            });
        } else {
            helper.fail("Block at lever position is not a lever");
        }
    }

    // Visual representation from above (bird's eye view):
    //        x →
    //      0   1   2   3   4
    //    ┌─────────────────────
    // z 0│ BW  LBW  DB  RW  BrW
    // ↓ 1│ LBW  BW  DB  BrW  RW
    //   2│ ISB ISB  BlW RNB RNB
    //   3│ YW  OW   Br  GW  LW
    //   4│ OW  YW   Br  LW  GW
    @GameTest(template = "leverinputoutputtest")
    public static void leverinputoutputtest(GameTestHelper helper) {
        var tick = 0;
        var inputChainBlockPosition = new BlockPos(1, 2, 1);
        var outputChainBlockPosition = new BlockPos(3, 2, 3);
        var leverPosition = new BlockPos(0, 2, 1);
        var redstoneWirePosition = new BlockPos(4, 2, 3);

        // Verify the structure has the correct blocks
        assertBlockNameAtPosition(helper, "Redstone Chain", inputChainBlockPosition);
        assertBlockNameAtPosition(helper, "Redstone Chain", outputChainBlockPosition);
        assertBlockNameAtPosition(helper, "Lever", leverPosition);
        // Verify that the blocks have the expected initial state
        assertLeverIsPowered(helper, leverPosition, false);
        assertRedstoneWirePowered(helper, redstoneWirePosition, false);

        var connectorItem = new ItemStack(MinecraftPlayground.REDSTONE_CHAIN_CONNECTOR.get());
        // Connect the wires
        helper.runAtTickTime(++tick, () -> {
            // First click: crouch-click the first chain block to save its position
            useItemOn(helper, inputChainBlockPosition, connectorItem);
            useItemOn(helper, outputChainBlockPosition, connectorItem);
            assertChainBlocksAreConnected(helper, inputChainBlockPosition, outputChainBlockPosition);
        });

        // Activate the lever and verify the output
        helper.runAtTickTime(++tick, () -> {
            helper.pullLever(leverPosition);
            assertRedstoneWirePowered(helper, redstoneWirePosition, true);
        });

        // ensure this is run as the last tick
        helper.runAtTickTime(++tick, helper::succeed);
    }

    /**
     * Asserts that two RedstoneChain blocks are connected to each other.
     * Verifies bidirectional connection: A→B and B→A.
     *
     * @param helper The GameTestHelper
     * @param pos1   First chain block position
     * @param pos2   Second chain block position
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
