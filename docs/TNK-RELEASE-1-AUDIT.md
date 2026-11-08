# TNK Unidentified Loot — Release 1 audit

Build: `tnkloot-1.0.0-tnk-neoforge-1.21.1.jar`.

**Candidate built and checked; the complete Beta 1.0 pack is not yet release-certified.** The loot integration passes the focused runtime checks below. Knaves' Needs optional-integration initialization and a separate dimension-stack registry freeze prevent certification of the simultaneous pack. They are recorded as blockers, not silently removed from the mod list.

## Inputs and source

The authoritative input is the uploaded **Towns & Kingdoms 3 | Beyond the Forge Beta 1.0** ZIP: Minecraft 1.21.1, NeoForge 21.1.262, 315 pinned manifest JARs plus the supplied mod overrides. Every manifest JAR was downloaded and its registry/dependency metadata inspected; the report pins each with its CurseForge project/file IDs and SHA-256. Manifest JAR inspection is distinct from a successful runtime test of the whole pack.

The verified repository redirects to `Mikaaah/Unidentified-Loot`. The remote `tnk-progression-beta` source at `bf790813d2c209bc41e8d6e6feebbf124632b423` predates the installed mod. This branch reconstructs the current source from the supplied 0.18.1 advancement hotfix/source kit, the 0.19.15 source overlay, and the actual Beta 1.0 `tnkloot-0.19.15-tnk-beta.1.jar`. It preserves the latest machine, GUI, NPC and collection work. All **88 original artwork PNGs** remain byte-identical. The main branch is not replaced by this candidate.

The real Progression 1.2.0-private.1 pack JAR and latest supplied 1.3.0-private.1 JAR were checked. The historical bootstrap subproject is not substituted for either companion. CI now downloads the actual supported 1.2.0 companion with a checksum.

## Implemented corrections

| Area | Result | Main implementation |
| --- | --- | --- |
| Progression | Resolve the real API once; use mob level/kill credit, clamp player source levels to 1–100 and the milestone ceiling, and retain sealed rarity when traded or identified | `loot/TnkLevelBridge`, `TnkLootRolls`, `BetaLoot` |
| Advancement rewards | Restore all four required origin/context mixins; skip every TNK replacement, bonus injection, routing and cap path for actual advancement rewards, including reused chest table IDs | `injection/FinalLoot`, `injection/mixin/*` |
| Decorative loot | Banners, trophy blocks and heads are not auto-classified as equipment; explicitly configured equipment remains supported | `dimension/BaseCompat`, `injection/FinalLoot` |
| Fixed native rewards | Preserve named/custom-component native rewards; copy the installed Legendary Spellbooks spell-slot components into randomly selected matching boss rewards | `injection/LootProtection`, `compat/NativeLootTemplates` |
| Boss crates | Universal boss crates accept armour, accessories and other configured categories; boss-exclusive equipment stays out of normal vault pools | `compat/BossRewards`, `menu/TnkMenu` |
| Invalid boss definitions | Correct `ender_hammer` → `monstrous_anchor` and `sickle_of_harvest` → `totem_of_moss`; remove the nonexistent Sculptor trial entity | `compat/ReleaseConfigMigration`, bundled boss presets |
| Additional bosses/rewards | Add Eye of the Storm and The Obliterator pools; include Cloud Golem's optional Stormbound Grimoire; route Deep Aether earned treasure tables | Boss presets and `dimension/DimensionLoot` |
| Duplicate boss awards | Stop bonus crates on the Aether bosses' key-only deaths and each Knight Phantom phase; earned treasure still routes to its pool | Boss presets/migration |
| Spell and accessory affixes | Add 342 conditionally loaded adapters mapping the installed Iron's Apothic / Apothic Compats affixes onto TNK rarities; preserve original conditions, value ranges and exclusivity | `data/tnkloot/affixes/compat`, `tools/build_tnk_compat_affixes.py` |
| Item catalogue | Share subgroup and source-level labels across the vault, collection and operator export | `compat/EquipmentGroups`, `EquipmentAudit` |
| Collection QoL | Discover identified gear acquired from automation/trading; repeated inventory scans do not inflate identification counts | `features/PlayerLedger`, `FeatureHooks` |
| Readability | Show sealed source levels, actual capped rarity probabilities, and clearer boss reward/discovery tooltips; accept old namespaced rarity IDs | Item/collection screens and rarity helpers |
| Pack item policy | Respect `fullyDisabledItems` and `infiniteFoods` from the supplied KubeJS catalogue in ordinary/boss pools and previews; prevent reintroduction of disabled Artifacts/infinite-food equipment | `compat/PackItemPolicy`, `PresetCleanup`, selector/reward paths |
| Maintenance | Cache boss-exclusive lookups, use long weighted-roll totals, correct dependency metadata and release CI, and retain migration backups | Build tools and compatibility classes |

