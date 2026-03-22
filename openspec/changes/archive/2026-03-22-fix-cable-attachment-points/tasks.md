## 1. Fix Output Renderer Regression

- [x] 1.1 Remove the `BlockSize` field and the incomplete `var anntenae` line from `RedstoneOutputBlockEntityRenderer`
- [x] 1.2 Set `antennaeAttachmentPointY` to `7.0 / 16.0` in the output renderer to match the current model's antennae tip height

## 2. Implement Facing-Aware Attachment for Output Block

- [x] 2.1 Read `FACING` from `entity.getBlockState()` in `RedstoneOutputBlockEntityRenderer.render()`
- [x] 2.2 Define the antennae X offset constant (`0.25`) and compute the facing-rotated start `Vec3` using a switch on FACING direction
- [x] 2.3 Replace the hardcoded `new Vec3(0.5, antennaeAttachmentPointY, 0.5)` start with the facing-aware start point

## 3. Implement Facing-Aware Attachment for Input Block

- [x] 3.1 Read `FACING` from `entity.getBlockState()` in `RedstoneInputBlockEntityRenderer.render()`
- [x] 3.2 Define the antennae X offset constant (`0.1875`) and correct Y to `9.0 / 16.0`; compute the facing-rotated start `Vec3`
- [x] 3.3 Replace the hardcoded `new Vec3(0.5, antennaeAttachmentPointY, 0.5)` start with the facing-aware start point
