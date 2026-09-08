package at.osa.redstonewire;

import at.osa.redstonewire.init.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import tests.RedstoneInputBlockTests;
import tests.RedstoneOutputBlockTests;
import tests.RedstoneWireBlockEntityTests;
import tests.StructureTests;

import java.util.function.Consumer;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

/**
 * The @Mod([modId]) should match an entry in the META-INF/neoforge.mods.toml file.
 */
@Mod(RedstoneWire.MODID)
public class RedstoneWire {
    public static final String MODID = "redstone_wire";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Since Minecraft 1.21.5, GameTest methods are registry entries instead of
     * annotation-scanned methods. The matching test-instance JSON files select
     * the structure, timeout, environment, and one of these functions.
     */
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, MODID);

    static {
        TEST_FUNCTIONS.register(
                "redstoneinputblocktests.comparatorcandetectinputblockpowerlevel",
                () -> RedstoneInputBlockTests::comparatorCanDetectInputBlockPowerLevel);
        TEST_FUNCTIONS.register(
                "redstoneoutputblocktests.testoutputblocktransmitspowerlevelfromwirenetwork",
                () -> RedstoneOutputBlockTests::testOutputBlockTransmitsPowerLevelFromWireNetwork);
        TEST_FUNCTIONS.register(
                "structuretests.teststructurecomposition",
                () -> StructureTests::testStructureComposition);
        TEST_FUNCTIONS.register(
                "redstonewireblockentitytests.breakingblockremovesreverseconnection",
                () -> RedstoneWireBlockEntityTests::breakingBlockRemovesReverseConnection);
    }

    public RedstoneWire(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        TEST_FUNCTIONS.register(modEventBus);

        //modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

}
