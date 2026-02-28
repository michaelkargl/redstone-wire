package at.osa.redstonewire.init;

import at.osa.redstonewire.RedstoneWire;
import at.osa.redstonewire.connector.RedstoneConnectorBlockEntity;
import at.osa.redstonewire.input.RedstoneInputBlockEntity;
import at.osa.redstonewire.output.RedstoneOutputBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntityTypes {
    private ModBlockEntityTypes() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RedstoneWire.MODID);

    public static final Supplier<BlockEntityType<RedstoneConnectorBlockEntity>> REDSTONE_CONNECTOR_ENTITY = BLOCK_ENTITY_TYPES.register(
            "redstone_connector_entity",
            () -> BlockEntityType.Builder.of(RedstoneConnectorBlockEntity::new, ModBlocks.REDSTONE_CONNECTOR_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<RedstoneInputBlockEntity>> REDSTONE_INPUT_ENTITY = BLOCK_ENTITY_TYPES.register(
            "redstone_input_entity",
            () -> BlockEntityType.Builder.of(RedstoneInputBlockEntity::new, ModBlocks.REDSTONE_INPUT_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<RedstoneOutputBlockEntity>> REDSTONE_OUTPUT_ENTITY = BLOCK_ENTITY_TYPES.register(
            "redstone_output_entity",
            () -> BlockEntityType.Builder.of(RedstoneOutputBlockEntity::new, ModBlocks.REDSTONE_OUTPUT_BLOCK.get()).build(null));
}
