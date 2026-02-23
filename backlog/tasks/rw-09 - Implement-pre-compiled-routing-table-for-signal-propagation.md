---
id: RW-09
title: Implement pre-compiled routing table for signal propagation
status: To Do
assignee: []
created_date: '2026-02-23 19:17'
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
- [ ] #1 ./gradlew build passes
- [ ] #2 Lever → InputBlock → ConnA → ConnB → OutputBlock → Lamp: toggling lever lights lamp instantly
- [ ] #3 Fast redstone clock connected to InputBlock: output follows at full clock speed (no tick latency)
- [ ] #4 Breaking a connector in the chain: lamp turns off (cache invalidated)
- [ ] #5 Reconnecting: lamp turns on again (cache rebuilt)
<!-- AC:END -->
