package at.osa.redstonewire;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RedstoneWire.MODID)
public class RedstoneWire {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "redstone_wire";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "redstone_wire" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "redstone_wire" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "redstone_wire" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "redstone_wire" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    // Create a Deferred Register to hold DataComponentTypes which will all be registered under the "redstone_wire" namespace
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredBlock<RedstoneConnectorBlock> REDSTONE_CONNECTOR_BLOCK = BLOCKS.register(
            "redstone_connector",
            () -> new RedstoneConnectorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final DeferredItem<BlockItem> REDSTONE_CONNECTOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("redstone_connector", REDSTONE_CONNECTOR_BLOCK);

    public static final DeferredBlock<RedstoneInputBlock> REDSTONE_INPUT_BLOCK = BLOCKS.register(
            "redstone_input",
            () -> new RedstoneInputBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "redstone_wire:redstone_input"
    public static final DeferredItem<BlockItem> REDSTONE_INPUT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("redstone_input", REDSTONE_INPUT_BLOCK);

    public static final DeferredBlock<RedstoneOutputBlock> REDSTONE_OUTPUT_BLOCK = BLOCKS.register(
            "redstone_output",
            () -> new RedstoneOutputBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final DeferredItem<BlockItem> REDSTONE_OUTPUT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("redstone_output", REDSTONE_OUTPUT_BLOCK);

    public static final DeferredBlock<RedstoneChainBlock> REDSTONE_CHAIN_BLOCK = BLOCKS.register(
            "redstone_chain",
            () -> new RedstoneChainBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));

    // Creates the block entity type for the Redstone Chain
    public static final Supplier<BlockEntityType<RedstoneChainEntity>> REDSTONE_CHAIN_ENTITY = BLOCK_ENTITY_TYPES.register(
            "redstone_chain_entity",
            () -> BlockEntityType.Builder.of(RedstoneChainEntity::new, REDSTONE_CHAIN_BLOCK.get()).build(null));

    // Data component for storing link data on the connector item
    public static final Supplier<DataComponentType<CompoundTag>> LINK_DATA = DATA_COMPONENT_TYPES.register(
            "link_data",
            () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    // Creates a creative tab with the id "redstone_wire:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> REDSTONE_WIRE_TAB = CREATIVE_MODE_TABS.register("redstone_wire_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.redstone_wire")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> REDSTONE_CONNECTOR_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(REDSTONE_CONNECTOR_BLOCK_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(REDSTONE_INPUT_BLOCK_ITEM.get());
                output.accept(REDSTONE_OUTPUT_BLOCK_ITEM.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public RedstoneWire(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entity types get registered
        BLOCK_ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so data component types get registered
        DATA_COMPONENT_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (RedstoneWire) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(REDSTONE_INPUT_BLOCK_ITEM);
            event.accept(REDSTONE_CONNECTOR_BLOCK_ITEM);
            event.accept(REDSTONE_OUTPUT_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
