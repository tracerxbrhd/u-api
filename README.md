# U-API

Foundational API library for the related Minecraft NeoForge mods in this ecosystem. It provides
scoped services, social and permission contracts, retained UI, HUD, world overlays,
bounded networking primitives, diagnostics, instance lifecycle and optional integrations.

- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Java: 21
- Version: 2.0.0
- Mod ID: `u_api`

U-API configuration is stored under `config/uapi/u-api/` as `common.toml`, `client.toml` and `server.toml`. These files are created and loaded through NeoForge and are edited manually. U-API does not provide an in-game configuration editor.

Version 2.0.0 adds the shared service, social, retained-UI, HUD, overlay, networking and
diagnostic foundation described in [`docs/API_2_FOUNDATION.md`](docs/API_2_FOUNDATION.md). The
deterministic 1.4 worldgen compatibility layer remains available; see
[`docs/WORLDGEN_INTEGRATION.md`](docs/WORLDGEN_INTEGRATION.md).

Optional JSON-driven inventory helper buttons are configured in
`config/uapi/u-api/sidebar_buttons.json`; see [`docs/SIDEBAR_BUTTONS.md`](docs/SIDEBAR_BUTTONS.md).

Optional mods can exchange bounded, privacy-filtered public profile sections through the neutral
facet contract described in [`docs/PROFILE_FACETS.md`](docs/PROFILE_FACETS.md).

Build on Windows with `gradlew.bat build`. The resulting artifact is
`build/libs/u-api-2.0.0+mc1.21.1.jar`.
