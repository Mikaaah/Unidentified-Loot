# 0.18.0-tnk-beta.1

Restores the full 0.17.0 feature set to the TNK Progression beta. The earlier
0.6.2 beta fork was based on an older release. This build combines the full
Identifier, salvage, preview and collection screens; XP payments and salvage
credit; Patchouli guide; supplied journal/NPC/rarity textures; all six rarities;
optional tooltip styling; boss groups, manual boss creation and universal boss
crates; boss signatures; dimension routing and optional compatibility imports;
late Apotheosis equipment conversion; and optional Create Ponder tutorials.

## Progression rules

TNK Progression remains required. Regular hostile equipment and chest equipment
keep their separate 2% rolls and `tnk-beta.json` settings. Equipment tiers follow
the existing level probability bands, using source level bounded by the receiving
player's milestone ceiling. Category selection includes every configured and
available category, including tools and accessories, with dimension-aware pools.

Explicit boss pools and Apotheosis invader rewards retain their full-featured
conversion path, boss identity and boss rarity settings. Their generated rarity
is restricted to the recipient's milestone cap. Dungeon accessory bonuses remain
configurable in `features.json`. Dimension equipment pools remain bound to the
drop's origin. Repeated processing cannot duplicate or reroll a sealed reward.

Generation, rather than ownership or later identification, applies progression
caps. Trading and milestone changes do not reroll existing tokens. Earlier-beta
namespaced rarity IDs are migrated to the full rarity system, including texture,
tooltip and affix metadata. Existing configured pools are not overwritten.

## Build

verified Create/Ponder compile inputs with `tools/prepare_ponder.py`; these are
not bundled into the mod. Supply the separate companion JAR when running tests:



The complete recovered sources are under `src/main/java/`. This replaces the
old binary-overlay packaging chain with a single Gradle compilation. Create is
optional at runtime; Patchouli and the declared Apotheosis stack are required.
Install the same loot JAR on the client and server.

Opt-in dedicated-server suites: `tnkloot.betaSmokeTest`,
`tnkloot.raritySmokeTest`, `tnkloot.marketSmokeTest`,
`tnkloot.workshopSmokeTest`, and `tnkloot.featuresSmokeTest`.
Use only a disposable world for these destructive test fixtures.
