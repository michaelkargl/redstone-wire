package tests;

import net.minecraft.gametest.framework.GameTestHelper;

import static tests.TestHelpers.assertBlockNameAtPosition;
import static tests.TestHelpers.validate2DXZGrid;

public class StructureTests {

    // NOTE: this test's NBT (structuretests.teststructurecomposition) is also used as the
    // sandbox for RedstoneWireBlockEntityTests. Changing its size or its (0,0,0)/(1,0,0)
    // cells affects that test too.

    // Registered in RedstoneWire.TEST_FUNCTIONS and described by a test_instance JSON file.
    public static void testStructureComposition(GameTestHelper helper) {
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

        // Since 1.21.5, GameTest coordinates start at the first block in the template.
        // The test-instance controller sits outside this coordinate space.
        assertBlockNameAtPosition(helper, "Blue Wool", 0, 0, 0); // template origin
        assertBlockNameAtPosition(helper, "Air", 0, 2, 0);       // two blocks above origin

        String[][] expectedGrid = {
                {"Blue Wool", "Blue Concrete", "Light Blue Wool", "Purple Terracotta", "Magenta Terracotta"},   // z=0
                {"Light Blue Concrete", "Cyan Concrete", "Cyan Wool", "Brown Wool", "Red Terracotta"},          // z=1
                {"Black Wool", "Gray Wool", "Purple Wool", "Light Gray Wool", "White Wool"},                    // z=2
                {"Yellow Concrete", "Orange Wool", "Magenta Wool", "Lime Wool", "Lime Concrete"},               // z=3
                {"Yellow Wool", "Orange Concrete", "Pink Wool", "Green Terracotta", "Green Wool"}               // z=4
        };

        new SpecFlow(helper)
                .given("The structure is set up correctly", () -> validate2DXZGrid(helper, expectedGrid, 0, 0, 0))
                .then("Test succeeds", helper::succeed);
    }
}
