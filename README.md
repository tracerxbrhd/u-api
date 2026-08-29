# U-API

Foundational API library for the related Minecraft NeoForge mods in this ecosystem. It provides
scoped services, social and permission contracts, retained UI, HUD, world overlays,
bounded networking primitives, diagnostics, instance lifecycle and optional integrations.

- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Java: 21
- Version: 2.1.1
- Mod ID: `u_api`

U-API configuration is stored under `config/uapi/u-api/` as `common.toml`, `client.toml` and `server.toml`. These files are created and loaded through NeoForge and are edited manually. U-API does not provide an in-game configuration editor.

Version 2.1.1 extends the shared service, social, retained-UI, HUD, overlay, networking and
diagnostic foundation described in [`docs/API_2_FOUNDATION.md`](docs/API_2_FOUNDATION.md). The
deterministic 1.4 worldgen compatibility layer remains available; see
[`docs/WORLDGEN_INTEGRATION.md`](docs/WORLDGEN_INTEGRATION.md).

Optional JSON-driven inventory helper buttons are configured in
`config/uapi/u-api/sidebar_buttons.json`; see [`docs/SIDEBAR_BUTTONS.md`](docs/SIDEBAR_BUTTONS.md).
When Sophisticated Backpacks is installed, U-API also adds a native Backpack tab to the inventory;
the tab appears only for a backpack equipped in the chest armor, Curios or Accessories handlers,
updates live when equipment changes, and opens that exact slot. Backpacks carried in the regular
inventory are deliberately ignored, and the integration adds no required dependency.

Optional mods can exchange bounded, privacy-filtered public profile sections through the neutral
facet contract described in [`docs/PROFILE_FACETS.md`](docs/PROFILE_FACETS.md). Facets support
simple label/value rows and richer identity cards with namespaced IDs, translatable components,
defensive item icons and bounded metadata.

Build on Windows with `gradlew.bat build`. The resulting artifact is
`build/libs/u-api-2.1.1+mc1.21.1.jar`.
