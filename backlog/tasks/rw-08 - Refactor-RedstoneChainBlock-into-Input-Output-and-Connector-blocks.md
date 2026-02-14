---
id: RW-08
title: 'Refactor RedstoneChainBlock into Input, Output, and Connector blocks'
status: To Do
assignee: []
created_date: '2026-02-14 19:40'
labels:
  - refactor
  - architecture
milestone: m-1
dependencies: []
references:
  - src/main/java/at/osa/redstonewire/RedstoneChainBlock.java
  - src/main/java/at/osa/redstonewire/RedstoneChainEntity.java
  - src/main/java/at/osa/redstonewire/RedstoneWire.java
documentation:
  - /Users/kami/.claude/plans/concurrent-sniffing-rossum.md
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Split the monolithic RedstoneChainBlock into three specialized block types to simplify signal propagation. Input blocks detect external redstone signals, Output blocks emit signals to surroundings, and Connector blocks are passive cable routing nodes. The existing RedstoneChainBlock is kept as a legacy block for backward compatibility.

Key design decisions:
- Adjacent-touching networks removed from new blocks (cable-only connections)
- Connector blocks have NO POWER property (truly passive, single blockstate)
- Only Input block entities tick periodically for self-correction
- Single shared RedstoneChainEntity type with instanceof checks for role-aware behavior
- Legacy RedstoneChainBlock kept unchanged as 4th block type
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Input block detects external redstone signals and feeds them into the cable network
- [ ] #2 Output block emits network signal to adjacent blocks via isSignalSource/getSignal
- [ ] #3 Connector block is purely passive - no POWER property, no signal detection or emission
- [ ] #4 Legacy RedstoneChainBlock still works unchanged in existing worlds
- [ ] #5 All three new block types can be cable-connected using the Connector item
- [ ] #6 Cable rendering works between all block type combinations
- [ ] #7 Signal propagation: lever -> Input -> Connector(s) -> Output -> lamp works end-to-end
<!-- AC:END -->
