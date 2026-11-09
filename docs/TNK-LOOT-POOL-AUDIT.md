# TNK 1.0.2 loot pool audit

Audit baseline: release/tnk-1.0.1, commit 7122c4d46ded1eddd0822a962d0c4fba44313fdb. Source/data audit completed before the changes on release/tnk-1.0.2.

## Inputs and coverage

| Input | Static inspection |
| --- | ---: |
| Pinned Beta 1.0 manifest JARs, SHA-256 verified | 315 |
| Other override JARs, replacing the old Loot JAR | 2 |
| Loot table resources, including Minecraft and explicit overrides | 21,870 |
| Unique loot table IDs | 21,815 |
| Global loot modifier resources | 409 |
| Native affix item entry resources | 488 |
| Native invader resources | 238 |
| Gear-set resources | 112 |
| Rarity overrides | 31 |
| Advancement references to loot tables | 28 |
| Configured TNK boss pools | 49 |

The Full KubeJS input was version 76, SHA-256 f39d9e567697028f06e06f3002e2a74614d6f2e50f7ac2e8e545605096114eb0. Physical duplicate IDs, conditional optional resources and obsolete plural `loot_tables` resources are recorded separately. These counts describe inspected data, not 21,870 executed Minecraft tests.

## Findings and changes

| Finding before changes | Resulting behavior |
| --- | --- |
| Native salvage uses rarity-holder identity; UL aliases only changed the bound value. Legendary had no salvage recipe. | Compare canonical rarities; all six materials have a native salvage route, including Echo Shards. Legacy saved IDs remain accepted. |
| Chest equipment was removed with no replacement opportunity when the player was absent. | Use the same frequency and seal a level-1/Common reward for automation. |
| Generic ordinary replacement raised a token to the input base's minimum tier and left ownerless tokens unsealed. | Bound ordinary replacement by the source tier and seal ownerless ordinary loot at level one. Deliberate fixed-tier boss prizes retain their rules. |
| `chests/` naming missed actual chest evaluations such as Twilight caches. | Carry actual CHEST origin alongside ADVANCEMENT_REWARD. Reward protection takes precedence. |
| Native item selection bypassed UL configuration. There were 58 resource definitions, covering 40 unique entry IDs, for TNK boss-reserved prizes. | Default and explicit native selection use UL eligibility. Named entry pools retain their native weights/constraints after filtering. |
| Explicit modifiers/trades could have no valid weighted entry, and trader offers bypassed the final loot-table hook. | Empty named pools return no gear safely. Both trader types filter their named choices and retain native pricing. |
| Native invader gear sets could provide excluded prizes. | Retain an eligible exact prize; choose a valid UL base for an excluded prize before native enchanting/affixing. |
| Random magic/accessory payloads could resemble protected fixed rewards. | Mark random pool entries so ordinary chest sealing catches them while fixed native payload protection remains intact. |
| Native affix conversion could make a fixed custom-named prize appear interchangeable. | Remember the fixed payload before conversion so the exact earned reward stays intact; named random entries still seal. |
| Aether dart shooters inherit ProjectileWeaponItem and were missed by the old bow/crossbow class check. | Include the shared projectile-weapon base class in the ranged equipment group. |

Standard `NestedLootTable` uses raw generation and NeoForge applies global modifiers at the root. No extra vanilla nested-table hook was added. Ancient Reforging definitions included in Apothic Compats are conditionally inactive without that mod; their resource presence does not activate a seventh rarity.

The End Remastered script remains unchanged: all 16 entity modifiers retain the latest boss/eye mapping. Archaion's Last of Deepslate natively drops a smithing template and Echo Charges; those are materials and are not replaced with an invented equipment reward.

## Verification

`LootPoolSmoke` runs in a real dedicated server with the exact pinned Apotheosis, Placebo, Aether, Twilight Forest, Apothic Compats and Lootr builds. It checks all six native salvage routes and legacy aliases, progression boundaries in loaded dimensions, every registered explicit affix item entry per dimension, generated chest/reward tables, preservation of materials and trophies, playerless/nonstandard chest contexts, fishing isolation, empty modifiers, reserved invader prizes and paid trader offers. CI requires its positive completion marker as well as the existing unit/server/asset checks.

Use the final CI receipt for actual counts and success/failure. No full-pack or graphical playtest is inferred from a successful focused server run. Previously recorded combined-pack startup blockers remain outstanding.
