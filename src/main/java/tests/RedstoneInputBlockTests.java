package tests;


import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;


public class RedstoneInputBlockTests {
    // Registered in RedstoneWire.TEST_FUNCTIONS and described by a test_instance JSON file.
    public static void comparatorCanDetectInputBlockPowerLevel(GameTestHelper helper) {
        var lowPowerLeverPosition = new BlockPos(8, 1, 1);
        var highPowerLeverPosition = new BlockPos(8, 1, 2);
        var lowPowerLampPosition = new BlockPos(2, 1, 0);
        var highPowerLampPosition = new BlockPos(4, 1, 1);

        new SpecFlow(helper)
                .given("A high power lever is placed in off position", () -> TestHelpers.assertLeverIsOff(helper, highPowerLeverPosition))
                .and("A low power lever is placed in off position", () -> TestHelpers.assertLeverIsOff(helper, lowPowerLeverPosition))
                .and("A high power redstone lamp in off position", () -> TestHelpers.assertRedstoneLampIsLit(helper, highPowerLampPosition, false))
                .and("A low power redstone lamp in off position", () -> TestHelpers.assertRedstoneLampIsLit(helper, lowPowerLampPosition, false))
                .when("Toggling the low power lever", () -> TestHelpers.pullLever(helper, lowPowerLeverPosition))
                .then("The low power lamp is lit", () -> TestHelpers.assertRedstoneLampIsLit(helper, lowPowerLampPosition, true), 10)
                .and("The high power lamp is unlit", () -> TestHelpers.assertRedstoneLampIsLit(helper, highPowerLampPosition, false))
                .when("Toggling the high power lever", () -> TestHelpers.pullLever(helper, highPowerLeverPosition))
                .then("The high power redstone lamp is lit", () -> TestHelpers.assertRedstoneLampIsLit(helper, highPowerLampPosition, true), 20)
                .and("Test succeeds", helper::succeed);
    }
}
