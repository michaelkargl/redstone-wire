# Custom Textures Implementation Guide

**Date:** February 3, 2026  
**Status:** ✅ Models Updated - Ready for Textures

---

## What I Just Did

✅ **Updated model files** to reference custom textures:

1. **`redstone_chain.json`** - Now references `"minecraftplayground:block/redstone_chain"`
2. **`redstone_chain_powered.json`** - Now references `"minecraftplayground:block/redstone_chain_powered"`
3. **Created textures directory** at: `src/main/resources/assets/minecraftplayground/textures/block/`

---

## What You Need to Do Next

### Option A: Add Textures to Your Mod (Built-in)

Place your 16x16 PNG texture files here:
```
src/main/resources/assets/minecraftplayground/textures/block/
├── redstone_chain.png          ← Unpowered state texture
└── redstone_chain_powered.png  ← Powered state texture
```

Then rebuild:
```bash
./gradlew build
```

### Option B: Use External Resource Pack (Recommended for Testing)

Create this folder structure in your Minecraft resourcepacks folder:

```
.minecraft/resourcepacks/RedstoneChainTextures/
├── pack.mcmeta
└── assets/
    └── minecraftplayground/
        └── textures/
            └── block/
                ├── redstone_chain.png
                └── redstone_chain_powered.png
```

**Location of resourcepacks:**
- **Windows:** `%APPDATA%\.minecraft\resourcepacks\`
- **macOS:** `~/Library/Application Support/minecraft/resourcepacks/`
- **Linux:** `~/.minecraft/resourcepacks/`

---

## Creating pack.mcmeta

Create this file in the root of your resource pack:

```json
{
  "pack": {
    "pack_format": 34,
    "description": "Custom textures for RedstoneChain blocks"
  }
}
```

**Pack format reference:**
- Minecraft 1.21.x: `34`
- Minecraft 1.20.5-1.21.1: `34`
- Minecraft 1.20.3-1.20.4: `22`
- Minecraft 1.20-1.20.2: `15`

---

## Creating Your Textures

### Texture Specifications
- **Format:** PNG
- **Size:** 16x16 pixels (Minecraft standard)
- **Files needed:**
  1. `redstone_chain.png` - Unpowered state
  2. `redstone_chain_powered.png` - Powered state

### Design Ideas

#### Unpowered State (`redstone_chain.png`)
Consider these themes:
- **Metallic:** Dark gray/silver with highlights
- **Circuit Board:** Dark green PCB with traces
- **Industrial:** Weathered iron/steel
- **Modern:** Clean white/gray tech
- **Steampunk:** Copper/bronze tones

#### Powered State (`redstone_chain_powered.png`)
Consider these themes:
- **Classic Redstone:** Bright red with glowing edges
- **Circuit Active:** Lit-up traces/LEDs on PCB
- **Energy:** Blue/cyan electric glow
- **Neon:** Bright colors with bloom effect
- **Molten:** Orange/red hot metal

---

## Tools for Creating Textures

### Image Editors
- **GIMP** (free, cross-platform) - https://www.gimp.org/
- **Paint.NET** (free, Windows) - https://www.getpaint.net/
- **Aseprite** (paid, pixel art focused) - https://www.aseprite.org/
- **Photoshop** (paid, professional)

### Online Tools
- **Nova Skin Editor** - https://minecraft.novaskin.me/
- **Blockbench** - https://www.blockbench.net/ (3D + textures)
- **Piskel** - https://www.piskelapp.com/ (pixel art)

### Starting from Vanilla
1. Extract Minecraft textures from the JAR:
   - `assets/minecraft/textures/block/iron_block.png`
   - `assets/minecraft/textures/block/redstone_block.png`
2. Edit in your preferred tool
3. Save with new names

---

## Quick Start: Simple Placeholder Textures

If you just want something working quickly:

### Unpowered Texture (Gray Square)
1. Create 16x16 PNG
2. Fill with gray (#7F7F7F)
3. Add some noise/detail
4. Save as `redstone_chain.png`

### Powered Texture (Red Square)
1. Create 16x16 PNG
2. Fill with red (#FF0000)
3. Add brightness/glow
4. Save as `redstone_chain_powered.png`

---

## Testing Your Textures

### With Resource Pack:
1. Place texture PNGs in resource pack
2. Launch Minecraft
3. **Options → Resource Packs**
4. Enable "RedstoneChainTextures"
5. Click **Done** (auto-reloads)
6. Place blocks and test

### With Built-in Textures:
1. Place texture PNGs in mod's textures folder
2. Rebuild: `./gradlew build`
3. Launch Minecraft
4. Place blocks and test

### Quick Reload
Press **F3 + T** in-game to reload textures without restarting!

---

## File Reference

### What Was Changed

#### Before:
```json
// redstone_chain.json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "minecraft:block/iron_block"  ← Vanilla texture
  }
}

