# Container helper buttons

U-API can render small helper buttons in the top-left corner of container screens: vanilla inventory, creative inventory, Curios/Trinkets-style inventory containers, workstations and modded container blocks. The feature is client-side UI only: button presses send normal server commands, so the server still controls permissions.

The helper buttons are intentionally not rendered on ordinary custom screens such as character/progression UIs, where they could overlap custom layouts.

When JEI is installed, U-API shifts the helper button panel below JEI's top-left bookmark controls and also registers the panel as a JEI extra GUI area. JEI does not move every bookmark element away from extra areas, so U-API performs the top-left offset itself.

The file is generated on first client load:

```text
config/uapi/u-api/sidebar_buttons.json
```

Default buttons:

- survival mode: `gamemode survival`
- creative mode: `gamemode creative`
- clear weather: `weather clear`
- rain: `weather rain`
- morning: `time set day`
- night: `time set night`

Players without permission to run these commands will see the normal server-side command failure.

## JSON format

```json
{
  "format_version": 2,
  "enabled": true,
  "columns": 2,
  "buttons": [
    {
      "id": "example:home",
      "order": 100,
      "title": "Go home",
      "item": "minecraft:compass",
      "command": "home",
      "permission_level": 0,
      "enabled": true
    },
    {
      "id": "example:kit",
      "order": 110,
      "title": "Starter kit",
      "item": "minecraft:chest",
      "commands": [
        "kit starter",
        "tellraw @s {\"text\":\"Requested starter kit\"}"
      ],
      "permission_level": 0,
      "enabled": true
    }
  ]
}
```

Fields:

- `format_version` is required and must equal `2`. Older or unversioned files are not loaded; delete
  them to generate a clean current file.
- `enabled` at root disables the whole sidebar.
- `columns` controls button columns. Values are clamped to `1..6`.
- `id` is used only for stable sorting/debugging.
- `order` controls placement.
- `title` is the tooltip text.
- `item` is a Minecraft item ID used as the icon.
- `command` is a single command without requiring `/`.
- `commands` is an optional array of commands executed in order.
- `permission_level` is required and must be a vanilla permission level from `0..4`. Use `0` for
  normal player commands such as `home`, `spawn` or `kit`; built-in administrative buttons use `2`.
- `enabled` on a button disables only that button.

Malformed or missing item IDs render as a barrier icon. Invalid JSON disables the sidebar and logs a warning.
