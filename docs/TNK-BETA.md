# TNK progression beta

This branch is specifically for Towns & Kingdoms. It adds a required `tnkprogression >= 0.1.0-beta.1` dependency. Main is unchanged. Version: **0.6.2-tnk-beta.1**.

## Equipment probability

Each direct player kill of a hostile mob implementing vanilla `Enemy` receives **one 2% equipment roll**. This is independent of tier weights. The death handler replaces configured equipment and affix-equipment drops from the vanilla/Apotheosis paths, while preserving gems, materials and unrelated loot. It adds at most one token. Missing equipment pools produce no token and a warning, not a reroll into a higher tier. Environment-only kills, summons and custom mobs without the `Enemy` marker are not included in this beta.

Chest tables have a separate 2% roll, using the lower of player level and milestone ceiling. Existing configured/affixed equipment entries are replaced by that roll, rather than receiving an additional guaranteed token. This deliberately changes chest weapon frequency. Other block/fishing/gameplay tables are ignored. Apotheosis remains the affix generator at identification; this beta owns the equipment drop frequency. Other mods appending equipment after LOWEST drop processing must be audited in the complete pack.

`config/tnkloot/tnk-beta.json` configures mob and chest chances and six independent rarity weights. Restart after edits. Existing `tiers.json` item pools remain authoritative and need all eight tiers. Never overwrite an existing pack pool with the bundled vanilla starter pool. New default progression mode is MANUAL; the beta's generated tokens use TNK Progression, not old advancement tiers.

## Conditional tier weights

| Source level | Equipment tier weights |
|---|---|
|1–10|0:75%, 1:25%|
|11–30|0:50%, 1:35%, 2:15%|
|31–50|1:50%, 2:35%, 3:15%|
|51–70|2:50%, 3:35%, 4:15%|
|71–80|3:50%, 4:35%, 5:15%|
|81–94|4:50%, 5:35%, 6:15%|
|95–100|5:60%, 6:35%, 7:5%|

Mob source level is clamped by the recipient's milestone ceiling before rolling. The actual mob remains at its full difficulty. Tiers: Novice, Apprentice, Adventurer, Explorer, Veteran, Champion, Hero, Legendary.

Rarity is a second roll, independent of equipment tier: Common, Uncommon, Rare, Epic, Legendary, Mythic. Defaults: weights 6000,2500,1000,400,90,10, restricted/renormalized to the milestone's eligible rarities. The token stores tier, selected rarity, source level and deterministic roll seed at generation. Identifying later or trading a token cannot improve its rarity. Trading is allowed; progression gates constrain generation, not ownership/use. Legacy/admin tokens without a rarity stamp identify as Common. Existing GUI tier rarity editing no longer controls TNK drop rarity; use the beta config.

## Release checks

Build with Java 21: `./gradlew build`. Test in a disposable pack world with both jars. Check 0%/100% configured drop chances, boundary mob levels, two FTB team members and a late joiner, reconnects and team changes, chest opening and token identification after milestones. Optional other mod items must be installed to validate their pools. Never publish this beta as the general-purpose release.

## Companion source

`progression-bootstrap/` contains the exact companion source for this initial beta, so CI and testers can build both mods before the standalone progression repository is created. It is a separate mod project and produces a separate JAR. Its README includes FTB Quests command examples and FTB Teams behavior.
