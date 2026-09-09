# Architecture

How Redstone Wire is actually built. Read [NeoForge Concepts](neoforge-concepts.md)
first if terms like *block entity*, *block state* or *DeferredRegister* are new —
this document assumes them.

- **Mod id:** `redstone_wire` (underscore — it is a namespace, hyphens are illegal)
- **Root package:** `at.osa.redstonewire`
- **Target:** Minecraft 1.21.11 / NeoForge 21.11.45, Java 21

---

## The idea in one paragraph

Vanilla redstone dust loses one power level per block and has to lie on the
ground. This mod adds three blocks that move a redstone signal through the air
over an arbitrary distance with **no signal loss**: you put a signal *in* at an
Input block, it travels along a graph of Connector blocks, and it comes back
*out* at every Output block on that graph.

---

## The three blocks

| Block | Registry name | Role | Extra block state |
| --- | --- | --- | --- |
| Input | `redstone_wire:redstone_input` | Reads vanilla redstone next to it, pushes into the network | `POWER` (0–15) |
| Connector | `redstone_wire:redstone_connector` | Relay node — carries the signal, stores no power | — |
| Output | `redstone_wire:redstone_output` | Emits vanilla redstone at the far end | `POWER` (0–15) |

All three extend `RedstoneWireBlock`, which centralises the parts they share:

- the antenna `VoxelShape` (slab + base ring + shaft),
- the `FACING` horizontal-direction property,
- the helpers for the two-click linking flow.

Their block entities all extend `RedstoneWireBlockEntity`, which owns the
connection list and its persistence.

> **Why a shared base rather than one configurable block?** The three roles have
> genuinely different redstone behaviour — see the `isSignalSource` table below.
> Encoding that as a block-state enum on one block would mean a switch statement
> in every override.

### Redstone behaviour, precisely

This is the part that is easy to get wrong, so it is worth stating explicitly:

| | `isSignalSource` | `getSignal` | Analog output |
| --- | --- | --- | --- |
| **Input** | `false` | `0` | **yes** — returns `POWER`, so a comparator can read it |
| **Connector** | inherited default (`false`) | inherited default | no |
| **Output** | `POWER > 0` | `POWER` | no |

The Input block deliberately emits **nothing**. If it re-emitted the signal it
just consumed, it would power its own source and you would have a feedback loop.
It exposes its level through `getAnalogOutputSignal` instead, which is
comparator-readable but does not power anything.

---

## How a signal travels

```
 [lever]                                                        [lamp]
    │                                                              ▲
    │ vanilla redstone                            vanilla redstone │
    ▼                                                              │
┌─────────┐   cable    ┌───────────┐   cable   ┌───────────┐   ┌────────┐
│  INPUT  │───────────▶│ CONNECTOR │──────────▶│ CONNECTOR │──▶│ OUTPUT │
└─────────┘            └───────────┘           └───────────┘   └────────┘
     │                       │                                      ▲
     │ 1. neighborChanged    │ 2. propagateSignal                   │
     │    reads power        │    walks the cached                  │
     │    sets own POWER     │    output set                    3. setBlock(POWER)
     └───────────────────────┴──────────────────────────────────────┘
```

**Step 1 — `RedstoneInputBlock.neighborChanged`.** Server side only. Reads
`level.getBestNeighborSignal(pos)`. If the level is unchanged it returns
immediately — this short-circuit is what stops redstone update storms from
turning into network walks. Otherwise it writes its own `POWER` state with
`Block.UPDATE_ALL` and calls `propagateSignal` on every linked connector.

**Step 2 — `RedstoneConnectorBlockEntity.propagateSignal`.** The hot path; a
clock signal can call this 20×/second. It does *not* walk the graph. It reads a
cached set of reachable Output positions and writes each one.

**Step 3.** Each Output block gets `setBlock(..., POWER, Block.UPDATE_ALL)`,
which makes vanilla re-evaluate its neighbours — lamps light, dust powers,
pistons fire.

Note there is **no distance limit and no attenuation**. Power 7 in is power 7 out,
whether the connectors are 3 blocks apart or 300.

### The output cache

Walking the connector graph on every signal change would be O(network) at 20 Hz.
Instead `RedstoneConnectorBlockEntity` keeps `reachableOutputs` plus a
`cacheDirty` flag:

- **Invalidated** by topology changes only. `connectionAdded` / `connectionRemoved`
  call `markNetworkDirty`, which floods the dirty flag across the whole connected
  component (with a `visited` set for loop protection) — every connector in the
  network is marked, not just the one that changed.
- **Rebuilt lazily** on the next `propagateSignal`, by a DFS
  (`rebuildOutputCache`) that follows `directConnections`, recursing through
  connectors and collecting Output positions.
- **Self-healing.** If `propagateSignal` finds a cached position that is no
  longer a `RedstoneOutputBlock` (someone mined it, or a piston moved it), it
  marks the cache dirty and skips that entry rather than crashing.

