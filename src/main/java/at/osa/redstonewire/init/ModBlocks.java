package at.osa.redstonewire.init;

import at.osa.redstonewire.RedstoneWire;
import at.osa.redstonewire.connector.RedstoneConnectorBlock;
import at.osa.redstonewire.input.RedstoneInputBlock;
import at.osa.redstonewire.output.RedstoneOutputBlock;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RedstoneWire.MODID);

    public static final DeferredBlock<RedstoneConnectorBlock> REDSTONE_CONNECTOR_BLOCK = BLOCKS.registerBlock(
            "redstone_connector",
            RedstoneConnectorBlock::new,
            properties -> properties.mapColor(MapColor.STONE).noOcclusion());

    public static final DeferredBlock<RedstoneInputBlock> REDSTONE_INPUT_BLOCK = BLOCKS.registerBlock(
            "redstone_input",
            RedstoneInputBlock::new,
            properties -> properties.mapColor(MapColor.STONE));

    public static final DeferredBlock<RedstoneOutputBlock> REDSTONE_OUTPUT_BLOCK = BLOCKS.registerBlock(
            "redstone_output",
            RedstoneOutputBlock::new,
            properties -> properties.mapColor(MapColor.STONE));
}
