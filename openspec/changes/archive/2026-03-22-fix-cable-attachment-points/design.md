## Context

Both `RedstoneInputBlockEntityRenderer` and `RedstoneOutputBlockEntityRenderer` draw a cable from their block to each connected block using `CableRenderer.renderCable()`. The cable start point (attachment on the source block) is currently hardcoded to `Vec3(0.5, y, 0.5)` — the horizontal center of the block — regardless of facing direction.

The block models place the antennae off-center (right side when facing NORTH):
- Input: antennae tip at model coords `(11, 9, 8)` → block-local `(0.6875, 0.5625, 0.5)` when NORTH
- Output: two antennae averaged at `(12, 7, 8)` → block-local `(0.75, 0.4375, 0.5)` when NORTH

Both antennae are centered in Z and offset in X. When the block rotates via `FACING`, the X offset becomes a Z offset (EAST/WEST) or reverses sign (SOUTH). The current hardcoded center does not rotate with the block, causing cables to visually disconnect from the antennae.

A secondary regression in `RedstoneOutputBlockEntityRenderer` changed `antennaeAttachmentPointY` from `11.0 / 16.0` to `11.0 / BlockSize` (where `BlockSize = 32.0f`), halving the Y. There is also an incomplete `var anntenae` statement left from an interrupted edit.

## Goals / Non-Goals

**Goals:**
- Cable start point visually connects to the antennae tip on both input and output blocks
- Attachment point rotates correctly for all four FACING directions (NORTH, SOUTH, EAST, WEST)
- Remove the `BlockSize` regression and incomplete variable from the output renderer
- Correct the Y attachment heights to match the current block models

**Non-Goals:**
- Changing the cable end point (connection to Connector blocks — that end is already block-center and is acceptable)
- Animating or dynamically reading the antennae position from the model JSON at runtime
- Supporting UP/DOWN facing (blocks only face horizontal directions)

## Decisions

### Decision: Hardcode antennae offset constants per renderer, not read from model JSON

The antennae tip position is a visual design constant that changes only when the model is redesigned. Reading it dynamically from the model at runtime would require parsing JSON, finding the right element, and transforming coordinates — significant complexity for no practical benefit.

**Alternative considered**: Expose a `getAntennaeAttachmentPoint(Direction)` method on the block entity. Rejected because the renderers are the only consumers of this value, and it would mix rendering concerns into the block entity layer.

**Chosen approach**: Each renderer defines its own `ANTENNAE_OFFSET` constant (the X offset from block center, i.e., `tipX - 0.5`) and `ANTENNAE_Y` constant. The facing-aware position is computed inline in `render()`.

### Decision: Offset is applied to the facing direction's right-hand axis

The antennae offset in model-local space is always along the +X axis (right side when facing NORTH). The correct world-local offset for each facing is:

```
NORTH → +X offset  →  start = (0.5 + offset, y, 0.5)
SOUTH → -X offset  →  start = (0.5 - offset, y, 0.5)
EAST  → +Z offset  →  start = (0.5, y, 0.5 + offset)
WEST  → -Z offset  →  start = (0.5, y, 0.5 - offset)
```

This is equivalent to rotating the model-local offset vector `(offset, 0, 0)` by the facing's clockwise angle around Y.

**Alternative considered**: Use `Direction.step()` and dot products. Rejected for added complexity; a switch/if-chain on the four directions is simpler and exhaustive.

## Risks / Trade-offs

- **Model changes break the constants** → If the block model is redesigned with a different antennae position, the renderer constants need manual updating. Mitigation: the constants are clearly named and documented with their model-coordinate source.
- **Y height discrepancy with old value** → The output renderer's original `11.0/16.0 = 0.6875` was above all geometry in the current model (antennae top is at Y=7/16=0.4375). Either the model changed or the old value was always slightly off. The fix targets the current model geometry.

## Migration Plan

Rendering-only change. No save data, network packets, or block state properties are affected. No migration needed. Rollback is a one-line revert of each renderer file.
