---
id: RW-09
title: Implement pre-compiled routing table for signal propagation
status: To Do
assignee: []
created_date: '2026-02-23 19:17'
updated_date: '2026-02-23 19:21'
labels:
  - signal-propagation
  - performance
  - architecture
dependencies: []
references:
  - src/main/java/at/osa/redstonewire/RedstoneConnectorBlockEntity.java
  - src/main/java/at/osa/redstonewire/RedstoneConnectorBlock.java
  - src/main/java/at/osa/redstonewire/RedstoneInputBlock.java
  - src/main/java/at/osa/redstonewire/RedstoneOutputBlock.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Goal

Replace the current BFS traversal (runs on every signal change) with a pre-compiled routing table. The InputBlock should speak directly to OutputBlocks without traversing the connector graph at signal time.

## How it works

**Phase 1 — Topology setup** (rare: triggered when network is wired up)
```
Player connects Connector A to Connector B:

  [InputBlock] ─── [ConnA] ─── [ConnB] ─── [OutputBlock]
                     ↑
               addConnection() fires
               → BFS crawls entire graph ONCE
               → discovers all reachable OutputBlocks
               → stores flat list in each connector:
                 reachableOutputs = [ OutputBlock@(x,y,z) ]
```

**Phase 2 — Signal propagation** (hot path: runs every signal edge, 20x/sec for clocks)
```
Lever toggled → InputBlock.neighborChanged fires:

  [InputBlock] ─── [ConnA] ─── [ConnB] ─── [OutputBlock]
       │
       └─ calls ConnA.propagateSignal(15)
                  └─ iterates reachableOutputs list
                  └─ level.setBlock( OutputBlock@(x,y,z), POWER=15 )
                       ↑
                  DIRECT SET — ConnB is never touched
```

**Complexity comparison**
- Current BFS: O(connectors + outputs) per signal edge
- Routing table: O(outputs) per signal edge — setup cost O(connectors) paid once at topology change

## Implementation

### `RedstoneConnectorBlockEntity`
- Add `List<BlockPos> reachableOutputs` (in-memory, not persisted)
- Add `boolean cacheDirty = true`
- Add `rebuildOutputCache(Level level)` — BFS through `directConnections`, scan 6 faces of each visited connector for adjacent `RedstoneOutputBlock`s
- Add `propagateSignal(int power, Level level)` — lazy rebuild if dirty, then iterate `reachableOutputs` and `level.setBlock(...)` each with `Block.UPDATE_ALL`
- Modify `addConnection()` — mark `cacheDirty = true` on all connectors in the network after adding
- Modify `loadAdditional()` — set `cacheDirty = true` (cache is not serialised, rebuilt lazily)
- Remove `receiveSignal()` BFS method and `updateAdjacentOutputs()` helper

### `RedstoneConnectorBlock`
- Add `neighborChanged()` — if adjacent block is a `RedstoneOutputBlock` (placed or broken), mark own entity's cache dirty

### `RedstoneInputBlock.neighborChanged` (completes existing TODO)
- Read `level.getBestNeighborSignal(pos)`, compare to current POWER
- If changed: `level.setBlock(pos, state.setValue(POWER, newPower), Block.UPDATE_ALL)`
- For each `Direction`: if adjacent entity is `RedstoneConnectorBlockEntity`, call `.propagateSignal(newPower, level)`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Lever → InputBlock → ConnectorA → ConnectorB → OutputBlock → Lamp: lamp turns on/off when lever flips
- [ ] #2 Signal updates are instant with no visible lag
- [ ] #3 Connections survive world save/reload
- [ ] #4 Breaking a connector mid-chain stops signal propagation
- [ ] #5 Reconnecting after break restores signal propagation
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Phase 0 — Make signal propagation work (immediate)

The BFS infrastructure is already fully implemented in `RedstoneConnectorBlockEntity.receiveSignal()`.
The only missing piece is wiring up `RedstoneInputBlock.neighborChanged()`.

### Step 1 — Implement `RedstoneInputBlock.neighborChanged()`
File: `src/main/java/at/osa/redstonewire/RedstoneInputBlock.java`

Replace the `TODO(human)` at line 49 with:
1. Read incoming power: `int newPower = level.getBestNeighborSignal(pos)`
2. Read current stored power: `int currentPower = state.getValue(POWER)`
3. If `newPower == currentPower`, return early (no change)
4. Update this block's state: `level.setBlock(pos, state.setValue(POWER, newPower), Block.UPDATE_CLIENTS)`
5. Iterate `Direction.values()`, check if `level.getBlockEntity(pos.relative(dir))` is a `RedstoneConnectorBlockEntity`
6. If yes, call `connector.receiveSignal(newPower, level)`

