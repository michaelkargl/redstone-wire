---
id: RW-11
title: Upgrade RedstoneWire to Minecraft 26.2
status: In Progress
assignee: []
created_date: '2026-09-15 06:31'
updated_date: '2026-09-15 09:24'
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
- [x] #1 Minecraft metadata targets exactly 26.2 with compatibility range [26.2].
- [ ] #2 The mod, JAR, tag, and release metadata use 26.2-1.0.0.
- [x] #3 Production build and all five required GameTests pass on Java 25.
- [ ] #4 A clean dedicated server and client load RedstoneWire successfully.
- [ ] #5 World persistence and cable rendering are manually verified before release.
- [ ] #6 Publishing remains paused until explicit approval.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Headless verification on 2026-09-15, Java 25.0.4.1. NOTE: the plan's JDK path C:\Users\kami\.jdks\openjdk-25.0.2-1 does not exist on this machine; the real one is C:\Users\MichaelKargl\.jdks\temurin-25.0.4.1. The plan has been corrected.

Green: compileJava --rerun-tasks clean against NeoForge 26.2.0.87 (no inherited 26.1 cache).

Green: the cable RenderPipeline port was checked member-by-member against the patched 26.2 jar. BindGroupLayouts.SAMPLER2, DepthStencilState.DEFAULT, withBindGroupLayout, withVertexBinding, withPrimitiveTopology and withDepthStencilState all exist with the used signatures - the port is correct, not guessed.

Green: build produces redstone_wire-26.2-1.0.0.jar; generated and packaged neoforge.mods.toml both read version 26.2-1.0.0, minecraft [26.2], neoforge [26.2.0.87,).

Green: runGameTestServer discovered and passed all 5 required GameTests in 681.9 ms.

Green: the isolated dedicated server loaded RedstoneWire 26.2-1.0.0 and reached Done.

AC#2 partial: mod version and JAR name are correct; the Git tag and GitHub release do not exist yet.

AC#4 partial: server side verified; no graphical client was launched in this session.

AC#5 open and the main remaining risk: 26.2 switches to a reversed depth buffer, and cable appearance has never been looked at. A human needs to run runClient and inspect it.
<!-- SECTION:NOTES:END -->
