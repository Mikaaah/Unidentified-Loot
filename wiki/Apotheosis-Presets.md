[← Guide home](https://github.com/Mikaaah/Unidentified-Loot/wiki) · [Project](https://github.com/Mikaaah/Unidentified-Loot) · [Get help](https://github.com/Mikaaah/Unidentified-Loot/issues)

# Apotheosis presets

Guild rank selects an Apotheosis rarity. These are real Apotheosis affixes and components, including their attribute modifiers, sockets, effects, names and tooltip support.

| Guild rank | Rarity | Stat rolls | Extra rolls | Sockets |
| --- | --- | --- | --- | --- |
| 0 Novice | Common | 1 | — | 0 |
| 1 Apprentice | Uncommon | 2 | — | 0 |
| 2 Adventurer | Rare | 2 | 1 effect | 0–2 |
| 3 Explorer | Epic | 3 | 1 effect | 0–2 |
| 4 Veteran | Legendary | 3 | 1 effect + 1 ability | 1–2 |
| 5 Champion / 6 Hero | Mythic | 4 | 1 effect + 1 ability | 1–3 |
| 7 Legendary | Ancient | 5 | 1 effect + 1 ability | 1–3 |

Rolls select distinct eligible affixes. A rule is skipped when its type has no remaining compatible affix for the item category, so e.g. a bow with four eligible stat affixes gets four rather than inventing a fifth. Effects and abilities only appear where supported. At least one actual affix is required for every identified item; failure consumes nothing.

## Included pools

- Melee: attack damage, attack speed, armor penetration and life steal; bleeding and execution at higher rarities.
- Armor: armor, toughness, health, movement speed and knockback resistance; healing when hurt at higher rarities.
- Ranged: draw speed, arrow damage and arrow velocity; sundering effects and magical arrows at higher rarities.
- Tools: mining speed, experience and reach; haste and radial mining at higher rarities.
- All supported categories can also roll luck.

The affixes are registered under `tnkloot:`, with a category/type path such as `tnkloot:melee/attribute/violent`. Stats start at 30% of the baseline values at Common and scale up through 45%, 60%, 75%, 90%, 100% and 115%. The seven rarities have zero natural-loot weight and unique salvage materials, so they do not replace Apotheosis' world-loot progression.

## Customize

1. Map any Guild tier to a registered rarity in `config/tnkloot/tiers.json`, or cycle its group in `/tnkloot editor`.
2. Override `data/tnkloot/rarities/<rarity>.json` in a datapack to change stat/effect/ability roll counts, sockets and durability.
3. Override `data/tnkloot/affixes/<category>/<type>/<name>.json` to change rarity-specific values, categories, weights or exclusions. Preserve the `tnkloot:<rarity>` keys: upstream affixes generally only define `apotheosis:<rarity>` values and cannot be applied to a custom rarity without matching values.
4. Use Minecraft's `/reload` for datapack affix/rarity changes; use `/tnkloot reload` for `tiers.json`.

Existing configuration files are never overwritten. The improved armor/tool pool ranges only become defaults for newly generated configs; copy the example deliberately to adopt them in an existing pack.

Preset schemas and data are adapted from Apotheosis under its MIT license, included in `META-INF/LICENSE-Apotheosis.txt`.
