Textures
========

There are two ways of providing textures for the new blocks.

1. Mod internally => see `src/main/resources/assets/redstone_wire/textures/blocks`
   - Baked into the mod jar, cannot be changed without rebuilding the mod. However,
     they can be overridden by creating a texture pack.
2. Mod externally
   - Create a texture pack and place the textures in the correct location.
   - ```js
     # resourcepacks/RedstoneWire/pack.mcmeta
     {
       "pack": {
       "pack_format": 34,
       "description": "Redstone Wires"
     }
     ```
   - Copy the internal textures into the texture pack
     ```bash
     cp -r src/main/resources/assets/redstone_wire resourcepacks/RedstoneWire/assets
     ```
