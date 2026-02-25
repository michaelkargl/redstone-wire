# Redstone Wire Mod - Memory

## Project Structure
- NeoForge Minecraft mod (Java), assets in `src/main/resources/assets/redstone_wire/`
- Textures are NOT in `src/main/resources` — they live in `run/resourcepacks/RedstoneWire/` (a dev resource pack that must be enabled in-game)
- Build output goes to `build/resources/main/` — the game reads from here at runtime

## Block Model Rules (hard-won lessons)
- Custom element models MUST have `"parent": "block/block"` — without it Minecraft rejects the model entirely and shows the error cube
- `__comment` fields are safe inside element objects `{}` but MUST NOT be placed inside `"faces": {}` — Minecraft's face deserializer tries to parse every key as a Direction enum and throws a JsonParseException on unknown keys, which surfaces as a FileNotFoundException/model load failure
- Non-full-cube blocks MUST have `.noOcclusion()` in their BlockBehaviour.Properties, otherwise adjacent block faces get incorrectly culled

## Block Architecture
- `RedstoneConnectorBlock` — the main connector block with antenna voxel shape (slab + base ring + shaft), FACING property, `noOcclusion()`
- `CableRenderer.attachY()` returns `11.0/16.0` for `RedstoneConnectorBlock` (tip of antenna shaft) vs `1.0` for full-cube blocks
- `RedstoneRelayBlock` was an experiment that was merged into `RedstoneConnectorBlock` and deleted

## Debugging Tips
- Check `run/logs/latest.log` for model loading errors — `FileNotFoundException` on a model path usually means JSON deserialization failed, not that the file is missing
