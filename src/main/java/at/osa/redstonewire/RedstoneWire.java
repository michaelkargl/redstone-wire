package at.osa.redstonewire;

import at.osa.redstonewire.init.*;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

/**
 * The @Mod([modId]) should match an entry in the META-INF/neoforge.mods.toml file.
 */
@Mod(RedstoneWire.MODID)
public class RedstoneWire {
    public static final String MODID = "redstone_wire";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RedstoneWire(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

}
