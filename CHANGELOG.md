## 1.0.0-tnk-neoforge-1.21.1 — Release 1 candidate

- Align the complete 0.19.15 feature set and all original artwork with the 0.18.1 advancement-origin fix.
- Use the current TNK Progression API for mob/source levels, milestone ceilings and sealed rarity caps; verify both 1.2.0 and 1.3.0 companions.
- Rebuild all four Create Ponder tutorials around actual stations, readable captions and correct XP/item paths.
- Add shared equipment subgroups, source-level ranges, full operator item/attribute/affix export and acquired-gear discoveries without inflated identification counts.
- Correct invalid boss IDs, add Eye of the Storm/The Obliterator rewards, protect native spell components and reserved trial prizes, and prevent boss gear/duplicate cache rewards leaking into ordinary loot.
- Add 342 optional TNK rarity adapters for installed Iron's Apothic and Apothic Compats definitions, including source attribution/licences.
- Preserve earned advancement rewards, decorative items and custom native prizes through all TNK replacement paths.
- Respect the Beta pack catalogue removals so automatic imports and identification cannot restore disabled Artifacts or infinite-food equipment. Validate catalogue override/reload and retain valid policy on malformed input.
- Restore Gradle wrapper execute permissions after source recovery.
- Correct capped chance displays, legacy rarity lookups, source-level tooltips, dependency metadata, pinned build inputs and CI's real companion dependency.
- Build and 19 unit tests pass; focused runtime checks include 2,476 equipment/rarity applications, 49 boss pools, real advancement contexts, Liquid XP machine persistence and client Ponder replay.
- Full Beta 1.0 pack certification remains blocked by optional-integration/dimension startup failures. See docs/TNK-RELEASE-1-AUDIT.md for exact coverage and receipts.

## 0.18.1-tnk-beta.1 — Advancement reward protection

- Preserve advancement loot rewards, including their quantities and item components.
- Exclude advancement reward contexts from equipment replacement, accessory injection, boss routing and progression capping, even when they reuse chest loot tables.
- Stop automatically classifying decorative block items as equipment; explicit configured equipment entries remain supported.
- Keep ordinary chest/mob equipment conversion and the full beta feature set.

## 0.18.0-tnk-beta.1 — Full feature beta

- Restored the complete 0.17.0 feature set to the TNK Progression beta.
- Restored compact brown/parchment Identifier, Salvage, Preview and Collection screens.
- Restored all supplied rarity, NPC and journal textures, six rarities and optional tooltip styles.
- Restored boss groups, manual boss editing, universal boss crates and signature perks.
- Restored XP costs, salvage credit, Patchouli guide, discoveries and reward previews.
- Restored dimension-aware pools and optional compatibility presets.
- Restored optional Create Ponder tutorials on the Identifier, Journal and all sealed loot items.
- Retained level-weighted regular mob/chest rolls and progression rarity caps.
- Added old-beta rarity migration and protection against duplicate processing.
- Replaced the binary-overlay build chain with complete Gradle-compilable source.

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
