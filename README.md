<p align="center"><img src="docs/assets/banner.png" alt="Unidentified Loot" width="100%"></p>

<h1 align="center">Unidentified Loot</h1>

<p align="center"><a href="https://www.curseforge.com/minecraft/mc-mods/unidentified-loot-a-towns-kingdoms-mod">CurseForge</a> · <a href="https://github.com/Mikaaah/TNK-Loot-Identification/wiki">Wiki</a> · <a href="https://github.com/Mikaaah/TNK-Loot-Identification/issues">Report an issue</a> · <a href="https://github.com/Mikaaah/TNK-Loot-Identification/blob/main/CHANGELOG.md">Changelog</a></p>

<p align="center"><img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-568736?style=flat-square"> <img alt="NeoForge" src="https://img.shields.io/badge/Loader-NeoForge-d77828?style=flat-square"></p>

Turn equipment drops into unidentified finds, then reveal their potential at a **seven-slot Guild Identifier**. A configurable loot and progression mod for **Minecraft 1.21.1 / NeoForge**, built around real **Apotheosis affixes**.

## Find it. Identify it. Build around it.

- Unidentified **weapons, bows, armor, tools and accessories**, with tier-aware rewards.
- A Guild Identifier counter with a cost preview, batch confirmation and animated item reveals.
- Configurable equipment pools, prices and progression; original item behavior and movesets are retained.
- Eight default Guild tiers, with advancement, quest, manual or unrestricted progression.
- An operator editor for rarity mappings and item pools, plus optional FTB Quests and FTB Teams integration.

## Install

Install the mod on **client and server** with **Java 21**, **Minecraft 1.21.1** and **NeoForge 21.1.262 or newer**. **Apotheosis is required**, with Placebo, Patchouli, Apothic Attributes, Apothic Spawners and Apothic Enchanting. FTB Quests and FTB Teams are optional.

[Download a successful GitHub artifact](https://github.com/Mikaaah/TNK-Loot-Identification/actions/workflows/build.yml): open a successful run, then download its plain mod JAR from Artifacts.

## First steps

1. Launch once to generate `config/tnkloot/tiers.json`.
2. Place the **Identifier** from the T&K Loot creative tab, or use `/give @s tnkloot:identifier` as an operator.
3. Collect an unidentified item, insert it at the counter, review the price and confirm.
4. For tier-aware test rewards, use `/tnkloot unidentified @s explorer helmet`.

Modpacks can change equipment pools, identification costs, tier names and unlock milestones. Existing configuration files are preserved on update.

## Guides

| Guide | Includes |
| --- | --- |
| [Identifier and loot](https://github.com/Mikaaah/TNK-Loot-Identification/wiki/Identifier-and-Loot) | Tokens, the counter and revealed equipment |
| [Progression and teams](https://github.com/Mikaaah/TNK-Loot-Identification/wiki/Progression-and-Teams) | Unlock modes and shared progress |
| [Configuration](https://github.com/Mikaaah/TNK-Loot-Identification/wiki/Configuration) | Prices, tiers and equipment pools |
| [Commands and editor](https://github.com/Mikaaah/TNK-Loot-Identification/wiki/Commands-and-Admin-Editor) | Rewards and operator tools |
| [FTB Quests](https://github.com/Mikaaah/TNK-Loot-Identification/wiki/FTB-Quests-Integration) | Tier-aware quest rewards |

## Credits and license

By **Towns & Kingdoms**. [All rights reserved](LICENSE). Requires Apotheosis and its dependencies. See [the changelog](CHANGELOG.md) for features present in each published source version.
