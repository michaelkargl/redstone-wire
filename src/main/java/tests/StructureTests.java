package tests;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

import static tests.TestHelpers.assertBlockNameAtPosition;
import static tests.TestHelpers.validate2DXZGrid;

@GameTestHolder("redstone_wire")

public class StructureTests {

    @GameTest()
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

        assertBlockNameAtPosition(helper, "Structure Block", 0, 0, 0); // (0,0,0) - structure origin
        assertBlockNameAtPosition(helper, "Air", 0, 2, 0);  // (0,2,0) - two blocks above origin

        String[][] expectedGrid = {
                {"Blue Wool", "Blue Concrete", "Light Blue Wool", "Purple Terracotta", "Magenta Terracotta"},   // z=0
                {"Light Blue Concrete", "Cyan Concrete", "Cyan Wool", "Brown Wool", "Red Terracotta"},          // z=1
                {"Black Wool", "Gray Wool", "Purple Wool", "Light Gray Wool", "White Wool"},                    // z=2
                {"Yellow Concrete", "Orange Wool", "Magenta Wool", "Lime Wool", "Lime Concrete"},               // z=3
                {"Yellow Wool", "Orange Concrete", "Pink Wool", "Green Terracotta", "Green Wool"}               // z=4
        };

        new SpecFlow(helper)
                .given("The structure is set up correctly", () -> validate2DXZGrid(helper, expectedGrid, 0, 0, 1))
                .then("Test succeeds", helper::succeed);
    }
}
