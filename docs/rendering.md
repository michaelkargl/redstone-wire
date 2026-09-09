# Rendering

Two separate rendering jobs in this mod:

1. **The blocks themselves** — static geometry, done with JSON models. Vanilla
   handles it.
2. **The cables between them** — dynamic geometry that depends on runtime state,
   done with a `BlockEntityRenderer`. We build the vertices by hand.

> **Changed in 1.21.9.** Rendering was substantially rewritten: `RenderType.create`
> gave way to `RenderPipeline`, and block entity renderers split into a two-phase
> extract/submit model. Tutorials written before that will not compile. See
> [NeoForge Concepts §7](neoforge-concepts.md) for the general shape.

---

## Part 1: Block models

Each block has three JSON files under
`src/main/resources/assets/redstone_wire/`:

| File | Purpose |
| --- | --- |
| `blockstates/<name>.json` | maps block state → model + rotation |
| `models/block/<name>.json` | the geometry (authored in Blockbench) |
| `models/item/<name>.json` | the inventory/hand model |

`FACING` is handled entirely in the blockstate file by rotating one model — there
is no separate model per direction:

```json
"facing=north": { "model": "redstone_wire:block/redstone_connector" },
"facing=south": { "model": "redstone_wire:block/redstone_connector", "y": 180 }
```

Input and Output use `multipart` rather than `variants` because they carry a
second property (`POWER`) — every `facing` × `power` combination must be covered
or the block renders untextured.

### Model rules that cost real debugging time

These were learned the hard way. Ignore them and you get an error cube with a
misleading exception.

- **Custom element models MUST declare `"parent": "block/block"`.** Without it,
  Minecraft rejects the model outright and shows the purple-black error cube.
- **`__comment` keys are safe inside an element object `{}`, but MUST NOT appear
  inside `"faces": {}`.** The face deserializer tries to parse every key in that
  object as a `Direction` enum and throws `JsonParseException` on anything it
  doesn't recognise. It surfaces as a `FileNotFoundException` on the model path,
  which sends you hunting for a missing file that is sitting right there.
- **Non-full-cube blocks MUST set `.noOcclusion()`** in their
  `BlockBehaviour.Properties`, or Minecraft culls the faces of adjacent blocks
  and you get holes in the terrain.

> **Debugging tip.** A `FileNotFoundException` on a model path almost always means
> JSON deserialization failed, not that the file is missing. Check
> `run/logs/latest.log`.

### The voxel shape

The collision/selection shape is shared by all three blocks and defined in Java,
not JSON — `RedstoneWireBlock.ANTENNA_SHAPE`:

```
   ││     ← shaft      (2px wide, Y 5–11)
 │    │   ← base ring  (6px wide, Y 2–5)
│──────│  ← flat slab  (16px wide, Y 0–2)
```

Note the shape is *not* derived from the model. Change one and you must change
the other by hand.

---

## Part 2: Cables

`renderer/CableRenderer` is a shared static utility; all three block entity
renderers call into it. Credit for the original approach goes to
[MaxLegend/OverheadRedstoneWires](https://github.com/MaxLegend/OverheadRedstoneWires).

### The pipeline

```java
public static final RenderPipeline CABLE_PIPELINE =
    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(MODID, "pipeline/cable"))
        .withVertexShader("core/rendertype_leash")
        .withFragmentShader("core/rendertype_leash")
        .withSampler("Sampler2")
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS)
        .build();
```

Points worth understanding:

- It **reuses vanilla's leash shader** (`core/rendertype_leash`). Leads are also
  untextured, vertex-coloured, lightmap-lit hanging lines — exactly our problem.
  Writing a custom shader here would be work for no gain.
- **Culling is off.** The cable is a very thin cuboid; with backface culling on it
  disappears at grazing angles.
- The format is `POSITION_COLOR_LIGHTMAP` — no UVs, because there is no texture.
- The pipeline must be registered on `RegisterRenderPipelinesEvent`
  (`RedstoneWireClient` wires up `CableRenderer::registerRenderPipeline`).

### Extract, then submit

Every BER here implements the same two phases:

```java
// Phase 1 — reads the block entity. Copy plain values into the state object.
extractRenderState(entity, state, ...) {
    state.connections = entity.getConnections();   // immutable snapshot
    state.power       = state.blockState.getValue(POWER);
    state.facing      = state.blockState.getValue(FACING);
}

// Phase 2 — runs later, off-thread. Touch `state` only.
submit(state, stack, nodeCollector, camera) {
    nodeCollector.submitCustomGeometry(stack, LIGHT_COLOR_RENDER,
        (pose, builder) -> renderCurvedCuboid(...));
}
```

`getConnections()` returns `List.copyOf(...)` for exactly this reason — `submit`
must never see a list the server thread can mutate underneath it.

### Geometry

Each cable is 12 straight segments; each segment is a 6-faced cuboid of thickness
0.02 blocks, built from a direction vector and two perpendiculars:

```java
Vec3 dir   = p2.subtract(p1).normalize();
// A near-vertical cable would make dir × up degenerate, so swap the up vector
Vec3 up    = Math.abs(dir.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
Vec3 right = dir.cross(up).normalize().scale(thickness);
```

### Sag

Sag is a sine curve, not a real catenary — cheaper, and visually
indistinguishable at these lengths:

```java
sagAmplitude = min(0.10 * horizontalDistance, 1.0);   // blocks
curve        = sin(t * PI) * -sagAmplitude;            // negative = downward
```

So a cable droops 0.10 blocks per block of horizontal span, clamped to 1.0 block
at the midpoint. Purely vertical cables (horizontal distance ≈ 0) skip the curve
entirely and draw straight.

### Drawing each cable once

A link is stored on *both* block entities, so both would draw it. The connector
renderer breaks the tie by position ordering:

```java
if (blockPos.compareTo(connection) < 0) { /* draw */ }
```

Only the lower position renders. Without this you pay double the geometry and get
z-fighting.

---

## Known issues

**Cables don't change colour with power.** `CableRenderer` captures `power` into
the render state and then never uses it — `drawThickSegment` hardcodes
`setColor(0.3f, 0, 0, 1f)`. The capture is deliberate groundwork for a later
change. Note also that connectors have no stored power at all:
`RedstoneWireBlockEntity.getSignal()` returns `0` and
`RedstoneConnectorBlockEntity` does not override it, so wiring colour up means
propagating power into connectors first.

**Attachment heights disagree.** `RedstoneWireBlock.ANTENNA_TIP_Y` is
`10.0f / 32.0f` (= 0.3125) and is used by the Input and Output renderers, while
`RedstoneConnectorBlockEntityRenderer` hardcodes a local
`11.0 / 16.0` (= 0.6875). The voxel shape puts the shaft tip at Y=11/16, and the
constant's own javadoc says "block pixels / 16" while the value divides by 32.
The result is that an Input↔Connector cable attaches at two different heights.
The Connector model geometry does start higher than the others, so the fix is not
simply making both use the same number — it needs a per-block attachment point.
