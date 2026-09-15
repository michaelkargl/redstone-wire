# Deployment

## Cutting a release

1. Set `minecraft_version`, `minecraft_version_range`, and `mod_version` in
   [gradle.properties](/gradle.properties). The mod version must use the exact
   Minecraft version as its prefix, for example `26.2-1.0.0`.
2. Build and test the exact JAR locally.
3. Push to `master` only after release approval.

The CI/CD pipeline builds and publishes a new GitHub release.

4. Publish the same JAR manually to [modrinth].

## Version format

RedstoneWire releases use `<minecraft-version>-<mod-semver>`:

| Change | Example |
| --- | --- |
| Bug fix for Minecraft 26.2 | `26.2-1.0.1` |
| Backward-compatible feature for Minecraft 26.2 | `26.2-1.1.0` |
| Breaking mod change for Minecraft 26.2 | `26.2-2.0.0` |
| First release for a Minecraft hotfix | `26.2.1-1.0.0` |
| First release for the next Minecraft version | `26.3-1.0.0` |

The release workflow rejects a `mod_version` whose Minecraft prefix differs
from `minecraft_version`, or whose suffix is not a three-component semantic
version. `package.json` has an independent development-tool version and is not
changed for mod releases.

## What CI does

Two workflows in `.github/workflows/`:

| Workflow | Trigger | Does |
| --- | --- | --- |
| `build.yml` | every push and pull request | builds with Gradle, uploads the jar as an artifact, then runs the GameTests via `./run_tests.sh all` |
| `release.yml` | push to `master` | validates the Minecraft-scoped version, performs a clean build, and publishes the exact versioned JAR |

Both run on JDK 25 (Temurin).

> Branch names containing a slash (e.g. `release/mc-26.2`) are slugified before
> being used in artifact names — GitHub Actions rejects `/` in artifact names and
> has no string-replace expression function, so the workflows compute a `slug`
> output with bash parameter expansion.

## Installing a build manually

```bash
./gradlew build
cp build/libs/redstone_wire-26.2-1.0.0.jar /path/to/minecraft-client/mods/
cp build/libs/redstone_wire-26.2-1.0.0.jar /path/to/minecraft-server/mods/
```

The mod is needed on **both** client and server — the blocks and network logic are
server-side, the cable rendering is client-side.

[modrinth]: https://modrinth.com/mod/redstone-wire
