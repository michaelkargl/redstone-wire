---
id: RW-09
title: Implement pre-compiled routing table for signal propagation
status: To Do
assignee: []
created_date: '2026-02-23 19:17'
updated_date: '2026-02-24 04:11'
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

### Step 1 — Implement `RedstoneInputBlock.neighborChanged()`
File: `src/main/java/at/osa/redstonewire/RedstoneInputBlock.java`

1. Read incoming power: `int newPower = level.getBestNeighborSignal(pos)`
2. Read current stored power: `int currentPower = state.getValue(POWER)`
3. If `newPower == currentPower`, return early (no change)
4. Update this block's state: `level.setBlock(pos, state.setValue(POWER, newPower), Block.UPDATE_CLIENTS)`
5. Iterate `Direction.values()`, check if `level.getBlockEntity(pos.relative(dir))` is a `RedstoneConnectorBlockEntity`
6. If yes, call `connector.propagateSignal(newPower, level)`

---

## Phase 1 — Routing table (RW-09 original goal)

### Traversal design — recursive DFS with `visited` set as parameter

Both traversal methods use the same recursive DFS + `visited` pattern for loop prevention. Connectors are bidirectionally linked, so without a `visited` set the graph would cycle (A→B→A→B...).

Each connector marks itself / contributes its own data and then delegates to its neighbors:

```java
// markNetworkDirty — each connector sets its own cacheDirty
public void markNetworkDirty(Set<BlockPos> visited) {
    if (!visited.add(this.getBlockPos())) return; // loop prevention
    this.cacheDirty = true;
    for (BlockPos neighbor : directConnections) {
        var be = level.getBlockEntity(neighbor);
        if (be instanceof RedstoneConnectorBlockEntity conn)
            conn.markNetworkDirty(visited);
    }
}

// rebuildOutputCache — entry point creates accumulator, DFS worker appends to it
void rebuildOutputCache(Level level) {
    reachableOutputs.clear();
    rebuildOutputCache(level, new HashSet<>(), reachableOutputs);
    cacheDirty = false;
}

void rebuildOutputCache(Level level, Set<BlockPos> visited, List<BlockPos> accumulator) {
    if (!visited.add(this.getBlockPos())) return;
    for (Direction dir : Direction.values()) {
        BlockPos adj = getBlockPos().relative(dir);
        if (level.getBlockState(adj).getBlock() instanceof RedstoneOutputBlock)
            accumulator.add(adj);
    }
    for (BlockPos neighbor : directConnections) {
        var be = level.getBlockEntity(neighbor);
        if (be instanceof RedstoneConnectorBlockEntity conn)
            conn.rebuildOutputCache(level, visited, accumulator);
    }
}
```

**Why accumulator instead of return-list?**
Return-list is O(N × M) in the worst case (linear chain, every connector has an output — ~500k copies for 1000 nodes). Accumulator is O(N + M): each output is appended exactly once. Rebuild is rare (topology change only), but the accumulator pattern is strictly better and equally readable.

### Step 1 — Add cache fields to `RedstoneConnectorBlockEntity`
```java
private final List<BlockPos> reachableOutputs = new ArrayList<>();
private boolean cacheDirty = true;
```
Not persisted — derived data, rebuilt lazily.

### Step 2 — Add `rebuildOutputCache(Level level)` + DFS worker
See traversal design above.

### Step 3 — Add `propagateSignal(int power, Level level)`
- If `cacheDirty`, call `rebuildOutputCache(level)` first (lazy rebuild — paid once per topology change)
- Iterate `reachableOutputs`, set each output's POWER via `Block.UPDATE_ALL`

Call chain:
```
topology change  →  addConnection() / removeConnection()  →  markNetworkDirty(new HashSet<>())  →  cacheDirty = true (all connectors)
signal edge      →  propagateSignal()  →  [if dirty] rebuildOutputCache()  →  iterate reachableOutputs
```

### Step 4 — Mark cache dirty on topology changes
- `addConnection()`: after adding, call `markNetworkDirty(new HashSet<>())`
- `removeConnection(BlockPos pos)` (new method): remove from `directConnections`, call `markNetworkDirty(new HashSet<>())`, then `setChanged()` + `syncToClient()`
- `loadAdditional()`: set `cacheDirty = true` (cache not serialised, rebuild on first propagation after world load)

### Step 5 — Update `RedstoneInputBlock.neighborChanged()` call site
Uses `connector.propagateSignal(power, level)` (Phase 0 already wires this up).
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
