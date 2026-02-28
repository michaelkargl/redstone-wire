package tests;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("redstone_wire")
public class RedstoneOutputBlockTests {

    @GameTest()
    public static void testOutputBlockTransmitsPowerLevelFromWireNetwork(GameTestHelper helper) {
        var lowPowerLeverPosition = new BlockPos(1, 2, 4);
        var highPowerLeverPosition = new BlockPos(1, 2, 1);
        var outputRedstoneSignalPosition = new BlockPos(4, 2, 1);

        new SpecFlow(helper)
                .given("A high power lever in off position", () -> TestHelpers.assertLeverIsOff(helper, highPowerLeverPosition))
                .and("A low power lever in off position", () -> TestHelpers.assertLeverIsOff(helper, lowPowerLeverPosition))
                .and("An output redstone signal of power 0", () -> TestHelpers.assertRedstoneWire(helper, outputRedstoneSignalPosition, 0))
                .when("Toggling the low power lever", () -> TestHelpers.pullLever(helper, lowPowerLeverPosition))
                .then("The output signal is low", () -> TestHelpers.assertRedstoneWire(helper, outputRedstoneSignalPosition, 7), 10)
                .when("Toggling the high power lever", () -> TestHelpers.pullLever(helper, highPowerLeverPosition))
                .then("The output signal is high", () -> TestHelpers.assertRedstoneWire(helper, outputRedstoneSignalPosition, 14), 20)
                .then("Test succeeds", helper::succeed);
    }
}
