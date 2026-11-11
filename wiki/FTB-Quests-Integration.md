> **Standalone 0.6.1-alpha reference.** For TNK's Progression-based levels, XP costs, salvage and six-rarity integration, use the [TNK edition guide](https://github.com/Mikaaah/Unidentified-Loot/wiki/TNK-Edition).

[← Guide home](https://github.com/Mikaaah/Unidentified-Loot/wiki) · [Project](https://github.com/Mikaaah/Unidentified-Loot) · [Get help](https://github.com/Mikaaah/Unidentified-Loot/issues)

# FTB Quests bridge: no KubeJS / no FTB API required

Set `progression_mode` to `FTB_QUESTS` in `config/tnkloot/tiers.json`, then `/tnkloot reload`.

For each milestone quest, add a **Command Reward** to FTB Quests. Set its permission level to **2**.
The executing reward must target the player claiming that quest. FTB Quests 1.21.1 executes Command Rewards with the claiming player as the command source, so `@s` targets that exact player. Use `tnkloot tier unlock @s explorer`, not a nearest-player selector.

| Quest unlock | Command Reward |
|---|---|
| Getting an Upgrade | `/tnkloot tier unlock @s apprentice` |
| Acquire Hardware | `/tnkloot tier unlock @s adventurer` |
| Diamonds! | `/tnkloot tier unlock @s explorer` |
| Enter the Nether | `/tnkloot tier unlock @s veteran` |
| Obtain Blaze Rod | `/tnkloot tier unlock @s champion` |
| Summon the Wither | `/tnkloot tier unlock @s hero` |
| Defeat Ender Dragon | `/tnkloot tier unlock @s legendary` |

Every grant writes both personal progress and, if FTB Teams is available and detected, the current team's progress. Other team members can use that tier. Even if every member claims the reward separately, repeat grants are idempotent.

IMPORTANT: A Command Reward runs **on claim**, not simply on quest completion. If you want automatic unlock the moment a quest completes, configure FTB Quests to auto-claim that reward or use a dedicated quest-completion integration in a subsequent release.

Use `/tnkloot tier` to view your current tier. To award tier-aware loot, add a Command Reward such as `tnkloot unidentified @s`. Ordinary item rewards containing a bare token have no recorded tier and default to rank 0.

Implementation reference: [FTB Quests 1.21.1 CommandReward](https://github.com/FTBTeam/FTB-Quests/blob/1.21.1/main/common/src/main/java/dev/ftb/mods/ftbquests/quest/reward/CommandReward.java). This bridge is a Command Reward, not a new custom reward type in the editor. Multiplayer claiming and optional team sharing still require testing in your pack.
