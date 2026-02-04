# Quick Reference: External Resource Pack Setup

## 1️⃣ Create Folder Structure

**Location:** `.minecraft/resourcepacks/`

```
RedstoneChainTextures/
├── pack.mcmeta
└── assets/
    └── minecraftplayground/
        └── textures/
            └── block/
                ├── redstone_chain.png
                └── redstone_chain_powered.png
```

---

## 2️⃣ Create pack.mcmeta

Save this as `pack.mcmeta` in the `RedstoneChainTextures` folder:

```json
{
  "pack": {
    "pack_format": 34,
    "description": "Custom textures for RedstoneChain blocks"
  }
}
```

---

## 3️⃣ Add Texture Files

Place your 16x16 PNG files in:
```
RedstoneChainTextures/assets/minecraftplayground/textures/block/
```

**Files needed:**
- `redstone_chain.png` - Unpowered state (gray/metallic)
- `redstone_chain_powered.png` - Powered state (red/glowing)

---

## 4️⃣ Enable in Minecraft

1. Launch Minecraft
2. **Options → Resource Packs**
3. Find "RedstoneChainTextures"
4. Click arrow to move to **Selected**
5. Click **Done**

---

## 5️⃣ Test

1. Place a RedstoneChainBlock
2. Should show your custom unpowered texture
3. Apply redstone power
4. Should switch to your powered texture

---

## Quick Commands

**Reload textures in-game:**
Press `F3 + T`

**Resourcepacks location:**
- macOS: `~/Library/Application Support/minecraft/resourcepacks/`
- Windows: `%APPDATA%\.minecraft\resourcepacks\`
- Linux: `~/.minecraft/resourcepacks/`

---

## Troubleshooting

**Missing texture (purple/black):**
- Check PNG files are 16x16 pixels
- Verify file names match exactly
- Ensure folder structure is correct

**Pack not showing:**
- Check pack.mcmeta is valid JSON
- Verify pack_format: 34 for MC 1.21.x

**Reload not working:**
- Disable and re-enable the pack
- Restart Minecraft

---

## What's Already Done ✅

- ✅ Model files updated to reference custom textures
- ✅ Textures directory created in mod
- ✅ Mod rebuilt and ready
- ✅ Documentation created

## What You Need to Do ⏳

- ⏳ Create the resource pack folder
- ⏳ Add pack.mcmeta file
- ⏳ Create/add your texture PNGs
- ⏳ Enable pack in Minecraft
- ⏳ Test!

That's it! 🎨
