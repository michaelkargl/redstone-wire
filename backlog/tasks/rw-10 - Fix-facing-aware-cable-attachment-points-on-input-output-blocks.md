---
id: RW-10
title: Fix facing-aware cable attachment points on input/output blocks
status: To Do
assignee: []
created_date: '2026-03-22 10:30'
labels:
  - rendering
  - bug
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Cable rendering on input and output blocks connects at the block center instead of the antennae tip. The attachment point needs to be computed from the antennae tip offset in the block model, rotated by the block's FACING direction. There is also a regression in RedstoneOutputBlockEntityRenderer where antennaeAttachmentPointY was changed from 11.0/16.0 to 11.0/32.0 (wrong divisor), and an incomplete var anntenae line that needs to be cleaned up. See openspec/changes/fix-cable-attachment-points/ for complete spec artifacts.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] Cables on input blocks visually originate from the antennae tip for all 4 FACING directions
- [ ] Cables on output blocks visually originate from the antennae tip for all 4 FACING directions
- [ ] Output renderer regression (wrong BlockSize constant divisor and incomplete `var anntenae` line) is removed
- [ ] Y attachment heights match current model geometry: input = 9/16, output = 7/16
<!-- AC:END -->