// redstone_chain_powered.json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "minecraft:block/redstone_block"  ← Vanilla texture
  }
}
```

#### After:
```json
// redstone_chain.json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "minecraftplayground:block/redstone_chain"  ← Custom texture
  }
}

// redstone_chain_powered.json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "minecraftplayground:block/redstone_chain_powered"  ← Custom texture
  }
}
```

---

## Troubleshooting

### Purple/Black Checkerboard (Missing Texture)
**Cause:** Texture file not found

**Solutions:**
- Check PNG files exist in correct location
- Verify file names match exactly (case-sensitive!)
- Ensure PNGs are exactly 16x16 pixels
- Rebuild mod if using built-in textures
- Reload resource pack if using external

### Still Shows Vanilla Textures
**Cause:** Old models being used

**Solutions:**
- Verify you rebuilt the mod after changing JSON files
- Clear build cache: `./gradlew clean build`
- Check resource pack is enabled and at top of pack list
- Press F3 + T to reload textures

### Resource Pack Not Appearing
**Cause:** Invalid pack.mcmeta

**Solutions:**
- Verify pack.mcmeta is valid JSON
- Check pack_format matches Minecraft version
- Ensure pack folder is in correct location
- Check folder name doesn't have special characters

---

## Recommended Workflow

1. ✅ **Models updated** (already done!)
2. **Create resource pack** with placeholder textures
3. **Enable in Minecraft** and verify it works (you'll see missing texture)
4. **Design textures** in your image editor
5. **Save to resource pack** and reload (F3 + T)
6. **Iterate** until you're happy with the design
7. **Copy final textures** to mod's textures folder
8. **Rebuild mod** with textures included
9. **Test final build** without resource pack

---

## Example Resource Pack Structure

```
RedstoneChainTextures/
├── pack.mcmeta
│   {
│     "pack": {
│       "pack_format": 34,
│       "description": "Custom RedstoneChain textures"
│     }
│   }
│
└── assets/
    └── minecraftplayground/
        └── textures/
            └── block/
                ├── redstone_chain.png (16x16)
                └── redstone_chain_powered.png (16x16)
```

---

## Next Steps

Choose your path:

### Path 1: Quick Test (Resource Pack)
1. Create resource pack folder structure
2. Add pack.mcmeta
3. Create simple placeholder textures
4. Enable in Minecraft
5. Verify blocks use custom textures

### Path 2: Final Integration (Built-in)
1. Create your final textures
2. Place in `src/main/resources/assets/minecraftplayground/textures/block/`
3. Rebuild mod
4. Test in-game

---

## Summary

✅ **Model files updated** - They now reference custom textures  
✅ **Textures directory created** - Ready for PNG files  
⏳ **Waiting for textures** - Add your 16x16 PNGs  
⏳ **Test in-game** - Verify everything works  

**Current status:** The mod is configured to use custom textures. As soon as you add the PNG files (either in the mod or via resource pack), they will display on your blocks!

---

## Quick Command Reference

```bash
# Rebuild mod
./gradlew build

# Clean and rebuild
./gradlew clean build

# Run client for testing
./gradlew runClient
```

**In-game hotkeys:**
- **F3 + T** - Reload textures/resource packs
- **F3 + S** - Reload sounds
- **F3 + F4** - Switch game modes (creative/survival)

---

Good luck with your textures! 🎨
