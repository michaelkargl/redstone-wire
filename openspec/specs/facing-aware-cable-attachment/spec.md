## Requirements

### Requirement: Cable attaches at the antennae tip
The cable rendered from an input or output block SHALL originate from the tip of the antennae on that block model, not from the geometric center of the block.

#### Scenario: Cable origin on input block facing NORTH
- **WHEN** a `RedstoneInputBlock` facing NORTH renders its cables
- **THEN** the cable start point SHALL be at block-local position `(0.6875, 0.5625, 0.5)` (antennae tip at model X=11, Y=9, Z=8 in 16-unit space)

#### Scenario: Cable origin on output block facing NORTH
- **WHEN** a `RedstoneOutputBlock` facing NORTH renders its cables
- **THEN** the cable start point SHALL be at block-local position `(0.75, 0.4375, 0.5)` (antennae tip at model X=12, Y=7, Z=8 in 16-unit space)

### Requirement: Cable attachment point rotates with block facing
The cable start point SHALL be computed by rotating the model-local antennae offset according to the block's `FACING` property, so that the cable always originates from the antennae tip regardless of block orientation.

#### Scenario: Input block facing SOUTH
- **WHEN** a `RedstoneInputBlock` facing SOUTH renders its cables
- **THEN** the cable start X SHALL be mirrored: `0.5 - 0.1875 = 0.3125`

#### Scenario: Input block facing EAST
- **WHEN** a `RedstoneInputBlock` facing EAST renders its cables
- **THEN** the antennae offset SHALL be applied to Z: start = `(0.5, y, 0.5 + 0.1875)`

#### Scenario: Input block facing WEST
- **WHEN** a `RedstoneInputBlock` facing WEST renders its cables
- **THEN** the antennae offset SHALL be applied to Z negatively: start = `(0.5, y, 0.5 - 0.1875)`

### Requirement: Output renderer regression is corrected
The `RedstoneOutputBlockEntityRenderer` SHALL NOT contain the `BlockSize` field or the incomplete `var anntenae` statement introduced by a prior incomplete edit.

#### Scenario: Y attachment height is correct for output block
- **WHEN** a `RedstoneOutputBlock` renders its cables
- **THEN** the Y attachment height SHALL be `7.0 / 16.0 = 0.4375`, matching the current model's antennae tip height
