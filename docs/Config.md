# Config & Customization

The mod reads its config from:

```
<minecraft_folder>/config/craftableenchantement.json
```

The file is created automatically with empty defaults on first launch.

---

## Config format

```json
{
  "disabled_enchantments": [],
  "custom_recipes": []
}
```

---

## Disabling a built-in recipe

Add the enchantment's resource location to `disabled_enchantments`.  
All levels of that enchantment will be removed.

```json
{
  "disabled_enchantments": [
    "minecraft:mending",
    "minecraft:silk_touch"
  ],
  "custom_recipes": []
}
```

After editing: restart the server or run `/reload`.

---

## Adding a custom recipe

Use `custom_recipes` to add recipes for any enchantment — including enchantments from other mods.

Each entry requires:

| Field | Type | Description |
|---|---|---|
| `enchantment` | String | Registry ID of the enchantment |
| `level` | Integer | Enchantment level (= number of XP bottles) |
| `ingredient` | String | Registry ID of the thematic item |

### Example — add a recipe for a modded enchantment

```json
{
  "disabled_enchantments": [],
  "custom_recipes": [
    {
      "enchantment": "mymod:super_enchant",
      "level": 3,
      "ingredient": "minecraft:diamond"
    }
  ]
}
```

Result: crafting **3 Experience Bottles + Book + Diamond** gives the modded enchanted book at level 3.

### Replacing an existing recipe's ingredient

Disable the original, then re-add it with a different item:

```json
{
  "disabled_enchantments": ["minecraft:mending"],
  "custom_recipes": [
    {
      "enchantment": "minecraft:mending",
      "level": 1,
      "ingredient": "minecraft:diamond"
    }
  ]
}
```

---

## Notes

- Invalid enchantment or item IDs are **silently skipped** (check server logs for warnings).
- The config is reloaded on every `/reload` — no need to restart.
- The config is **server-side only**; clients do not need it.
