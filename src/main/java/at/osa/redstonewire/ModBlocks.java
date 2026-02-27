package at.osa.redstonewire;

import at.osa.redstonewire.connector.RedstoneConnectorBlock;
import at.osa.redstonewire.input.RedstoneInputBlock;
import at.osa.redstonewire.output.RedstoneOutputBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RedstoneWire.MODID);

    public static final DeferredBlock<RedstoneConnectorBlock> REDSTONE_CONNECTOR_BLOCK = BLOCKS.register(
            "redstone_connector",
            () -> new RedstoneConnectorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion()));

    public static final DeferredBlock<RedstoneInputBlock> REDSTONE_INPUT_BLOCK = BLOCKS.register(
            "redstone_input",
            () -> new RedstoneInputBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));

    public static final DeferredBlock<RedstoneOutputBlock> REDSTONE_OUTPUT_BLOCK = BLOCKS.register(
            "redstone_output",
            () -> new RedstoneOutputBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
}
