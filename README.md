<p align="center"><img src="docs/assets/banner.png" alt="Unidentified Loot" width="100%"></p>

<h1 align="center">Unidentified Loot</h1>

<p align="center"><strong>Bring home a mystery. Identify your finds, discover powerful equipment, and build around the result.</strong></p>

<p align="center"><a href="https://www.curseforge.com/minecraft/mc-mods/unidentified-loot-a-towns-kingdoms-mod">Downloads</a> · <a href="https://github.com/Mikaaah/Unidentified-Loot/wiki">Player guide</a> · <a href="https://github.com/Mikaaah/Unidentified-Loot/issues">Report an issue</a> · <a href="https://github.com/Mikaaah/Unidentified-Loot/blob/release/tnk-1.0.0/CHANGELOG.md">Changelog</a> · <a href="https://discord.gg/CRYk3uDPVt">Discord</a></p>

<p align="center"><img src="https://img.shields.io/badge/Minecraft-1.21.1-5b8731?style=flat-square" alt="Minecraft 1.21.1"> <img src="https://img.shields.io/badge/NeoForge-d77828?style=flat-square" alt="NeoForge"> <a href="https://mikaaah.github.io/TownsAndKingdoms/projects/"><img src="https://img.shields.io/badge/Towns_%26_Kingdoms-Modding_Hub-7f384a?style=flat-square" alt="Towns & Kingdoms Modding Hub"></a></p>


## A mystery worth bringing home

**Minecraft 1.21.1 · NeoForge 21.1.262 · Java 21 · 1.0.0 TNK release candidate**

**1.0.1 update:** crafted equipment can be reforged with the same UL rarity groups and attribute limits used by loot and invader prizes. Pinnacle is activated automatically. See [equipment and Apotheosis integration](docs/APOTHEOSIS-UNIFICATION.md).

This edition uses the real TNK Progression API for source levels, milestone ceilings, rarity caps and mob levels. It retains the Beta 1.0 / 0.19.15 Identifier, seven-slot identification and salvage, previews, collection, custom artwork, boss editor, dimension rewards and optional Create Mechanical Identifier/Ponder integration.

The equipment and boss integration checks pass in focused dependency batches. **The complete Beta 1.0 pack is not certified:** Knaves' Needs optional-integration startup and a separate dimension registry-freeze failure block that test. See [the release audit](docs/TNK-RELEASE-1-AUDIT.md) for the evidence and exact limits.

| Component | Supported / checked |
| --- | --- |
| Minecraft / loader | 1.21.1 / NeoForge 21.1.262 |
| Java | 21 |
| TNK Progression | Required; 1.2.0-private.1 and 1.3.0-private.1 checked |
| Core integrations | Apotheosis 8.9.0, Apothic Attributes 2.11.0, Placebo 9.9.4, Patchouli 1.21.1-93-NEOFORGE |
| Create | Optional; 6.0.10 with bundled Ponder 1.0.82 checked |
| Liquid XP automation | Optional; checked against the Beta 1.0 Create Enchantment Industry runtime |
| Other loaders / Minecraft versions | Not supported by this build |

Replace the old `tnkloot` JAR on **both client and server**; install only one copy. Keep the Progression companion as a separate mod. Existing sealed drops retain their rarity and tier. The release migrates the shipped Beta preset once and writes configuration backups. Custom additions are retained by the release migration; existing compatibility import rules still apply. The optional full-pack configuration supplied with the audit is for the stock Beta pack, not a wholesale replacement for a customized server configuration.

The pack item catalogue's fully disabled equipment and infinite-food removals also apply to automatic imports, reward pools and previews. `/tnkloot reload` refreshes this policy.

Use `/tnkloot audit` as an operator to export the actual runtime item groups, source-level ranges, item modifiers, registered attributes, affix definitions and boss mappings to `config/tnkloot/reports`.
## Pull up a chair

Questions, builds, feedback, server news and the occasional wildly overambitious factory: join the **Towns & Kingdoms Modding Hub**.

[![Join the Towns & Kingdoms Discord](https://i.imgur.com/Ojgcccm.png)](https://discord.gg/CRYk3uDPVt)

**[Join Discord](https://discord.gg/CRYk3uDPVt)** · [Explore all projects](https://mikaaah.github.io/TownsAndKingdoms/projects/) · [CurseForge projects](https://www.curseforge.com/members/mikaaah/projects)
