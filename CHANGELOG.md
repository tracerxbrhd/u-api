# Changelog

## 1.3.1 - 2026-07-10

- Added JSON-driven container helper buttons in the fixed top-left screen corner.
- Added built-in helper actions for gamemode survival/creative, weather clear/rain, morning and night.
- Limited helper rendering to container screens so custom non-container UIs do not get overlapped.
- Added optional JEI compatibility: helper buttons are offset below JEI's top-left bookmark controls and registered as a JEI extra GUI area.
- Added per-button `permission_level` checks for server-safe helper commands.
- Narrowed declared Minecraft/NeoForge support to the tested 1.21.1 / 21.1.x line.

## 1.3.0 - 2026-07-06

- Breaking: extended shared inventory tabs with synchronized visibility predicates.
- Kept the API neutral so addon mods can hide server-disabled screens without U-API knowing their gameplay rules.

## 1.2.0 - 2026-07-05

- Added lifecycle-safe runtime configuration snapshots so optional integration discovery never reads NeoForge config values before they are loaded.
- Standardized manually edited Common, Client and Server configuration files under `config/uapi/u-api/`.
- Removed the experimental in-game configuration editor, screen registry and raw text editor.
- Kept Common, Client and Server configuration strictly file-based.

## 1.1.0 - 2026-07-04

- Added the optional accessory integration service and a reflection-safe Curios provider.
- Preserved default item data components when ecosystem creative tabs are populated.

## 1.0.1 - 2026-07-04

- Changed the project and packaged artifact license to All Rights Reserved.
- Added the license notice to `META-INF/LICENSE` in release JARs.

## 1.0.0

- Initial public release.
