# U-API

Foundational API library for the related Minecraft NeoForge mods in this ecosystem. It provides shared instance lifecycle, command registration, file-backed configuration, rewards, integrations and inventory-tab infrastructure used by dependent modules.

- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Java: 21
- Version: 1.2.0
- Mod ID: `u_api`

U-API configuration is stored under `config/uapi/u-api/` as `common.toml`, `client.toml` and `server.toml`. These files are created and loaded through NeoForge and are edited manually. U-API does not provide an in-game configuration editor.

Build on Windows with `gradlew.bat build`. The resulting artifact is `build/libs/u-api-1.2.0.jar`.
