package at.osa.minecraftplayground;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // === Redstone Chain Network Category ===
    static {
        BUILDER.push("redstoneChain");
    }

    public static final ModConfigSpec.IntValue MAX_CONNECTION_DISTANCE = BUILDER
            .comment("Maximum distance (in blocks) between two connected chain blocks. Connections beyond this distance are rejected.")
            .defineInRange("maxConnectionDistance", 24, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_CONNECTIONS_PER_CHAIN = BUILDER
            .comment("Maximum number of connections allowed per chain block. Prevents visual clutter and performance issues.")
            .defineInRange("maxConnectionsPerChain", 5, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue UPDATE_INTERVAL_TICKS = BUILDER
            .comment("How often to perform periodic network updates (in ticks). 20 ticks = 1 second. This acts as a backup to event-driven updates.")
            .defineInRange("updateIntervalTicks", 20, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SIGNAL_LOSS_DELAY_TICKS = BUILDER
            .comment("How many ticks to wait before clearing cached signal after input is lost. Prevents flickering when power briefly turns off.")
            .defineInRange("signalLossDelayTicks", 1, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    // === Cable Rendering Category ===
    static {
        BUILDER.push("cableRendering");
    }

    public static final ModConfigSpec.IntValue CABLE_SEGMENTS = BUILDER
            .comment("Number of segments to divide cable into (more = smoother curve)")
            .defineInRange("cableSegments", 8, 1, 100);

    public static final ModConfigSpec.DoubleValue CABLE_THICKNESS_IN_BLOCKS = BUILDER
            .comment("Thickness of cable in blocks (0.03 = ~2 pixels)")
            .defineInRange("cableThicknessPixels", 0.03, 0, 0.1);

    public static final ModConfigSpec.DoubleValue CABLE_SAG_AMOUNT = BUILDER
            .comment("Amount of sag at the middle of the cable (0.0 = no sag, -1.0 = full sag)")
            .defineInRange("cableSagAmount", -1.0, -1.0, 0);

    public static final ModConfigSpec.IntValue MAX_RENDER_DISTANCE = BUILDER
            .comment("Maximum distance to render cables (in blocks)")
            .defineInRange("maxRenderDistance", 128, 1, 512);

    static {
        BUILDER.pop();
    }

    // === Cable Colors Category ===
    static {
        BUILDER.push("cableColors");
    }

    public static final ModConfigSpec.DoubleValue UNPOWERED_RED = BUILDER
            .comment("Red component for unpowered cables (0.0 = no red, 1.0 = full red)")
            .defineInRange("unpoweredRed", 0.3, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue POWERED_RED_BASE = BUILDER
            .comment("Base red component for powered cables (0.0 = no red, 1.0 = full red)")
            .defineInRange("poweredRedBase", 0.6, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue POWERED_RED_BONUS = BUILDER
            .comment("Additional red component for powered cables based on power level (0.0 = no extra red, 1.0 = full extra red)")
            .defineInRange("poweredRedBonus", 0.3, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue GREEN_VALUE = BUILDER
            .comment("Green component for cables (0.0 = no green, 1.0 = full green)")
            .defineInRange("greenValue", 0.0, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BLUE_VALUE = BUILDER
            .comment("Blue component for cables (0.0 = no blue, 1.0 = full blue)")
            .defineInRange("blueValue", 0.0, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    // === Utility & Debug Category ===
    static {
        BUILDER.push("utility");
    }

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "minecraft:iron_ingot", Config::validateItemName);

    static {
        BUILDER.pop();
    }


    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
