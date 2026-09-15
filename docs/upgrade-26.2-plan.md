# Minecraft 26.2 Upgrade Plan

Status: **Headless verification complete; paused before the client visual pass**
Plan date: **2026-09-15**

Pause recorded: **2026-09-15, at the user's request**

## Outcome

Upgrade RedstoneWire from the current Minecraft `26.1` codebase to the next
stable Java Edition release, Minecraft `26.2`, and publish the first build under
the new Minecraft-scoped version:

```text
26.2-1.0.0
```

The finished JAR will target **Minecraft Java Edition 26.2 exactly**, run on both
the NeoForge client and dedicated server, and be named:

```text
redstone_wire-26.2-1.0.0.jar
```

The configuration, source, metadata, and release-automation changes have now
been applied and verified headlessly on 2026-09-15. Only the graphical client
pass and the world-migration check remain.

## Scope decisions

- Start from the clean `feat/upgrade-to-26.1` tip (`34636ac`), not from
  `master`, which still targets Minecraft `1.21.11`.
- Target only stable Minecraft `26.2`; do not target 26.3 snapshots,
  pre-releases, or release candidates.
- Declare exact Minecraft compatibility as `[26.2]`. This JAR will not claim to
  support `26.1`, a future `26.2.1`, or `26.3` without separate verification.
- Keep Java 25 and Mojang's unobfuscated names.
- Make only compatibility, build, versioning, test, and documentation changes.
  Feature work and unrelated refactoring are out of scope.
- Keep WorldEdit and other third-party runtime mods out of the core upgrade
  path. They can be tested later with 26.2-compatible builds.
- Preserve `docs/upgrade-1.21.11-report.html` as a historical report.

## Target stack

The versions below match the official NeoForge 26.2 ModDevGradle MDK as of the
plan date.

| Component | Current | Target |
| --- | --- | --- |
| Minecraft Java Edition | `26.1` | `26.2` |
| Minecraft compatibility | `[26.1]` | `[26.2]` |
| NeoForge | `26.1.0.19-beta` | `26.2.0.87` |
| Java toolchain | `25` | `25` |
| ModDevGradle | `2.0.146` | `2.0.147` |
| Gradle wrapper | `9.2.1` | `9.2.1`, unless the MDK proves it incompatible |
| Mappings | Mojang names, no Parchment | unchanged |
| RedstoneWire version | `1.0.10` | `26.2-1.0.0` |

Primary references:

