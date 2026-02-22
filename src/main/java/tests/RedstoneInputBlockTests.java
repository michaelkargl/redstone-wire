package tests;


import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;


@GameTestHolder("redstone_wire")
public class RedstoneInputBlockTests {
    @GameTest
    public static void comparatorCanDetectInputBlockPowerLevel(GameTestHelper helper) {
        var leverPosition = new BlockPos(2, 2, 0);
        var redstoneLampPosition = new BlockPos(2, 2, 4);
        new SpecFlow(helper)
                .given("A lever is placed in off position", () -> TestHelpers.assertLeverIsPowered(helper, leverPosition, false))
                .and("A redstone lamp in off position", () -> TestHelpers.assertRedstoneLampIsLit(helper, redstoneLampPosition, false))
                .when("A lever is toggled on", () -> TestHelpers.pullLever(helper, leverPosition))
                .then("The redstone lamp should be lit", () -> TestHelpers.assertRedstoneLampIsLit(helper, redstoneLampPosition, true))
                .and("Test succeeds", helper::succeed);
    }
}
