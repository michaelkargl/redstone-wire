# Textures

Textures live **inside the mod jar**, under
`src/main/resources/assets/redstone_wire/textures/block/`, organised one folder
per block:

```
textures/block/
├── connector/   antennae.png  base.png
├── input/       antennae.png  antennae_base.png  antennae_floor.png
│                accumulator.png  accumulator_top.png  eye.png  texture.png
└── output/      antennae.png  antennae_base.png
                 accumulator.png  accumulator_pack.png  texture.png
```

> The namespace is `redstone_wire` with an **underscore**. Resource-location
> namespaces cannot contain hyphens — `redstone-wire:block/...` silently fails to
> resolve and you get the purple-black missing-texture cube.

## How a model references one

Models declare a texture map and then refer to entries by index:

```json
{
  "parent": "block/block",
  "textures": {
    "0": "redstone_wire:block/connector/antennae",
    "1": "redstone_wire:block/connector/base",
    "particle": "redstone_wire:block/connector/antennae"
  },
  "elements": [
    { "faces": { "north": { "uv": [8, 0, 16, 1], "texture": "#1" } } }
  ]
}
```

`particle` is required — it is what Minecraft uses for break and step particles.
Omit it and you get a warning plus untextured particles.

Models authored in Blockbench may set `"texture_size": [32, 32]` (the Input model
does). That tells Minecraft the UV coordinate space is 32×32 rather than the
default 16×16; it does **not** have to match the PNG's pixel size, but the UVs in
the file are interpreted against it.

## Changing a texture

1. Edit or replace the PNG in place.
2. Rebuild: `./gradlew build` (or `reload_gradle.sh`).

The game reads from `build/resources/main/` at runtime, so an edit that hasn't
been through a build will not show up.

---

## Overriding textures with a resource pack

Baked-in textures can be overridden without rebuilding the mod — useful when
iterating on artwork, and the only way for a player to reskin the blocks.

**1. Create the pack** in your instance's `resourcepacks/` folder. For the dev
instance that is `run/resourcepacks/`; for a normal install:

- Windows: `%APPDATA%\.minecraft\resourcepacks\`
- macOS: `~/Library/Application Support/minecraft/resourcepacks/`
- Linux: `~/.minecraft/resourcepacks/`

```
resourcepacks/RedstoneWire/
├── pack.mcmeta
└── assets/redstone_wire/textures/block/...
```

**2. Add `pack.mcmeta`** at the pack root:

```json
{
  "pack": {
    "pack_format": 34,
    "description": "Redstone Wires"
  }
}
```

`pack_format` is tied to the game version and changes often. 34 covers early
1.21.x; if the pack shows as incompatible, check the current value for your exact
Minecraft version rather than assuming.

**3. Seed it from the mod's own textures**, so you start from something that
already works:

```bash
cp -r src/main/resources/assets/redstone_wire resourcepacks/RedstoneWire/assets/
```

**4. Enable it:** Options → Resource Packs → move it to Selected → Done.

**5. Iterate.** `F3 + T` reloads resources without restarting the game.

### Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| Purple/black checkerboard | Path or namespace wrong. Check `redstone_wire`, not `redstone-wire`, and that the folder under `textures/block/` matches the model's reference. |
| Pack not listed | `pack.mcmeta` is invalid JSON, or it is one level too deep — it must sit at the pack root, next to `assets/`. |
| Pack listed as incompatible | Wrong `pack_format` for this Minecraft version. |
| Edits not showing | `F3 + T` didn't take — toggle the pack off and on. For *mod-internal* textures, you need a rebuild, not a reload. |

---

## Housekeeping note

Four PNGs sit directly in `textures/block/` (rather than in a per-block folder)
and are referenced by **no** model:

- `redstone_input_powered.png`
- `redstone_input_unpowered.png`
- `redstone_output_powered.png`
- `redstone_output_unpowered.png`

They are left over from the pre-refactor chain block. The string
`redstone_input_unpowered` does appear in `models/block/redstone_connector.json`,
but as an *element name*, not a texture reference — it does not keep the file
alive. All four ship in the jar for no reason and should be cleaned up
separately.
