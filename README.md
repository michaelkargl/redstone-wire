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

Configuration
=============

This mod uses NeoForge's configuration system to provide runtime customization. After running Minecraft with this mod for the first time, a configuration file will be automatically generated.

**Configuration File Location:**
`config/redstone-wire-common.toml`

### Editing the Configuration

1. Stop Minecraft if it's running
2. Open `config/redstone-wire-common.toml` in any text editor
3. Modify values (validation ranges are enforced)
4. Save the file
5. Restart Minecraft for changes to take effect

Testing
=======

Refer to the [Testing Guide](docs/testing.md) for more information.

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