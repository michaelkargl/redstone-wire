## Why

Cable rendering on input and output blocks visually connects at the block center rather than the antennae tip, because the attachment point is hardcoded to `(0.5, y, 0.5)` and does not account for the antennae's offset position or the block's `FACING` direction. A recent regression also halved the Y attachment height on the output block by dividing by `32` instead of `16`.

## What Changes

- Fix `RedstoneOutputBlockEntityRenderer`: remove broken `BlockSize` constant and incomplete `var anntenae` line; restore correct Y attachment height
- Fix both `RedstoneInputBlockEntityRenderer` and `RedstoneOutputBlockEntityRenderer` to compute the cable start position from the actual antennae tip in model-local space, rotated by the block's `FACING` direction
- No changes to block logic, NBT, or signal propagation

## Capabilities

### New Capabilities

- `facing-aware-cable-attachment`: Cable start point is computed from the antennae tip offset rotated by the block's `FACING` direction, so cables always visually originate from the correct point on the model regardless of orientation.

### Modified Capabilities

<!-- None — this is a rendering-only fix with no spec-level behavior changes -->

## Impact

- `RedstoneOutputBlockEntityRenderer.java` — rewrite attachment point calculation
- `RedstoneInputBlockEntityRenderer.java` — rewrite attachment point calculation
- No changes to block, block entity, model, or texture files
