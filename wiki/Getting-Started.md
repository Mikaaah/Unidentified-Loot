# Getting Started

## Installation and quick start

Install the compiled `tnkloot` JAR on **both client and server**, with Java 21 and NeoForge **21.1.262 or newer** for Minecraft 1.21.1. Apotheosis is required, along with Placebo, Patchouli, Apothic Attributes, Apothic Spawners and Apothic Enchanting. FTB Quests and FTB Teams remain optional.

1. Start a world/server once to generate `config/tnkloot/tiers.json`.
2. Edit the tiers and weapon pools. See [the configuration guide](https://github.com/Mikaaah/Unidentified-Loot/wiki/Configuration).
3. Run `/tnkloot reload`, then `/tnkloot validate` as an operator.
4. Run `/tnkloot unidentified @s` in game, place an Identifier (`/give @s tnkloot:identifier`), open its inventory, insert the token and confirm the shown cost. Add a category, for example `/tnkloot unidentified @s explorer helmet`, to award a different token type.

The default config has eight tiers: Novice, Apprentice, Adventurer, Explorer, Veteran, Champion, Hero and Legendary. Vanilla advancement milestones unlock them automatically. Modpacks can replace the entire tier list, names, milestones and weighted weapon pools.

