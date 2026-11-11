# One equipment rarity system

TNK Edition 1.0.2 makes Common, Uncommon, Rare, Epic, Legendary and Mythic the active equipment rarity groups used by both Unidentified Loot and Apotheosis. The native five IDs remain compatibility aliases for saved items, data references and invader definitions. Generated equipment selects the UL affix definitions and uses the same tier-bounded values as identified loot.

## Crafted equipment

Craft the item normally. Put that exact item into an Apotheosis reforging table, supply the rarity material and Sigils of Rebirth where required, then pay the normal vanilla XP cost. Names, damage, enchantments and native item components remain attached to the item. Crafted equipment does not become a random armour piece. The material tier limits affix strength and TNK Progression still limits the available rarity.

| UL group | Reforging/salvaging material | Base material cost | Sigils | Level requirement | Table |
| --- | --- | ---: | ---: | ---: | --- |
| Common | Mysterious Scrap Metal | 1 | 0 | 2 | Simple or upgraded |
| Uncommon | Timeworn Fabric | 2 | 1 | 5 | Simple or upgraded |
| Rare | Luminous Crystal Shard | 2 | 2 | 15 | Simple or upgraded |
| Epic | Arcane Sands | 2 | 4 | 30 | Upgraded |
| Legendary | Echo Shard | 3 | 4 | 40 | Upgraded |
| Mythic | Godforged Pearl | 3 | 5 | 50 | Upgraded |

These are the first-choice base costs. Apotheosis multiplies costs for the other two choices. A rarity above the player's current TNK milestone produces no selectable result and consumes no input. High-tier crafting materials do not bypass that cap. Existing native equipment can be reforged through the same route.

All six groups also salvage through the native Apotheosis station. The normal native recipe yields 1–4 of the corresponding material; Legendary yields Echo Shards. Canonical comparison keeps older saved native rarity IDs compatible with these ingredients.

## Apotheosis invaders and attribute boosts

Native invader definitions use the UL rarity aliases, with the extra Legendary group supported. The native equipment-generation path uses UL affixes. The additional invader affix boost, augmentation upgrades, rerolls and malice boosts cannot increase a UL affix beyond its equipment tier's configured upper bound. The augmenting button stops offering an upgrade at that bound.

Eligible invader equipment drops become sealed UL rewards containing the exact native prize. Identification keeps its item, enchantments and native components. An excluded gear-set prize is replaced with eligible UL equipment before the invader affixes and enchants it. Once identified, the prize is not sealed again.

Default random native loot, explicit pools, global modifier entries and wandering trader offers respect enabled UL items, source-level eligibility, dimensions and boss-only exclusions. Owned-item reforging retains the exact earned/crafted item. An empty explicit pool gives no gear and preserves other rewards.

Ordinary chest generation recognizes the actual CHEST evaluation source, including nonstandard Twilight Forest table paths. Automation without a player uses a sealed level-1/Common roll at the configured chest frequency. Transferring that token does not improve it. Advancement-origin protection takes precedence, including when an advancement reuses a chest table. Fixed native payloads, materials, keys, trophies and the pack's End Remastered eye drops keep their existing sources.

Gems, their purities and enchantments remain their own Apotheosis systems. This update unifies equipment rarity and affixes; it does not replace gem definitions or erase installed gems.

## World tier

`config/tnkloot/apotheosis-integration.json` defaults to:

```json
{"force_pinnacle": true}
```

Every player automatically uses **Pinnacle**, Apotheosis's highest world tier, including on join, respawn and dimension transfer. The separate Apotheosis advancement gate and manual tier selection are bypassed while this setting is enabled. It does not change Minecraft's difficulty setting or remove TNK Progression's own level and rarity caps. Set `force_pinnacle` to false and reload to return control to Apotheosis.

## Compatibility and verification

Target: Minecraft 1.21.1, NeoForge 21.1.262, Java 21, Apotheosis 8.9.0 and the real TNK Progression companion. The existing Identifier, Salvage, Collection and Ponder artwork is retained. See the release verification receipts for actual build/test results; the earlier full-pack dimension startup blockers are a separate outstanding validation limit.
