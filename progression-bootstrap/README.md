# Towns & Kingdoms Progression — 0.1.0-beta.2

Minecraft **1.21.1**, NeoForge **21.1.262+**, Java **21**. Original implementation; no Auto Leveling or Just Leveling code is bundled. This is an initial pack beta; tune the balance in a test world.

## Progression

Personal character level 1–100. Level 1 supplies one skill point, with one more per earned level: 100 total. Vanilla XP remains independent. Mining, combat, fishing and discovery of unique chunks contribute to indefinitely repeatable activity milestones. Their requirements grow quadratically; their XP rewards grow with the square root of the completed rank. Levels cost `100 + 40 * level²` XP. These initial values deliberately favor a mixed playstyle; a single repeatable activity can still supply all XP.

`config/tnkprogression.json` controls activity starting costs/rewards, level ceilings, team sharing, skill category, mob attribute scaling, distance scaling and dimension baselines. `/tnkprogress reload` atomically validates and reloads it. Default level ceilings: 10, 30, 50, 70, 80, 94, 100. Default highest eligible rarity by ceiling: Uncommon, Rare, Epic, Legendary, Legendary, Mythic, Mythic. These are starting balance proposals, not fixed campaign decisions.

No XP overflow is banked at a ceiling. Activity reward progress pauses at the ceiling; unique exploration locations are still recorded, so revisiting cannot cash them in later. Quest rewards claimed while capped are consumed without banking. Finish unlock quests before claiming XP rewards.

## Commands and FTB Quests

Use FTB Quests **Command Rewards**, with permission level **2**, executing as the claiming player. `{p}` is FTB's player substitution. Do not use nearest-player selectors.

* One-time campaign XP: `tnkprogress reward {p} tnk:chapter_1_quest_04 250`
* Repeatable quest XP: `tnkprogress xp {p} 100`
* Unlock the next bracket: `tnkprogress unlock {p} 30`
* A world event can issue the same unlock command from the server or KubeJS.
* Personal status: `/tnkprogress status`

Set personal XP rewards to be claimable **by each player**, not a shared reward claimed once for the team. The once-only reward ledger is per player and persists across team changes; keep reward IDs stable and unique. Do not use the once-only command for repeatable quests.

FTB Teams is optional. When installed, milestones are stored by team UUID and immediately apply to current/offline/new members. XP, activity ranks and character levels are **not pooled**. The actor also keeps their personal unlock. Other members borrowing a team unlock do not keep that unlock after leaving. Existing character levels/skills are retained after leaving, but further XP and new loot are constrained by their effective ceiling. Team switching never duplicates quest XP claims.

## Pufferfish's Skills

Optional bridge targets the public 0.19.2 API on the 1.21 branch. Configure the exact existing tree category ID (default `tnk:character`). Disable that category's native experience and point exchange sources. This mod sets **total earned entitlement**, not unspent points, under `tnkprogression:character_level`, at login and level-up. Respecs and reconnects do not add points. Remove old point sources during migration; this mod does not destroy existing skill data automatically. Missing/API-incompatible integration logs a warning. The reduced tree itself must be authored separately from this mod.

## Player level badges

An original, icon-free bronze `LVL 0`–`LVL 100` badge appears before the player name in standard chat and on the normal overhead nameplate. It displays **character level**, never vanilla XP level. Actual progression still starts at 1; artwork also supports 0. Every level uses the same simple frame and cream lettering. The bitmap font and all 101 badges are bundled in the mod jar; no separate badge resource pack is needed. Install this version on **both server and clients**.

The server sends authoritative levels on login, level-up, respawn and dimension change. New arrivals receive existing players' levels, and existing players receive the arrival's level. Client state clears when disconnecting. Name formatting preserves the existing display-name component and leaves scoreboard teams and signed chat message content alone. It refreshes the cached name without adding duplicate prefixes. Hidden nameplate/team visibility rules still apply. Custom chat or nameplate mods that replace vanilla display names need an in-pack compatibility check. Tab-list badges are not enabled.

Regenerate the original pixel artwork with `python tools/build_level_badges.py` (Pillow required only for asset generation). `Level-Badges-Preview.png` is a placement mockup using the shipped artwork, not a game capture.

## Mob levels

Hostile `Mob` entities implementing vanilla `Enemy` receive a persistent level at first join. It uses the dimension baseline plus horizontal distance from world spawn and depth below Y48, clamped to 1–100. Defaults: Overworld 1, Nether 31, End 71; +1 level per 256 blocks; +1 per 16 blocks below Y48. It does **not** belong to whichever player triggered spawning. Level is retained on chunk reload and does not change with the killer or nearby players. Health and melee attack damage use stable modifier IDs so reloads cannot stack modifiers. Defaults: +3.5% base health and +1.5% base damage per level above 1. Display uses a custom name when none existed; no client renderer is required. Existing custom mob names remain unchanged.

Loot API uses `min(mob level, recipient milestone ceiling)`. Difficulty itself is not reduced by the recipient's progression. Custom creatures without the `Enemy` marker and indirect/summon kill attribution need explicit pack integration; this beta only credits direct player kills. It does not globally increase vanilla XP.

## Persistence and beta limitations

World saved data: `tnkprogression.dat`. It stores personal profiles, reward claims, team milestones, visited chunks and player-placed block locations. Mining only counts the vanilla base-stone/ore tags included in the event handler; placed blocks are excluded, but piston/automated block movement is not tracked. Automated quarry output is not player mining. Fish progress uses increases of the vanilla fish-caught statistic; offline/past statistics are not retroactively rewarded. Exploration uses unique dimension/chunk pairs and does not count travel distance.

Existing worlds begin at character level 1. Back up before installation. No automatic conversion of the old 150-point tree or old loot tokens is performed. Legacy unsealed loot tokens fall back to Common in the TNK loot beta rather than upgrading when identified later.

## Build / validation

`./gradlew build` with Java 21. Run dedicated-server assertions in a **disposable world only** using `-Dtnkprogression.smoke=true`; they create a test profile, validate caps, quest claims and save roundtrip, then stop the server. See the workflow. Runtime integration with FTB Teams and the installed Pufferfish version must be checked in the pack before publishing.
