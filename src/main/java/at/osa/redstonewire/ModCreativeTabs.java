package at.osa.redstonewire;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RedstoneWire.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> REDSTONE_WIRE_TAB = CREATIVE_MODE_TABS.register("redstone_wire_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.redstone_wire"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.REDSTONE_CONNECTOR_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Automatically register all items to creative tab
                ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
            }).build());
}
