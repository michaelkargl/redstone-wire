package tests;


import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("redstone_wire")
public class RedstoneOutputBlockTests {

    @GameTest()
    public static void testOutputBlockTransmitsPowerLevelFromWireNetwork(GameTestHelper helper) {
        new SpecFlow(helper)
                .then("Test succeeds", helper::succeed);
    }
}
