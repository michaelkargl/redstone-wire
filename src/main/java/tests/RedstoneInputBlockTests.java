package tests;


import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;


@GameTestHolder("redstone_wire")
public class RedstoneInputBlockTests {
    @GameTest
    public static void comparatorCanDetectInputBlockPowerLevel(GameTestHelper helper) {
        var lowPowerLeverPosition = new BlockPos(8, 2, 1);
        var highPowerLeverPosition = new BlockPos(8, 2, 2);
        var lowPowerLampPosition = new BlockPos(2, 2, 0);
        var highPowerLampPosition = new BlockPos(4, 2, 1);

        new SpecFlow(helper)
                .given("A high power lever is placed in off position", () -> TestHelpers.assertLeverIsPowered(helper, highPowerLeverPosition, false))
                .and("A low power lever is placed in off position", () -> TestHelpers.assertLeverIsPowered(helper, lowPowerLeverPosition, false))
                .and("A high power redstone lamp in off position", () -> TestHelpers.assertRedstoneLampIsLit(helper, highPowerLampPosition, false))
                .and("A low power redstone lamp in off position", () -> TestHelpers.assertRedstoneLampIsLit(helper, lowPowerLampPosition, false))
                .when("Toggling the low power lever", () -> TestHelpers.pullLever(helper, lowPowerLeverPosition))
                .then("The low power lamp is lit", () -> TestHelpers.assertRedstoneLampIsLit(helper, lowPowerLampPosition, true), 10)
                .and("The high power lamp is unlit", () -> TestHelpers.assertRedstoneLampIsLit(helper, highPowerLampPosition, false))
                .when("The high power lever is toggled on", () -> TestHelpers.pullLever(helper, highPowerLeverPosition))
                .then("The high power redstone lamp is lit", () -> TestHelpers.assertRedstoneLampIsLit(helper, highPowerLampPosition, true), 20)
                .and("Test succeeds", helper::succeed);
    }
}
