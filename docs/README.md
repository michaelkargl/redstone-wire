# Redstone Wire — Documentation

Everything written down about this mod, and a fair bit about NeoForge modding in
general. Written to be read in order if you are new to modding.

**Current target:** Minecraft 1.21.11 · NeoForge 21.11.45 · Java 21

---

## Start here

If you are new to Minecraft modding, read these three in order. They go from
"what even is a block entity" to "here is why this mod is shaped like this".

| | Document | What you get |
| --- | --- | --- |
| 1 | **[NeoForge Concepts](neoforge-concepts.md)** | The platform, from scratch, with C#/F# analogies. Client/server split, mod lifecycle, events, registries, block vs block state vs block entity, NBT and data components, rendering, codecs, packets, tickers, and the gotchas that bite newcomers. Nothing mod-specific. |
| 2 | **[Usage Guide](usage.md)** | What the mod does from a player's seat. Read it before the code — it is much easier to follow the implementation once you have flipped the lever yourself. |
| 3 | **[Architecture](architecture.md)** | How this mod is built: the three blocks, how a signal travels, the output cache, the two-click linking flow, and the registration layout. |

## Reference

Read these when you are working on that particular area.

| Document | Covers |
| --- | --- |
| **[Rendering](rendering.md)** | Block models (and the JSON rules that cost real debugging time), plus the hand-built cable geometry, the render pipeline, and the extract/submit BER split. |
| **[Textures](textures.md)** | Where textures live, how models reference them, and how to override them with a resource pack. |
| **[Testing](testing.md)** | Writing and running GameTests: the function/JSON/NBT trio, the `SpecFlow` helper, and how to debug a failure. |
| **[Deployment](deployment.md)** | Cutting a release. |

## Elsewhere in the repo

| Path | What it is |
| --- | --- |
| [`../README.md`](../README.md) | Project overview and quick start |
| [`../backlog/`](../backlog/README.md) | Task tracking (Backlog.md) |
| [`../openspec/`](../openspec/README.md) | Change proposals and specs (OpenSpec) |
| `upgrade-1.21.11-report.html` | Point-in-time report from the 1.21.11 / NeoForge 21.11.45 upgrade. Historical record, not a living document. |

---

## Useful external references

- [NeoForge Documentation](https://docs.neoforged.net/) — the primary source
- [NeoForge Registries](https://docs.neoforged.net/docs/concepts/registries/)
- [Block Entities](https://docs.neoforged.net/docs/blockentities/)
- [Minecraft Wiki: Model format](https://minecraft.wiki/w/Model) — the JSON schema for block models

---

## A note on version drift

Minecraft's modding APIs move fast, and this mod has already been through several
breaking upgrades. Where an API changed recently, these docs say so inline —
those markers are the most useful thing in here when a tutorial you found online
refuses to compile. The three that matter most right now:

- **1.21.5** — `@GameTest`/`@GameTestHolder` annotations removed; tests became
  registry entries. See [Testing](testing.md).
- **1.21.9** — rendering rewritten: `RenderPipeline` replaced `RenderType.create`,
  and BERs split into extract/submit. See [Rendering](rendering.md).
- **1.21.11** — block entity teardown moved from `Block#onRemove` to
  `preRemoveSideEffects`. See [Architecture](architecture.md).

If you touch code that one of these documents describes, update the document in
the same PR. The alternative is what this folder was cleaning up from.
