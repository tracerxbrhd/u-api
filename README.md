# U-API

Foundational API library for the related Minecraft NeoForge mods in this ecosystem. It provides shared instance lifecycle, command registration, file-backed configuration, rewards, integrations and inventory-tab infrastructure used by dependent modules.

- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Java: 21
- Version: 1.3.1
- Mod ID: `u_api`

U-API configuration is stored under `config/uapi/u-api/` as `common.toml`, `client.toml` and `server.toml`. These files are created and loaded through NeoForge and are edited manually. U-API does not provide an in-game configuration editor.

Version 1.3.1 adds optional JSON-driven inventory helper buttons for common server commands and addon-defined actions. The sidebar file is generated at `config/uapi/u-api/sidebar_buttons.json`; see [`docs/SIDEBAR_BUTTONS.md`](docs/SIDEBAR_BUTTONS.md).

Build on Windows with `gradlew.bat build`. The resulting artifact is `build/libs/u-api-1.3.1+mc1.21.1.jar`.
