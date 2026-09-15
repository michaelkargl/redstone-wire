---
id: RW-11
title: Upgrade RedstoneWire to Minecraft 26.2
status: In Progress
assignee: []
created_date: '2026-09-15 06:31'
labels: []
dependencies: []
references:
  - docs/upgrade-26.2-plan.md
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Port the validated Minecraft 26.1 baseline to stable Minecraft 26.2 and adopt the Minecraft-scoped release version 26.2-1.0.0. Execute the linked plan one gated step at a time and pause on its documented stop conditions.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Minecraft metadata targets exactly 26.2 with compatibility range [26.2].
- [ ] #2 The mod, JAR, tag, and release metadata use 26.2-1.0.0.
- [ ] #3 Production build and all five required GameTests pass on Java 25.
- [ ] #4 A clean dedicated server and client load RedstoneWire successfully.
- [ ] #5 World persistence and cable rendering are manually verified before release.
- [ ] #6 Publishing remains paused until explicit approval.
<!-- AC:END -->
