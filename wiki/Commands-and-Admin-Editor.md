> **Standalone 0.6.1-alpha reference.** For TNK's Progression-based levels, XP costs, salvage and six-rarity integration, use the [TNK edition guide](https://github.com/Mikaaah/Unidentified-Loot/wiki/TNK-Edition).

[← Guide home](https://github.com/Mikaaah/Unidentified-Loot/wiki) · [Project](https://github.com/Mikaaah/Unidentified-Loot) · [Get help](https://github.com/Mikaaah/Unidentified-Loot/issues)

# Commands and Admin Editor

## Commands

| Command | Permission | Purpose |
| --- | --- | --- |
| `/tnkloot tier` | Everyone | Show your effective tier |
| `/tnkloot tier list` | Everyone | List configured tier IDs and ranks |
| `/tnkloot tier unlock <players> <id or rank>` | Operator level 2 | Grant a tier, without lowering existing progress |
| `/tnkloot tier reset <players>` | Operator level 2 | Clear selected personal progress and their current shared team progress |
| `/tnkloot unidentified <players> [tier] [category]` | Operator level 2 | Award a category token; categories: weapon, bow, helmet, chestplate, leggings, boots, tool, accessory |
| `/tnkloot reload` | Operator level 2 | Validate and atomically reload settings |
| `/tnkloot validate` | Operator level 2 | Report missing registry items and empty live pools |

Use an explicit player name when running from the server console. Unidentified items awarded through ordinary `/give` or an Item Reward have no tier data and identify at tier 0. Use the mod's award command for tier-aware rewards. `/tnkloot unidentified @s explorer helmet` gives a tier-specific helmet token.


## In-game editor

Operators can use `/tnkloot editor` to navigate ranks, cycle Apotheosis rarities, add or remove held equipment and create tiers. Changes are validated and saved immediately. Edit tier IDs, display names and advancement unlocks in `config/tnkloot/tiers.json`, then run `/tnkloot reload`.
