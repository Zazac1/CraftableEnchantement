# Craftable Enchantement

A Fabric mod for Minecraft 1.21.11 that lets you craft any enchanted book directly in a crafting table.

## Features

- 📖 Craft **113 enchanted books** covering every vanilla enchantment
- 🧪 Each recipe uses **N XP bottles** (one per level) + a **book** + a **thematic ingredient**
- ⚖️ Shapeless recipes — items can be placed in any slot
- 🗡️ Weapons, armor, tools, bow, crossbow, trident, fishing rod and curses are all supported
- 🔨 No custom items, no config — pure data-pack approach, fully vanilla-compatible
- 🌐 Works in singleplayer and multiplayer (server-side mod)

## Crafting Logic

| Slot content | Purpose |
|---|---|
| N × Experience Bottle | Sets the enchantment **level** (1–5 bottles) |
| 1 × Book | Base material |
| 1 × Thematic item | Identifies the **enchantment type** |

Example — Fire Aspect II: `2 × Experience Bottle + Book + Flint and Steel`

## Build & Installation

```powershell
.\gradlew.bat build
```

Place the JAR from `build\libs\` into your Fabric `mods` folder.

Dev launch:

```powershell
.\gradlew.bat runClient
```

## Compatibility

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.141.4+1.21.11 |
| ModMenu | 17.0.0 (opt.) |

## License & Distribution

- Code: MIT (see `LICENSE` file)
- Inclusion in modpacks is allowed — please credit Zazac1

## Links

- 🐛 Issues: https://github.com/Zazac1/CraftableEnchantement/issues
- 💻 Source: https://github.com/Zazac1/CraftableEnchantement