`receiveSignal()` already does BFS through connectors and calls `updateAdjacentOutputs()` which
sets `RedstoneOutputBlock.POWER` via `Block.UPDATE_ALL` — that triggers Minecraft's redstone
neighbor updates and lights up lamps, etc.

### Step 2 — Fix item type in `RedstoneConnectorBlock.java:52`
Currently uses `Items.REDSTONE` (changed during debugging). Decide on final item:
- `Items.STRING` was the original design (makes thematic sense as a "wire")
- `Items.REDSTONE` is also reasonable (redstone dust = wire)
Update the `if (!heldItem.is(Items.REDSTONE))` check accordingly.

## Phase 1 — Routing table optimization (RW-09 original goal)

Only attempt this after Phase 0 is verified working in-game.

Replace `receiveSignal()` BFS-at-signal-time with a pre-compiled cache:

### Step 1 — Add cache fields to `RedstoneConnectorBlockEntity`
```java
private final List<BlockPos> reachableOutputs = new ArrayList<>();
private boolean cacheDirty = true;
```

### Step 2 — Add `rebuildOutputCache(Level level)` method
- BFS through `directConnections` (same algorithm as current `receiveSignal()`)
- For each visited connector, check 6 faces for `RedstoneOutputBlock`
- Collect all found output positions into `reachableOutputs`
- Set `cacheDirty = false`

### Step 3 — Add `propagateSignal(int power, Level level)` method
- If `cacheDirty`, call `rebuildOutputCache(level)` first
- Iterate `reachableOutputs`, set each output's POWER block state via `Block.UPDATE_ALL`
- Remove the old `receiveSignal()` method

### Step 4 — Mark cache dirty on topology changes
In `addConnection()` and a new `removeConnection()`: set `cacheDirty = true` on all
reachable connectors (another BFS), then call `syncToClient()`

In `RedstoneConnectorBlock`, add `neighborChanged()` to detect when an OutputBlock
is placed/broken adjacent to a connector and mark `cacheDirty = true`.

### Step 5 — Update `RedstoneInputBlock.neighborChanged()` call site
Replace `connector.receiveSignal(power, level)` with `connector.propagateSignal(power, level)`
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Current Code State (as of 2026-02-23)

### What is DONE and working:
- `RedstoneConnectorBlockEntity.receiveSignal(int power, Level level)` — full BFS traversal, updates each connector's POWER block state, and calls `updateAdjacentOutputs()` on each (lines 158–179)
- `RedstoneConnectorBlockEntity.updateAdjacentOutputs()` — checks all 6 faces for `RedstoneOutputBlock`, sets their POWER via `Block.UPDATE_ALL` (lines 184–192)
- `RedstoneOutputBlock.isSignalSource()` and `getSignal()` — emits redstone to neighbors when POWER > 0 (lines 54–61)
- `RedstoneConnectorBlock` — two-click connection system working; string-click creates bidirectional connections; cable renders between blocks
- All three blocks registered in `RedstoneWire.java`; `REDSTONE_CONNECTOR_ENTITY` registered for `RedstoneConnectorBlock` only

### What is NOT done:
- `RedstoneInputBlock.neighborChanged()` has a `TODO(human)` at line 49 — signal never enters the network
- `RedstoneInputBlock` and `RedstoneOutputBlock` do NOT have `BlockEntity` — they don't need one for Phase 0

### Known discrepancy:
- `RedstoneConnectorBlock.java:52` checks `Items.REDSTONE` — was changed from `Items.STRING` during debugging. Intentional or should be reverted? Confirm before next session.

### Architecture note — WireNodeBlock refactor (separate task RW-08.01):
The session discussed extracting `RedstoneConnectorBlock`'s connection logic into a `WireNodeBlock` abstract base class so Input/Output blocks could also create connections. This is NOT required for signal propagation to work — it is a code-quality improvement. Do NOT block Phase 0 on it.

### Complete signal flow (Phase 0):
```
Lever ON
  → neighborChanged fires on adjacent RedstoneInputBlock
    → level.getBestNeighborSignal(pos) = 15
    → POWER state updated on InputBlock
    → iterate Direction.values()
      → found RedstoneConnectorBlockEntity at (x,y,z)
        → connector.receiveSignal(15, level)  [BFS starts here]
          → sets each reachable connector's POWER = 15
          → for each connector, checks 6 faces for RedstoneOutputBlock
            → sets RedstoneOutputBlock POWER = 15 via UPDATE_ALL
              → Minecraft fires neighborChanged on lamp/piston/etc
                → lamp turns on
```

### NBT persistence:
`directConnections` is serialized to disk. `reachableOutputs` (Phase 1) should NOT be persisted — it is derived/cached data. Rebuild it lazily on first `propagateSignal()` after world load.
<!-- SECTION:NOTES:END -->
