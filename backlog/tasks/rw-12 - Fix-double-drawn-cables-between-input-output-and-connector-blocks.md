---
id: RW-12
title: Fix double-drawn cables between input/output and connector blocks
status: To Do
assignee: []
created_date: '2026-09-16 09:35'
updated_date: '2026-09-16 09:35'
labels:
  - rendering
  - bug
dependencies: []
priority: medium
ordinal: 600
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A cable between two blocks is drawn twice when one end is an input or output block.

`RedstoneConnectorBlockEntityRenderer.submit()` guards against drawing a cable from both ends
with `if (blockPos.compareTo(connection) < 0)`, so a connector-to-connector cable is drawn
exactly once. `RedstoneInputBlockEntityRenderer.submit()` and
`RedstoneOutputBlockEntityRenderer.submit()` have no such guard and draw every connection
unconditionally. When a connector's position sorts lower than the linked input/output block,
the connector draws the same cable a second time.

Visible as slightly darker cables (the geometry is alpha-blended twice) and z-fighting shimmer
where the two copies overlap. Also doubles the vertex count for those cables.

Fix by choosing one owner per cable. Either extend the `compareTo` ordering to all three
renderers, or make the connector skip connections whose block entity is not itself a connector
and let the input/output side own those cables.

Found while fixing the frustum-culling bug (missing `getRenderBoundingBox` overrides); kept
separate to keep that PR to one responsibility.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each cable is submitted exactly once per frame, regardless of which block types it connects
- [ ] #2 Cable colour/opacity is identical for connector-connector and input-connector cables
- [ ] #3 No z-fighting shimmer on input-connector and output-connector cables when the camera moves
- [ ] #4 Ownership rule is applied consistently across all three cable renderers and documented in CableRenderer
<!-- AC:END -->
