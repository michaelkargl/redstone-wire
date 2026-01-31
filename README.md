![Redstone Wire! Redstone in, Redstone out! Easy!](images/redstone-wire-text.png)


Project Management
=======

> To report issues, please use the "Issues" tab above

This project uses [Backlog.md] for managing the project development.

```pwsh
backlog board view
backlog browser
```

[Backlog.md]: https://backlog.md

Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Configuration
=============

This mod uses NeoForge's configuration system to provide runtime customization. After running Minecraft with this mod for the first time, a configuration file will be automatically generated.

**Configuration File Location:**
`config/minecraftplayground-common.toml`

### Configuration Sections

#### Redstone Chain Network
Controls the behavior of redstone chain connections:

- `maxConnectionDistance` (default: 24) - Maximum distance in blocks between connected chain blocks
- `maxConnectionsPerChain` (default: 5) - Maximum number of connections per chain block
- `updateIntervalTicks` (default: 20) - Backup periodic network update frequency in ticks
- `signalLossDelayTicks` (default: 1) - Delay before clearing cached signal after power loss

#### Cable Rendering
Customizes the visual appearance of cables:

- `segments` (default: 8) - Number of segments dividing the cable for smoothness (1-100)
- `thicknessInBlocks` (default: 0.03) - Cable thickness in blocks (0.03 ≈ 2 pixels)
- `sagAmount` (default: -1.0) - Cable sag at the middle (0 = no sag, -1 = full sag)
- `maxRenderDistance` (default: 128) - Maximum distance in blocks at which cables render (1-512)

#### Cable Colors
RGB values for cable appearance:

- Unpowered cables: Configurable RGB values (default: dark red)
- Powered cables: Base and bonus red values for power visualization
- Green and blue channels for custom color schemes

#### Utility Settings
- `logDirtBlock` (default: true) - Whether to log the dirt block on startup
- `magicNumber` (default: 42) - Demonstration configuration value
- `magicNumberIntroduction` (default: "The magic number is... ") - Message prefix
- `itemStrings` (default: ["minecraft:iron_ingot"]) - List of items to log on startup

### Editing the Configuration

1. Stop Minecraft if it's running
2. Open `config/minecraftplayground-common.toml` in any text editor
3. Modify values (validation ranges are enforced)
4. Save the file
5. Restart Minecraft for changes to take effect

**Example Configuration:**
```toml
logDirtBlock = true
magicNumber = 42

[redstoneChain]
    maxConnectionDistance = 32
    maxConnectionsPerChain = 10

[cableRendering]
    segments = 12
    thicknessInBlocks = 0.05
    sagAmount = -0.5
```

Additional Resources:
==========
Community Documentation: https://docs.neoforged.net/
NeoForged Discord: https://discord.neoforged.net/
