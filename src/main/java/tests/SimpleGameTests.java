package tests;

import at.osa.minecraftplayground.MinecraftPlayground;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.LeverBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;

import static tests.TestHelpers.*;


// structure name is <classname>.<testname>.nbt (all lowercase)
// create a structure by
// 1. creating a @GameTest annotated test function
// 2. running the dev server
// 3. connecting via dev client
// 4. run /test create <classname>.<testname>
@GameTestHolder("minecraftplayground")
public class SimpleGameTests {

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

        new SpecFlow(helper)
                .given("The structure is set up correctly", () -> validate2DXZGrid(helper, expectedGrid, 0, 0, 1))
                .then("Test succeeds", helper::succeed);
    }

    @GameTest
    public static void leverActionTest(GameTestHelper helper) {
        var redstoneLampPosition = new BlockPos(2, 2, 2);
        var leverBlockPosition = new BlockPos(2, 3, 2);
        assertBlockNameAtPosition(helper, "Redstone Lamp", redstoneLampPosition); // 9
        assertBlockNameAtPosition(helper, "Lever", leverBlockPosition); // 10

        var leverBlockState = helper.getBlockState(leverBlockPosition);
        if (!(leverBlockState.getBlock() instanceof LeverBlock leverBlock)) {
            helper.fail("Block at lever position is not a lever");
        }

        new SpecFlow(helper)
                .given("The lever is in its default state", () -> assertLeverIsPowered(helper, leverBlockPosition, false))
                .when("I pull the lever", () -> pullLever(helper, leverBlockPosition))
                .then("The lever should be powered", () -> assertLeverIsPowered(helper, leverBlockPosition, true))
                .and("The redstone lamp should be lit", () -> assertRedstoneLampIsLit(helper, redstoneLampPosition))
                .and("Test succeeds", helper::succeed);
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
        var inputChainBlockPosition = new BlockPos(1, 2, 1);
        var outputChainBlockPosition = new BlockPos(3, 2, 3);
        var leverPosition = new BlockPos(0, 2, 1);
        var redstoneWirePosition = new BlockPos(4, 2, 3);
        var connectorItem = new ItemStack(MinecraftPlayground.REDSTONE_CHAIN_CONNECTOR.get());

        new SpecFlow(helper)
                .given("The structure is set up correctly", () -> {
                    assertBlockNameAtPosition(helper, "Redstone Chain", inputChainBlockPosition);
                    assertBlockNameAtPosition(helper, "Redstone Chain", outputChainBlockPosition);
                    assertBlockNameAtPosition(helper, "Lever", leverPosition);
                    assertLeverIsPowered(helper, leverPosition, false);
                    assertRedstoneWirePowered(helper, redstoneWirePosition, false);
                })
                .when("I connect the two Redstone Chain blocks", () -> {
                    useItemOn(helper, inputChainBlockPosition, connectorItem);
                    useItemOn(helper, outputChainBlockPosition, connectorItem);
                })
                .then("They should be connected bidirectionally", () -> {
                    assertChainBlocksAreConnected(helper, inputChainBlockPosition, outputChainBlockPosition);
                })
                .when("I pull the lever", () -> {
                    helper.pullLever(leverPosition);
                })
                .then("The redstone wire should be powered", () -> {
                    assertRedstoneWirePowered(helper, redstoneWirePosition, true);
                })
                .and("Test succeeds", helper::succeed);
    }

}
