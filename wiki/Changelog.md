[← Guide home](https://github.com/Mikaaah/Unidentified-Loot/wiki) · [Project](https://github.com/Mikaaah/Unidentified-Loot) · [Get help](https://github.com/Mikaaah/Unidentified-Loot/issues)

# Changelog

## 0.6.1-alpha

- Add the T&K Loot Creative inventory tab containing the Identifier block and all eight unidentified item types.
- Creative tokens include valid tier-0 identification data.


## 0.6.0-alpha

- Replace inventory-search identification with a vanilla-style one-row counter: seven token slots and a cost-confirmation button.
- Reveal each paid item through all eligible base-item previews, with slowing note-block ticks and a reveal chime.
- Prepare batches atomically, lock inventory operations while rolling, and return final results on close/disconnect/death.
- Remove handheld right-click identification and its admin-button bypass.
- Protect tagged identifier villagers from creative and survival attacks, repair legacy attendants, fix counter-relative placement/facing, and add arrival/departure particles.
- Replace reflective Apotheosis integration with its required typed API, deterministic affix rolls, and failure-before-payment behavior.
- Supply 23 real Apotheosis affix definitions with values for all custom rarities and seven graduated rarity presets.
- Improve default armor/tool material ranges, preserving existing configs.
- Add live dedicated-server checks for tier/category affixes, batch payment and close, and villager lifecycle.

## 0.5.0-alpha

- Add identifier and operator menus, configurable rarity mappings and tier/pool editing.


## 0.4.0-alpha

- Add separate unidentified tokens for weapons, bows, helmets, chestplates, leggings, boots, tools and accessories.
- Convert configured item IDs in vanilla and modded loot tables to their matching tier-stamped category token.
- Snapshot rank, category, source item, source type and a stable base-item roll seed on each generated token.
- Add configurable emerald identification prices with per-tier defaults and per-item overrides.
- Add the craftable Identifier block with a stationary, invulnerable villager attendant.
- Add category-aware `/tnkloot unidentified` awards and pool validation.
- Make Apotheosis 1.21.1-8.9.0 and its Placebo, Patchouli, Apothic Attributes, Spawners and Enchanting dependencies required.
- Update configuration examples, documentation and placeholder item models/localizations.

## 0.3.0-alpha

- Add custom Common-to-Ancient Apotheosis rarities mapped from Guild tiers.
- Add optional Apotheosis affixes and configured weapon-loot replacement.
- Preserve original weapon item IDs and existing weapon moveset behavior.

## 0.2.0-alpha

- Add persistent player and optional FTB-team progression, commands, and JSON-driven tiers and weapon pools.
