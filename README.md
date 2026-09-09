![Redstone Wire! Redstone in, Redstone out! Easy!](images/redstone-wire-text.png)

This mod provides a means to transfer redstone charges over
a handing wire. Nothing more, nothing less.

![wires at midnight](/images/wires-at-midnight.png)

Project Management
=======

> To report issues, please use the "Issues" tab above

This project uses [Backlog.md] and [OpenSpec] for managing the project development.

```pwsh
npm install
npx openspec init

npx backlog board view
npx backlog browser
```

[.vscode/extensions.json]: ./.vscode/extensions.json
[OpenSpec]: openspec/README.md
[Backlog.md]: backlog/README.md

Documentation
=============

All documentation lives in [`docs/`](docs/README.md), which has a reading order
for anyone new to NeoForge modding.

| Document | Covers |
| --- | --- |
| [NeoForge Concepts](docs/neoforge-concepts.md) | Modding from scratch, with C#/F# analogies |
| [Usage Guide](docs/usage.md) | Using the blocks in-game |
| [Architecture](docs/architecture.md) | How the mod is built |
| [Rendering](docs/rendering.md) | Block models and cable rendering |
| [Textures](docs/textures.md) | Texture layout and resource packs |
| [Testing](docs/testing.md) | Writing and running GameTests |
| [Deployment](docs/deployment.md) | Cutting a release |

Configuration
=============

> **Not currently wired up.** The config screen factory is registered, but
> `registerConfig` is commented out in `RedstoneWire`, so no config file is
> generated and no values are read. The translation keys in `en_us.json` are
> groundwork for a later change.

Running
=======

This mod uses IntelliJ IDEA for development. Two run configurations are provided: one for the client and one for the server. You can run either configuration to start Minecraft with the mod loaded.

> **Note:** It is recommended to use Singleplayer for development, however server-side testing is also supported.
>           Sometimes, however, it is necessary to rebuild the mod jar after making changes.
>           `reload_gradle.sh`

Usage
=====

1. Build the mod jar => `reload_gradle.sh`
2. copy the mod jar into your server **and** client `mods` folder
3. cp build/libs/redstone-wire-*.jar /path/to/minecraft-client/mods/
4. cp build/libs/redstone-wire-*.jar /path/to/minecraft-server/mods/