The pack policy is read from `kubejs/pack_catalog.json` when present, with the shipped Beta policy as the fallback. `/tnkloot reload` refreshes it; invalid overrides retain the last valid exclusions. `pack-disabled-items.csv` lists the explicit exclusions. Static review also found global LootJS filters in `cleanup.js`, `ae2_compat.js` and `ie_compat.js`; the new TNK advancement guard does not bypass those established pack policies. Their end-to-end behavior still needs the corrected full-pack run.

The one-time release migration cleans **stock Beta preset entries** that are unavailable or boss-exclusive, normalizes the old Ancient tier reference to Mythic, and keeps progression under TNK Progression. User-added entries are retained by this migration; pre-existing compatibility import/cleanup behavior still applies. Backups use `.before-tnk-release-1.bak`. New optional compatibility does not require every supported mod to be installed.

## Equipment, levels and attributes

The reports contain **1,190 items observed in successful runtime batches**, grouped by category, subgroup, tier range, possible source-level range, origin dimension, boss pool, weight and actual base item modifiers. The groups include melee, spellcasting, ranged weapons, shields, fishing, farming, mining, axes, armour slots and accessories.

The **491 stock Beta item IDs not observed in those successful batches** are in a separate file. Some may be obsolete; others depend on mods excluded from the successful batches. They are not represented as verified or all missing. In particular, BetterEnd, BetterNether and Knaves' Needs cannot be certified from these runs. The real full-pack `/tnkloot audit` resolves the installed registries after startup.

The attribute report lists **199 actually registered attributes**, their defaults and declared bounds. Item rows include native base modifier amount, operation and slot. The JSON and affix CSV include **1,019 affix resource definitions**, their conditions, categories, rarity/value mappings and ranges, including the 342 TNK adapters. Resource presence does not mean its optional-mod conditions are active. Mod-computed spell, relic or Curios effects are not all static vanilla ItemAttributeModifiers; their resource/component definitions must also be consulted. This is an API/data audit, not a claim that every resulting combat combination has been play-balanced.

Ordinary loot uses these source-level probabilities after the recipient's milestone ceiling is applied:

| Effective source level | Tier probabilities |
| --- | --- |
| 1–10 | T0 75%, T1 25% |
| 11–30 | T0 50%, T1 35%, T2 15% |
| 31–50 | T1 50%, T2 35%, T3 15% |
| 51–70 | T2 50%, T3 35%, T4 15% |
| 71–80 | T3 50%, T4 35%, T5 15% |
| 81–94 | T4 50%, T5 35%, T6 15% |
| 95–100 | T5 60%, T6 35%, T7 5% |

`levels-1-100.csv` expands every level; `rarity-affix-bands.csv` expands every TNK rarity and tier. A source-level range shows where an item can enter a pool, not an equip requirement or guaranteed drop. Boss pools retain their configured tiers, reward weights and boss rarity distribution subject to the recipient's rarity cap. Vanilla XP fees and salvage credit remain separate from character levels. No new global exploration, dimension or Wither access lock is added.

## Boss and reward coverage

All **49 configured boss pools** were observed with registered entities across the boss and weapons batches. The exact entity, tier and every configured reward ID appear in `boss-rewards.csv` and `complete-audit.json`. The main boss batch performed **149 forced reward identifications**, including the mixed equipment categories and native spell components. Additional magic/weapons batches performed five and four boss reward checks respectively; those figures include overlap and are not claimed as unique item totals.

Boss materials, keys and ingredients remain native drops. Named or custom-component fixed prizes are preserved. Aether/Twilight earned caches consolidate their configured equipment into the corresponding crate without stripping unrelated materials. Earthrend Gauntlet remains reserved for the Sculptor's native trial interaction; no fake death entity is used to award it. Vanilla Wither/dragon materials are not ordinary equipment replacements.

## Ponder rebuild

Four separate tutorials cover sealed loot/levels, batch identification, salvage review, and Liquid XP automation. They use small dark-oak workshops, the actual counter/clerk and machine models, real depot-held items, readable paced captions and the machine's facing-relative port. Automation shows top input, right-side Liquid XP and bottom extraction, explains the need for a pumping network, and does not imply rotational power is required by the Identifier.

The actual Minecraft client replayed all four tutorials and saved **12 rendered checkpoints**. Screenshots are included under `reports/ponder`. The development-only replay/screenshot harness is excluded from the release JAR. Existing Identifier, Salvage, Preview and Collection artwork is unchanged.

## Verification evidence

