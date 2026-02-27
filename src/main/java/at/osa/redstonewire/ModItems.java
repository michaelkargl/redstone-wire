package at.osa.redstonewire;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RedstoneWire.MODID);

    public static final DeferredItem<BlockItem> REDSTONE_CONNECTOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("redstone_connector", ModBlocks.REDSTONE_CONNECTOR_BLOCK);
    public static final DeferredItem<BlockItem> REDSTONE_INPUT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("redstone_input", ModBlocks.REDSTONE_INPUT_BLOCK);
    public static final DeferredItem<BlockItem> REDSTONE_OUTPUT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("redstone_output", ModBlocks.REDSTONE_OUTPUT_BLOCK);
}
