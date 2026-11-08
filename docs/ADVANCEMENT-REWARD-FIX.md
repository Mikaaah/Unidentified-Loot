# Advancement reward protection — 0.18.1-tnk-beta.1

Minecraft 1.21.1 / NeoForge 21.1.262+ / Java 21. Requires the same TNK Progression and Apotheosis/Patchouli stack as 0.18.0-tnk-beta.1.

The final loot pass now preserves advancement rewards before any TNK replacement, bonus injection, boss consolidation or rarity capping. It retains the actual ADVANCEMENT_REWARD parameter-set origin on LootParams instead of inferring the origin from a table name. A reward can therefore reuse a chest table without becoming an unidentified drop. Nested evaluations using those parameters retain the protection. Other mods' loot modifiers still run normally.

Decorative BlockItems are no longer auto-classified as equipment. Explicit entries in the equipment configuration retain priority.

Update the loot JAR on both client and server; keep TNK Progression and the existing configs. No GUI, texture, Ponder or boss feature is removed. This prevents future conversions; it cannot reconstruct components already discarded by an older build. Previously broken rewards may need to be granted again.

The opt-in dedicated-server regression is enabled with `-Dtnkloot.advancementRewardSmokeTest=true`. Success is logged as `TNKLOOT_ADVANCEMENT_REWARDS_OK`; failures as `TNKLOOT_ADVANCEMENT_REWARDS_FAILED`. It checks real CommonHooks processing of advancement contexts, including a reused chest table, counts/custom components, builder-origin isolation, decorative blocks and valid ordinary equipment conversion. The banner/trophy fixtures use vanilla items; the full modpack still needs testing with its exact reward mods.