| Check | Result |
| --- | --- |
| Gradle build / JUnit | Successful; 19 tests, no failures or errors |
| Boss integration batch | 100 source-level identifications; 774 equipment/rarity checks; 149 boss rewards; 29 advancement/banner/trophy checks |
| Magic/accessory batch | 100 source-level identifications; 782 equipment/rarity checks; 5 boss rewards; 24 protected reward checks |
| Weapons/Epic Fight/Alex's Caves batch | 100 source-level identifications; 920 equipment/rarity checks; 4 boss rewards; 18 protected reward checks |
| TNK Beta integration | 448 identifications; milestone/rarity caps; legacy rarity migration; sealed trading; 0/100% mob/chest chance and idempotence |
| Boss editor integration | 160 mixed-category Mythic rolls; imports/ghost guards, material/player protection, percentages and persistence |
| Real advancement context | Reward counts/components preserved by TNK; reused-builder origin isolation; reused chest IDs; normal equipment still identifiable |
| Pack policy regression | Actual catalogue override applied; malformed override rejected without losing exclusions; forbidden equipment absent from ordinary/boss options |
| Mechanical Identifier | Actual Create 6.0.10 / CEI 2.4.2; sided capabilities, real Liquid XP, capacity and simulation, affixes, persisted in-flight job, no double payment, blocked inputs and invalid boss safety |
| Companion minimum | Dedicated server smoke passed with actual Progression 1.2.0-private.1; focused batches use 1.3.0-private.1 |
| Client Ponder | All four tutorials replayed; 12 rendered checkpoints inspected |
| Packaging | 88 unchanged Beta artwork PNGs; 48 exact rarity icons; 70 acyclic/complete models; four mixins; 342 optional affixes; no embedded Minecraft/Create/Progression APIs or screenshot harness |

Total focused equipment/rarity checks: **2,476**, including repeated equipment shared by the batches. These tests exercise real registered items and affix application, not a mock list. Logs with positive `TNKLOOT_*_OK` markers are included. A successful Gradle task alone was not treated as a passing Minecraft test.

Other mods may append bonus loot when an advancement intentionally reuses a chest table. The CommonHooks regression verifies the awarded original stacks and absence of TNK sealing while allowing those third-party additions; TNK does not disable other mods' global loot modifiers.

## Remaining release blockers and limits

| Priority | Finding | Evidence / next validation |
| --- | --- | --- |
| Pack blocker | Knaves' Needs 3.0.0-beta.1 initializes Blue Skies additions through Fzzy Config 0.7.7+fix1 even though Blue Skies is absent. `RegistrarImpl.init` throws `NoSuchElementException` from `BlueSkiesAdditionsRegistries.smartRegistrar` with the stock enabled integration flags | `knavesneeds-blocker.log`; inspect the optional integration flags against this exact JAR. A local diagnostic disabling absent integrations did not by itself make the dimension stack start |
| Pack blocker | BetterEnd/BetterNether/BCLib/Wover dimension batch fails registry freeze with 13 unregistered intrusive holders. It also fails with Knaves' Needs omitted. The root cause is not isolated, and independence from Loot is not proven | `dimension-registry-blocker.log`; resolve with the exact dimension stack, then rerun the full runtime audit. Do not use the current report to claim its equipment registrations are valid |
| Pack verification gap | The 315-mod simultaneous server run encounters additional client-class-loading issues; the combined heavy equipment stack exceeded this environment's memory limit | `full-pack-server-attempt.log`; successful split batches establish the integration coverage above, not whole-pack startup |
| Playtest gap | No complete live multiplayer campaign, boss encounter, quest reward run or combat balance pass was completed | Validate the packaged candidate in the corrected actual pack before publishing as final |

The supplied config is derived from the **full Beta configuration**, with confirmed ordinary imports and boss corrections. It is not one of the pruned split-runtime test configs. Its release marker is deliberately zero so migration resolves availability once against the real installed full-pack registries. Unobserved Beta entries remain until that real registry pass. Do not wholesale overwrite a server's customized settings with this preset.

## Deliverables and installation

- `tnkloot-1.0.0-tnk-neoforge-1.21.1.jar`: install on both sides, replacing the old `tnkloot` JAR.
- `tnkloot-1.0.0-tnk-neoforge-1.21.1-sources.jar`: source reference; do not install as a mod.
- `TNK_Loot_Release_1_TNK_1.21.1.zip`: JARs, optional stock-pack config, this audit, changelog, exact reports, verification receipts and Ponder screenshots.

Supported target: **Minecraft 1.21.1 / NeoForge / Java 21 only**. Tested loader is the pack's **21.1.262**. TNK Progression is required and remains separate. Create and the attribute compatibility providers remain optional. The JAR is ready for the next corrected-pack validation pass; the blockers above must be resolved before calling the entire pack release-ready.
