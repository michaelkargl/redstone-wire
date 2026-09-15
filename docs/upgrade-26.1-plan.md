# Minecraft 26.1 Upgrade Plan

Status: **Validated as the Minecraft 26.2 migration baseline**

## Scope

Upgrade Redstone Wire one Minecraft release family at a time:

- From Minecraft `1.21.11`
- To Minecraft `26.1` exactly
- Do not include compatibility with `26.1.1`, `26.1.2`, or `26.2` in this change

The next upgrade can monkey-bar from this completed 26.1 baseline.

## Target stack

| Component | Before | Target |
| --- | --- | --- |
| Minecraft | `1.21.11` | `26.1` |
| Minecraft compatibility range | `[1.21.11]` | `[26.1]` |
| NeoForge | `21.11.45` | `26.1.0.19-beta` |
| Java | `21` | `25` |
| ModDevGradle | `2.0.144` | `2.0.146` |
| Parchment | `2025.12.20` | Removed |
| Redstone Wire | `1.0.9` | `1.0.10` |

These versions match the official NeoForge Minecraft 26.1 ModDevGradle MDK.
Gradle `9.2.1` was already new enough and was left unchanged.

## Work completed before the pause

- Updated `gradle.properties` to Minecraft `26.1`, exact range `[26.1]`,
  NeoForge `26.1.0.19-beta`, and mod version `1.0.10`.
- Updated ModDevGradle to `2.0.146`.
- Updated the Java toolchain and GitHub Actions jobs to Java 25.
- Removed Parchment configuration because Minecraft 26.1 ships unobfuscated names.
- Downloaded and generated the Minecraft 26.1 / NeoForge development artifacts.
- Ran the first `compileJava` pass. It reached project compilation and reported
  17 source errors in three API-change groups.
- Applied fixes for all 17 reported errors:
  - `CameraRenderState` moved to `net.minecraft.client.renderer.state.level`.
  - `BlockEntityRenderState.blockState` became private, so render extraction now
    reads the state from `entity.getBlockState()`.
  - `Player.displayClientMessage(message, true)` became
    `Player.sendOverlayMessage(message)`.

## Final baseline verification

The 26.1 upgrade was committed as `34636ac` and re-verified on 2026-09-15 before
starting the Minecraft 26.2 port:

- Gradle 9.2.1 ran on Java 25.0.2.
- Freshly generated metadata reported RedstoneWire `1.0.10`, Minecraft `[26.1]`,
  and NeoForge `[26.1.0.19-beta,)`.
- `compileJava` and `build` completed successfully.
- `runGameTestServer` discovered and passed all five required tests.
- The isolated dedicated server loaded RedstoneWire `1.0.10` and reached `Done`.
- The isolated client loaded Minecraft 26.1, NeoForge, and RedstoneWire `1.0.10`,
  then completed client setup, resource loading, texture-atlas creation, and
  audio startup without a RedstoneWire error.

This establishes a sound migration baseline. The complete manual cable visual
check is intentionally carried forward as a required release gate for 26.2.

## Resume checklist

1. Re-run Java compilation.

   ```powershell
   $env:JAVA_HOME = 'C:\Users\kami\.jdks\openjdk-25.0.2-1'
   $env:Path = "$env:JAVA_HOME\bin;$env:Path"
   .\gradlew.bat compileJava
   ```

2. Treat each new compiler error as a 26.1 migration item. Consult the generated
   26.1 sources in Gradle's NeoForm cache before choosing replacement APIs.

3. Once production code compiles, compile and migrate the GameTests. Pay special
   attention to registry names, test-instance JSON, fixture loading, and helper
   coordinate behavior.

4. Audit resources and metadata even if compilation succeeds:
   - `META-INF/neoforge.mods.toml` expansion and exact Minecraft range
   - `pack.mcmeta` / pack-format expectations
   - block models, item models, blockstates, translations, and test-instance JSON
   - render pipeline, shader names, vertex format, and cable geometry submission

5. Run verification in increasing cost order:

   ```powershell
   .\gradlew.bat compileJava
   .\gradlew.bat build
   .\gradlew.bat runGameTestServer
   .\gradlew.bat runServer
   ```

6. Inspect the client visually before release. Verify connector/input/output
   models, facing-aware cable attachment points, cable sag, lighting, and block
   breaking behavior. Rendering changed substantially in the previous upgrade and
   compilation cannot prove visual correctness.

7. Review documentation for stale Java 21, Minecraft 1.21.11, NeoForge 21.11,
   Parchment, GameTest, and renderer guidance. Preserve
   `docs/upgrade-1.21.11-report.html` as a historical report rather than rewriting
   its point-in-time values.

8. Review `git diff`, confirm the produced artifact is
   `build/libs/redstone_wire-1.0.10.jar`, then commit only after all automated
   checks pass and the client visual test is recorded.

## Known operational note

This shell has no Java on its default `PATH`. Gradle must be launched with the
installed Java 25 JDK shown above. Accessing that JDK and downloading dependencies
may require sandbox approval.

The repository requests Backlog.md MCP for task tracking, but no Backlog MCP
resource was available in this session. Create or update the upgrade task through
Backlog tooling when work resumes; do not hand-edit Backlog task files.
