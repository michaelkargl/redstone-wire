# Deployment

## Cutting a release

1. Increase the mod version — `mod_version` in [gradle.properties](/gradle.properties)
2. Push to `master`

The CI/CD pipeline builds and publishes a new GitHub release.

3. Publish the release manually to [modrinth]

## What CI does

Two workflows in `.github/workflows/`:

| Workflow | Trigger | Does |
| --- | --- | --- |
| `build.yml` | every push and pull request | builds with Gradle, uploads the jar as an artifact, then runs the GameTests via `./run_tests.sh all` |
| `release.yml` | push to `master` | reads `mod_version` from `gradle.properties` and publishes a GitHub Release |

Both run on JDK 21 (Temurin).

> Branch names containing a slash (e.g. `release/mc-1.21.11`) are slugified before
> being used in artifact names — GitHub Actions rejects `/` in artifact names and
> has no string-replace expression function, so the workflows compute a `slug`
> output with bash parameter expansion.

## Installing a build manually

```bash
./gradlew build
cp build/libs/redstone-wire-*.jar /path/to/minecraft-client/mods/
cp build/libs/redstone-wire-*.jar /path/to/minecraft-server/mods/
```

The mod is needed on **both** client and server — the blocks and network logic are
server-side, the cable rendering is client-side.

[modrinth]: https://modrinth.com/mod/redstone-wire