- [Minecraft Java Edition 26.2 changelog](https://feedback.minecraft.net/hc/en-us/articles/46690753273997-Minecraft-Java-Edition-26-2)
- [Official NeoForge 26.2 MDK properties](https://raw.githubusercontent.com/NeoForgeMDKs/MDK-26.2-ModDevGradle/main/gradle.properties)
- [Official NeoForge 26.2 MDK build script](https://raw.githubusercontent.com/NeoForgeMDKs/MDK-26.2-ModDevGradle/main/build.gradle)
- [NeoForge mod-file documentation](https://docs.neoforged.net/docs/gettingstarted/modfiles/)
- [NeoForge versioning documentation](https://docs.neoforged.net/docs/gettingstarted/versioning/)

Minecraft 26.2 raises the data-pack version to `107.1`, raises the resource-pack
version to `88.0`, and changes rendering to use a reversed depth buffer. The
custom cable renderer therefore remains a high-risk area even if Java compiles.

## Current resume checkpoint

Completed before the pause:

- Re-verified the 26.1 parent commit on Java 25.0.2.
- Regenerated correct 26.1 metadata for RedstoneWire `1.0.10`.
- Passed the clean 26.1 `compileJava` and `build` tasks.
- Discovered and passed all five required 26.1 GameTests.
- Started the isolated 26.1 dedicated server successfully; it loaded
  RedstoneWire `1.0.10` and reached `Done`.
- Started the isolated 26.1 client successfully; it loaded Minecraft 26.1,
  NeoForge, and RedstoneWire `1.0.10`, completed client setup, loaded resources
  and texture atlases, and initialized audio without a RedstoneWire error.
- Confirmed that `feat/upgrade-to-26.2` is based directly on the validated 26.1
  lineage.
- Created Backlog task `RW-11` with the required acceptance criteria.
- Froze the target stack at Minecraft `26.2`, NeoForge `26.2.0.87`,
  ModDevGradle `2.0.147`, and Java 25.
- Applied, but did not yet verify, the following working-tree changes:
  - Minecraft `26.1` → `26.2`
  - Minecraft range `[26.1]` → `[26.2]`
  - NeoForge `26.1.0.19-beta` → `26.2.0.87`
  - ModDevGradle `2.0.146` → `2.0.147`
  - RedstoneWire `1.0.10` → `26.2-1.0.0`
  - development directories → `run/<minecraft-version>/<run-type>`
- Updated the 26.1 plan with its final baseline-verification evidence.

Completed during the 2026-09-15 11:06-11:10 resume, on Java 25.0.4.1 at
`C:\Users\MichaelKargl\.jdks\temurin-25.0.4.1`:

- `compileJava` passes against Minecraft 26.2 / NeoForge 26.2.0.87, including a
  full `--rerun-tasks` pass, so no result is inherited from a 26.1 cache.
- The cable `RenderPipeline` port was checked member-by-member against the
  patched 26.2 jar: `BindGroupLayouts.SAMPLER2`, `DepthStencilState.DEFAULT`,
  `withBindGroupLayout`, `withVertexBinding`, `withPrimitiveTopology` and
  `withDepthStencilState` all exist with the used signatures.
- `build` produces `redstone_wire-26.2-1.0.0.jar`, the exact name `release.yml`
  validates.
- Generated and packaged `neoforge.mods.toml` both read version `26.2-1.0.0`,
  minecraft `[26.2]`, neoforge `[26.2.0.87,)`.
- `runGameTestServer`: all 5 required GameTests were discovered and passed.
- The isolated dedicated server loaded RedstoneWire `26.2-1.0.0` and reached
  `Done`.
- Backlog task `RW-11` is tracked in Git; the untracked-file note below is stale.

Still not completed:

- No graphical client has been launched on 26.2, so cable appearance under the
  reversed depth buffer is still unverified. This is the main open risk.
- No 26.1 world has been copied and opened on 26.2, so block, link, item and
  power-state migration is unverified.
- No optional/third-party mods have been tested against 26.2.
- The final JAR has not been installed into a real client or server instance.

Backlog task `RW-11` lives at
`backlog/tasks/rw-11 - Upgrade-RedstoneWire-to-Minecraft-26.2.md` and is now
tracked in Git. Use Backlog tooling to change its state rather than editing it
by hand.

No Minecraft client or server process remains from the checks. The only Java
process observed at the pause was the reusable Gradle daemon.

### Exact resume action

Keep the current working tree. Resume at the Step 4 verification gate, using
Java 25:

```powershell
$jdk25Path = 'C:\Users\MichaelKargl\.jdks\temurin-25.0.4.1'
$env:JAVA_HOME = $jdk25Path
$env:Path = "$jdk25Path\bin;$env:Path"
.\gradlew.bat generateModMetadata
```

Then inspect
`build/generated/sources/modMetadata/META-INF/neoforge.mods.toml`. Continue to
Step 5 only if it contains RedstoneWire `26.2-1.0.0`, Minecraft `[26.2]`, and
NeoForge `[26.2.0.87,)`. If dependency resolution or metadata generation fails,
record the first useful error and remain paused at Step 4.

## Versioning policy

### Format

Use this composite format from 26.2 onward:

```text
<minecraft-version>-<mod-semver>
```

For this release:

```text
26.2-1.0.0
│    └──── RedstoneWire semantic version within the 26.2 line
└───────── Exact supported Minecraft release
```

The whole composite string is not strict SemVer; the suffix `1.0.0` is the
SemVer portion. This is intentional and follows the combined version style used
as an example by NeoForge's mod-file documentation.

### Bump rules

| Change | Next version example |
| --- | --- |
| Bug fix while still targeting Minecraft 26.2 | `26.2-1.0.1` |
| Backward-compatible RedstoneWire feature for 26.2 | `26.2-1.1.0` |
| Breaking RedstoneWire change for 26.2 | `26.2-2.0.0` |
| First build for a Minecraft 26.2 hotfix | `26.2.1-1.0.0` |
| First build for the next Minecraft drop | `26.3-1.0.0` |

Do not retrospectively rename the existing `1.0.9` or `1.0.10` history. The new
scheme begins with the 26.2 release.

### Required consistency

For every release, all of these values must agree:

| Surface | 26.2 value |
| --- | --- |
| `minecraft_version` | `26.2` |
| `minecraft_version_range` | `[26.2]` |
| `mod_version` | `26.2-1.0.0` |
| Generated `neoforge.mods.toml` version | `26.2-1.0.0` |
| JAR name | `redstone_wire-26.2-1.0.0.jar` |
| Git tag | `v26.2-1.0.0` |
| GitHub release title | `v26.2-1.0.0` |
| Modrinth version number | `26.2-1.0.0` |
| Modrinth game version | Minecraft `26.2` |
| Loader | NeoForge |

`package.json` remains at its own package version. It exists only for repository
development tools and is not the Minecraft mod's release version.

## Execution protocol

Work through one numbered step at a time.

1. Run the stated verification before moving on.
2. Record the result in the progress log at the end of this document.
3. Continue automatically only when the step's gate is green.
4. If a pause condition occurs, stop with the tree in a coherent state and
   record the failing command, first useful error, affected files, and proposed
   next action.
5. Never hide a failure by disabling a RedstoneWire test or widening a version
   range.
6. Do not mix optional-mod failures into the RedstoneWire migration.
7. Do not test world upgrades against the only copy of a user world.

No release-triggering push or merge is allowed without a final explicit approval.

## Small-step implementation plan

### Step 1 — Re-establish the 26.1 baseline

**Goal:** prove that the starting commit is sound before attributing failures to
Minecraft 26.2.

- Confirm the worktree is clean and HEAD is the expected 26.1 commit.
- Set both the shell Gradle runtime and IntelliJ Gradle JVM to the installed Java
  25 JDK at `C:\Users\MichaelKargl\.jdks\temurin-25.0.4.1`.
- Stop any Gradle daemon that was started with Java 21, then confirm
  `gradlew --version` reports Java 25.
- Regenerate mod metadata so cached `1.0.9` output cannot be mistaken for the
  current `1.0.10` baseline.
- Run, separately and in this order:

  ```powershell
  .\gradlew.bat clean
  .\gradlew.bat generateModMetadata
  .\gradlew.bat compileJava
  .\gradlew.bat build
  .\gradlew.bat runGameTestServer
  ```

- Confirm all five currently expected required GameTests are discovered and
  pass.
- Launch the isolated 26.1 client once, verify that the mod list reports
  `1.0.10`, then exit normally.
- Start the isolated dedicated server, wait for a successful `Done` message, and
  stop it cleanly.
- Reconcile the stale status in `docs/upgrade-26.1-plan.md` with the evidence
  gathered here.

**Gate:** clean build, five passing GameTests, clean client startup, and clean
dedicated-server startup on 26.1.

**Pause if:** Java 21 is still used; metadata still says `1.0.9`; a RedstoneWire
error occurs; tests are missing; or the dedicated server loads client-only code.
Do not begin the 26.2 bump until the baseline issue is understood.

### Step 2 — Create an isolated 26.2 work branch and runtime

**Goal:** make the upgrade resumable without disturbing the validated 26.1
checkpoint.

- Create `feat/upgrade-to-26.2` from the validated `34636ac` lineage.
- Confirm the new branch has no unrelated changes.
- Change development run directories to be Minecraft-version-scoped, for
  example:

  ```text
  run/26.2/client
  run/26.2/server
  run/26.2/gametest
  run/26.2/data
  ```

- Do not delete or reuse the existing `run/` worlds or mods.
- Reload the Gradle project so IntelliJ regenerates run configurations with Java
  25 and the new directories.

**Gate:** the new branch is based on the validated 26.1 commit and all 26.2 run
directories are isolated.

**Pause if:** the worktree becomes dirty from unexpected IDE changes or the
branch base differs from the validated baseline.

### Step 3 — Reconfirm and freeze the 26.2 toolchain

**Goal:** avoid porting against a moving or pre-release dependency stack.

- Re-open the official 26.2 MDK immediately before editing build files.
- Confirm Minecraft `26.2` remains a stable release.
- Confirm the MDK still specifies a stable NeoForge build and ModDevGradle
  version.
- If the official MDK has moved past NeoForge `26.2.0.87` or ModDevGradle
  `2.0.147`, record the newer stable values in this plan before source edits.
- Reject alpha, beta, snapshot, pre-release, and release-candidate targets.
- Freeze the selected versions for the rest of this upgrade. Do not casually
  upgrade them midway through compiler fixes.

**Gate:** one recorded, mutually compatible, stable 26.2 stack.

**Pause if:** the official MDK is unavailable, only pre-release artifacts are
available, or its Java/Gradle requirements conflict with the installed tools.

### Step 4 — Apply only the platform and mod-version changes

**Goal:** make the smallest configuration change that asks Gradle for 26.2.

Update:

- `gradle.properties`
  - `minecraft_version=26.2`
  - `minecraft_version_range=[26.2]`
  - `neo_version=26.2.0.87`, or the frozen stable value from Step 3
  - `mod_version=26.2-1.0.0`
- `build.gradle`
  - ModDevGradle `2.0.147`, or the frozen value from Step 3
  - comments that incorrectly say 26.1
  - version-scoped run directories from Step 2
- Keep Java 25, Gradle 9.2.1, and the no-Parchment configuration unchanged
  unless the official MDK demonstrates a required change.

Do not edit production Java in this step.

**Verification:** run `generateModMetadata` only, then inspect its generated TOML
and confirm the exact Minecraft, NeoForge, and RedstoneWire versions.

**Gate:** Gradle configures successfully and generated metadata contains
`[26.2]` and `26.2-1.0.0`.

**Pause if:** dependency resolution fails, Gradle needs a different Java version,
metadata expansion is stale, or the selected NeoForge artifact does not match
Minecraft 26.2.

### Step 5 — Add release-version consistency checks

**Goal:** prevent a future release whose Minecraft prefix and metadata disagree.

- Teach `.github/workflows/release.yml` to read both `minecraft_version` and
  `mod_version`.
- Fail the release job unless `mod_version` matches the composite form
  `<minecraft_version>-<major>.<minor>.<patch>`.
- Keep the resulting tag as `v${mod_version}`.
- Confirm the release job expects the exact JAR name generated by Gradle.
- Document the bump rules in `docs/deployment.md`.
- Do not change `package.json` as part of the mod release.

**Gate:** `26.2-1.0.0` passes the check and deliberately mismatched sample values
fail it.

**Pause if:** the validation would reject legitimate future Minecraft hotfix
versions or the release workflow can select a stale JAR.

### Step 6 — Run the first 26.2 production compile

**Goal:** obtain a precise API-migration list without mixing in test or runtime
noise.

Run:

```powershell
.\gradlew.bat compileJava --stacktrace
```

- Save the complete first compiler output.
- Group errors by API area, such as registration, block behavior, block-entity
  persistence, data components, client setup, or rendering.
- For each group, inspect the generated 26.2 Minecraft sources and the NeoForge
  26.2 source/API before choosing a replacement.
- Fix one error group at a time and re-run `compileJava` after each group.
- Keep behavioral changes minimal and add comments only where a 26.2 API choice
  would otherwise be unclear.

High-risk source areas in this repository:

- `CableRenderer` and the three block-entity renderers
- block-entity extract/submit render states and light data
- `RedstoneWireClient` renderer/pipeline/config-screen registration
- `RedstoneWireBlockEntity` save/load and update packets
- link data components stored on redstone items
- redstone neighbor updates and reverse-connection cleanup
- deferred registration of blocks, items, block entities, and GameTests

**Gate:** production Java compiles with no warnings that indicate a broken or
deprecated compatibility path.

**Pause if:** a replacement changes save data, networking, redstone semantics, or
cable geometry; if official sources do not establish the intended replacement;
or if a fix would require a feature refactor.

### Step 7 — Build and inspect the packaged JAR

**Goal:** prove that compilation, resource processing, and packaging agree.

- Run `build` without GameTests first.
- Confirm the only release JAR is
  `build/libs/redstone_wire-26.2-1.0.0.jar`.
- Inspect the TOML inside the JAR, not only the generated source copy.
- Confirm:
  - mod version `26.2-1.0.0`
  - Minecraft dependency `[26.2]`
  - frozen NeoForge minimum
  - side `BOTH`
- Check the JAR for accidental development files, old generated metadata, and
  duplicate resource paths.

**Gate:** the packaged artifact has the expected name and exact metadata.

**Pause if:** multiple ambiguous JARs are produced, metadata differs from
`gradle.properties`, or a required asset is missing.

After this gate, make the first small green checkpoint commit containing the
toolchain bump and any required production-source migration.

### Step 8 — Audit 26.2 resources and data

**Goal:** catch issues Java compilation cannot see.

- Compare the project's resource layout with the official 26.2 MDK.
- Account for Minecraft 26.2 resource-pack version `88.0` and data-pack version
  `107.1`.
- The repository currently has no `pack.mcmeta`; do not add one merely because a
  number changed. Add or change pack metadata only if the 26.2 loader or MDK
  requires it.
- Validate all three blockstates, block models, item definitions, item models,
  textures, and translations.
- Validate every `test_instance` JSON and NBT structure fixture.
- Confirm no resource refers to renamed vanilla textures, shaders, or atlases.
- Run data generation only if the project actually has a provider that should
  produce tracked files; do not accept unrelated generated churn.

**Gate:** resources load without missing-model, missing-texture, shader,
data-pack, registry, or fixture errors.

**Pause if:** a schema conversion is required, existing worlds would need a data
fix, or generated output changes files unrelated to this port.

### Step 9 — Migrate and run GameTests

**Goal:** prove the redstone behavior survived the API update.

- Compile the registered test functions and helpers against 26.2.
- Fix test API changes separately from production logic.
- Run `runGameTestServer` in the new `run/26.2/gametest` directory.
- Require all five existing tests to be discovered. Zero discovered tests is a
  failure, even if Gradle exits successfully.
- Confirm coverage still exercises:
  - input power detection
  - output power transmission
  - linked connector behavior
  - reverse-link cleanup when a block is broken
  - structure/test-fixture composition
- If production behavior must change, add or update a regression test before the
  fix is accepted.
- Repeat once from a clean build to rule out stale generated metadata or classes.

**Gate:** all five required GameTests pass twice, including once from a clean
build.

**Pause if:** tests are not discovered, an NBT fixture silently upgrades, a test
passes only in isolation, or resolving a failure would weaken an assertion.

### Step 10 — Validate a clean dedicated server

**Goal:** verify the supported Minecraft server stated by the new version prefix.

- Start NeoForge/Minecraft 26.2 in `run/26.2/server` with no optional mods.
- Use a new disposable world first.
- Confirm RedstoneWire loads as `26.2-1.0.0` and the server reaches `Done` without
  client-class loading errors.
- Stop the server normally and start it a second time to verify persistence.
- Connect a clean 26.2 NeoForge client carrying the same JAR.
- Place input, connector, and output blocks; create a cable; toggle power; and
  confirm the server and client agree.

**Gate:** two clean server starts plus one successful client/server functional
smoke test.

**Pause if:** the server requires a client class, registry data changes between
starts, the network handshake rejects the JAR, or redstone state diverges.

### Step 11 — Validate world and item-data compatibility

**Goal:** ensure the upgrade does not destroy a user's existing RedstoneWire
network.

- Make a copy of a small 26.1 world containing:
  - all three block types and multiple facings
  - linked and unlinked connectors
  - powered and unpowered networks
  - a redstone item holding in-progress link data
- Open only the copy in 26.2.
- Confirm all blocks, block entities, connections, power levels, item data, and
  rendered cables survive.
- Save, close, reopen, and check again.
- Break one linked block and verify reverse connections are cleaned up.

**Gate:** no missing blocks, lost links, corrupted item data, or load-time data
errors after two 26.2 loads.

**Pause if:** any data is lost or rewritten incompatibly. Preserve the copied
world and logs for diagnosis; never retry against the original world.

### Step 12 — Perform the mandatory client visual pass

**Goal:** verify the custom rendering behavior affected by Minecraft 26.2's
renderer changes.

Test with the default OpenGL backend first:

- connector, input, and output models in world and inventory
- every horizontal/vertical facing and attachment point
- short, long, horizontal, vertical, and diagonal cables
- sag amount and the no-sag vertical case
- powered and unpowered networks
- lighting in bright, dark, indoor, and chunk-boundary situations
- camera movement around cables from all sides
- culling, transparency, Z-fighting, flicker, and reversed-depth artifacts
- block breaking and any expected crumbling-overlay behavior
- chunk unload/reload and save/reload
- link and unlink overlay messages

If the machine supports it, repeat a short cable smoke test with the experimental
Vulkan backend and record the result. Vulkan-specific failure is not automatically
a release blocker while Minecraft labels that backend experimental, but it must
not be misreported as an OpenGL or server failure.

**Gate:** no visual or functional regression under the default backend, with
screenshots or a short written result recorded.

**Pause if:** cables disappear, attach to the wrong face, flicker, Z-fight, have
incorrect light, crash the renderer, or differ materially from the 26.1 result.

### Step 13 — Test optional mods separately

**Goal:** keep the core release verdict independent from unrelated mod failures.

- Leave the existing disabled WorldEdit file disabled during Steps 1–12.
- Do not load the existing BetterF3 or Cloth Config jars from `run/mods`; they
  target Minecraft 1.21.1 and have already failed during a 26.1 launch.
- Only after the clean RedstoneWire client is green, optionally install versions
  explicitly published for Minecraft 26.2 and NeoForge into a separate run
  directory.
- Test WorldEdit, BetterF3, and Cloth Config one at a time before combining them.
- If an optional mod fails without RedstoneWire in the same profile, record it as
  that mod's compatibility issue and do not change RedstoneWire to mask it.

**Gate:** optional. RedstoneWire has no declared integration with these mods, so
their availability is not required for `26.2-1.0.0`.

**Pause only if:** RedstoneWire works alone but reproducibly fails when paired
with an otherwise working 26.2-compatible optional mod and that compatibility is
intended to be supported.

### Step 14 — Update documentation and run CI-equivalent checks

**Goal:** make the repository describe what the artifact actually supports.

- Update current-version references in `README.md`, `docs/README.md`,
  `docs/deployment.md`, `docs/testing.md`, `docs/rendering.md`, and any other
  living document found by a repository-wide search.
- Document the composite version scheme and bump examples.
- Update Java/NeoForge/Minecraft comments in Gradle and GitHub Actions where
  necessary.
- Keep historical report values untouched.
- Verify GitHub Actions still uses Java 25.
- Run the same clean build and GameTest commands used by CI.
- Search for stale live references to `26.1`, `1.0.10`, and the old NeoForge
  version; retain only deliberately historical references.
- Review `git diff` and ensure `run/`, build output, logs, worlds, IDE files, and
  downloaded optional mods are not included.

**Gate:** local CI-equivalent checks pass and living documentation consistently
describes Minecraft 26.2 / RedstoneWire `26.2-1.0.0`.

**Pause if:** local and CI commands differ materially, documentation would claim
an untested compatibility range, or unrelated user changes appear in the diff.

Create small green commits at natural boundaries, for example:

1. `chore: target Minecraft 26.2 and adopt scoped versioning`
2. `fix: port RedstoneWire APIs to Minecraft 26.2` if source fixes were needed
3. `test: validate RedstoneWire on Minecraft 26.2`
4. `docs: document the Minecraft 26.2 release`

Do not create a checkpoint commit while compilation or required tests are red.

### Step 15 — Assemble the release candidate and pause

**Goal:** produce a final artifact without publishing it.

- Run one final clean build and all five GameTests.
- Repeat the dedicated-server startup and default-backend client smoke test with
  the final JAR, not Gradle's development classpath.
- Confirm the SHA-256 checksum and record the artifact path.
- Confirm tag `v26.2-1.0.0` does not already exist locally or remotely.
- Confirm the release workflow will select exactly
  `redstone_wire-26.2-1.0.0.jar`.
- Fill in the progress log and release checklist below.

**Mandatory pause:** present the final diff, test evidence, remaining known
limitations, and artifact checksum for approval. Do not merge or push a
release-triggering commit yet.

### Step 16 — Publish only after approval

**Goal:** publish exactly the artifact that passed the release-candidate gate.

- Merge/push according to the repository's release process only after explicit
  approval, allowing GitHub Actions to create `v26.2-1.0.0`.
- Verify the GitHub release contains the expected JAR.
- Publish the same file to Modrinth with:
  - version number `26.2-1.0.0`
  - game version Minecraft `26.2`
  - loader NeoForge
  - client and server environment support
- Download the published artifact and perform a final metadata/checksum sanity
  check.

**Gate:** GitHub and Modrinth expose the same tested artifact and metadata.

**Pause if:** CI rebuilds a different checksum, the tag already exists, Modrinth
metadata differs, or either platform selects another JAR.

## Completion criteria

The upgrade is done only when every required item is true:

- [x] Minecraft is exactly `26.2` and the metadata range is `[26.2]`.
- [x] NeoForge and ModDevGradle match the frozen stable 26.2 MDK stack.
- [ ] Java 25 is used locally, by IntelliJ, and in CI.
- [ ] Mod version, metadata, JAR, tag, GitHub release, and Modrinth version all
      use `26.2-1.0.0`.
- [x] Production compilation and a clean build pass.
- [x] All five required GameTests are discovered and pass from a clean build.
- [ ] A clean dedicated server starts twice and accepts a matching client.
- [ ] A copied 26.1 world preserves blocks, links, item data, and power state.
- [ ] The OpenGL client visual checklist passes, including cable rendering.
- [x] No stale or incompatible optional mod influenced the core test result.
- [x] Living documentation and release automation describe the new versioning
      policy.
- [ ] The final JAR itself has been inspected and tested.
- [ ] Release approval was obtained before the release-triggering merge/push.

## Progress log

Update this table during implementation so work can stop and resume safely.

| Step | Status | Evidence / last result | Resume action |
| --- | --- | --- | --- |
| 1. Re-establish 26.1 baseline | Complete | Java 25; clean build; 5/5 GameTests; server and client startup passed on 2026-09-15 | None |
| 2. Isolate branch and runtime | Complete | Version-scoped `run/26.2/*` directories verified by the GameTest and server runs on 2026-09-15 | None |
| 3. Freeze stable stack | Complete | Frozen at NeoForge `26.2.0.87` / ModDevGradle `2.0.147` on 2026-09-15 | None |
| 4. Platform/version properties | Complete | Generated `neoforge.mods.toml`: version `26.2-1.0.0`, minecraft `[26.2]`, neoforge `[26.2.0.87,)` | None |
| 5. Release consistency checks | Complete | `release.yml` version-prefix guard added; JAR name matches its computed `jar_path` | None |
| 6. Production compile | Complete | `compileJava --rerun-tasks` clean on Java 25.0.4.1 against NeoForge 26.2.0.87 | None |
| 7. Package inspection | Complete | `redstone_wire-26.2-1.0.0.jar`: 29 classes, correct mods.toml, item/lang/test_instance assets present | None |
| 8. Resource/data audit | Complete | Item definitions, `en_us.json`, and four `test_instance` entries ship in the JAR; no recipes by design | None |
| 9. GameTests | Complete | `runGameTestServer`: 5/5 required tests passed in 681.9 ms on 2026-09-15 | None |
| 10. Dedicated server | Complete | Isolated server loaded RedstoneWire `26.2-1.0.0` and reached `Done (0.240s)` | Re-run once more before release for the "starts twice" criterion |
| 11. World/item migration | Not started | Requires a copied 26.1 world; not attempted | Copy a 26.1 world and open it on 26.2 |
| 12. Client visual pass | Not started | Blocked: requires a graphical client, which this session was not permitted to launch | Human must run `runClient` and inspect cable rendering |
| 13. Optional mods | Not started, optional | — | Wait for Step 12 |
| 14. Docs and CI parity | Complete | Workflows on JDK 25; `docs/deployment.md` documents the Minecraft-scoped version policy | None |
| 15. Release candidate | Not started | — | Wait for Step 14 |
| 16. Publish | Blocked by design | Requires explicit approval | Present release candidate first |

## Tracking note

Backlog task `RW-11` tracks this upgrade and references this document. As of
2026-09-15 its acceptance criteria #1 and #3 are met; #2 is met for the mod
version and JAR name but not for the Git tag or published release, and #4 is
met only on the server side. #5 and #6 remain open by design.
