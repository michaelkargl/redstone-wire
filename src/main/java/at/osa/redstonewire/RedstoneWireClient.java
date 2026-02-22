package at.osa.redstonewire;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = RedstoneWire.MODID, dist = Dist.CLIENT)
public class RedstoneWireClient {

    public RedstoneWireClient(IEventBus modEventBus, ModContainer container) {
        // Register mod bus events manually
        modEventBus.addListener(RedstoneWireClient::onClientSetup);
        modEventBus.addListener(RedstoneWireClient::registerRenderers);

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        RedstoneWire.LOGGER.info("HELLO FROM CLIENT SETUP");
        RedstoneWire.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register the block entity renderer for redstone chains
        event.registerBlockEntityRenderer(RedstoneWire.REDSTONE_CONNECTOR_ENTITY.get(), RedstoneConnectorBlockEntityRenderer::new);
    }
}