So the cost is O(network) once per topology edit, and O(outputs) per signal —
which is the right trade for a graph that is rewired rarely and pulsed often.

---

## Linking: the two-click flow

There is no custom connector item. You link blocks with **plain vanilla redstone
dust**, in two clicks:

1. Right-click a **Connector** with redstone → its `BlockPos` is written onto the
   held item stack as a `CONNECTOR_LINK_DATA` data component (a `CompoundTag`
   with `x`/`y`/`z`).
2. Right-click a second block (Connector, Input, or Output) with the same stack →
   the saved position is read back, the component is cleared, and a
   **bidirectional** link is created.

Bidirectional matters: `createBidirectionalConnection` adds the position to *both*
block entities' lists. That is what lets either end render the cable and either
end clean up when broken.

> **Why a data component and not a field on the item?** The stack has to survive
> being dropped, put in a chest, and synced to the client. Data components are the
> 1.20.5+ replacement for item NBT and get persistence plus network sync for free —
> see `ModDataComponents`, which declares both `persistent(CompoundTag.CODEC)` and
> `networkSynchronized(...)`.

### Teardown

`RedstoneWireBlockEntity.preRemoveSideEffects` runs just before the block entity
is destroyed and calls `removeBidirectionalConnections`, which walks this block's
links and removes the *reverse* entry from each partner.

> **Changed in 1.21.11.** This used to live in `Block#onRemove`. If you are
> following an older tutorial and your connections leave dangling stubs on the
> partner block, this is why.

---

## Registration

Registration is split by concern under `at.osa.redstonewire.init`, all wired up in
the `RedstoneWire` constructor:

| Class | Registers |
| --- | --- |
| `ModBlocks` | the three blocks. Connector alone gets `.noOcclusion()` |
| `ModItems` | one `BlockItem` per block, via `registerSimpleBlockItem` |
| `ModBlockEntityTypes` | one block entity type per block |
| `ModDataComponents` | `connector_link_data` |
| `ModCreativeTabs` | the "Redstone Wire" tab — auto-adds every mod item, plus vanilla redstone |

`RedstoneWire.TEST_FUNCTIONS` registers GameTest functions; see [Testing](testing.md).

Client-only setup lives in `RedstoneWireClient`, annotated
`@Mod(value = MODID, dist = Dist.CLIENT)` so it never loads on a dedicated
server. It registers the three block entity renderers and the cable render
pipeline.

### Two gotchas worth internalising

**Block state property order is load-bearing.** `StateDefinition.Builder` assigns
numeric IDs by insertion order. Subclasses must call
`super.createBlockStateDefinition(builder)` *before* adding their own properties —
reordering after a world has been saved corrupts placed blocks. Both
`RedstoneInputBlock` and `RedstoneOutputBlock` carry a comment saying so.

**`noOcclusion()` is not optional for non-full-cube blocks.** Without it,
Minecraft assumes the block fills its cube and culls the faces of adjacent
blocks, leaving holes in the world. The Connector has it. See
[Rendering](rendering.md) for the rest of the model rules.

---

## Class map

```
at.osa.redstonewire
├── RedstoneWire                     @Mod entry point, GameTest registry
├── RedstoneWireClient               @Mod(dist = CLIENT) — renderers, pipeline
├── RedstoneWireBlock                abstract: FACING, VoxelShape, link helpers
├── RedstoneWireBlockEntity          abstract: connections, persistence, teardown
├── init/
│   ├── ModBlocks  ModItems  ModBlockEntityTypes
│   └── ModDataComponents  ModCreativeTabs
├── input/     RedstoneInputBlock      + BlockEntity + BlockEntityRenderer
├── output/    RedstoneOutputBlock     + BlockEntity + BlockEntityRenderer
├── connector/ RedstoneConnectorBlock  + BlockEntity + BlockEntityRenderer
└── renderer/  CableRenderer           shared cable geometry + pipeline
```

`tests/` sits alongside in `src/main/java` (not `src/test/java`) because GameTest
functions have to be registered by the mod at runtime.

---

## Known rough edges

Honest list of things that are unfinished rather than designed:

- **No connection validation.** `RedstoneConnectorBlock.handleSecondClick` carries
  a `TODO` — there is no max distance, no max connections per node, and nothing
  stops you linking a block to itself.
- **Cable colour is hardcoded.** `CableRenderer` captures `power` into the render
  state and then ignores it; every cable draws dark red. See [Rendering](rendering.md).
- **The config system is not wired up.** `registerConfig` is commented out in
  `RedstoneWire`. No config file is generated, despite the config screen factory
  being registered and `en_us.json` still carrying ~40 config translation keys.
- **No crafting recipes.** `data/redstone_wire/recipe/` is empty; the blocks are
  creative-tab only.
