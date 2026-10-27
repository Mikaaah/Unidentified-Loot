# Troubleshooting

## Identification is unavailable

Install matching mod versions on client and server with Java 21, Minecraft 1.21.1, NeoForge 21.1.262+ and Apotheosis 1.21.1-8.9.0+ with its dependencies. Check `identification_enabled` and `station_identification`. Use the block or attendant to open the counter.

## A token cannot be identified

Run `/tnkloot validate`. Check that its category and recorded rank have a nonempty live equipment pool, that the currency exists and the player can afford the batch. Apotheosis must have a registered rarity and at least one eligible affix for each item. A bad rarity, unsupported category or empty affix pool refuses the whole batch before payment. Accessories need an Apotheosis loot-category integration.

## Configuration will not reload

Unknown keys, invalid JSON, duplicate ranks or invalid weights reject the whole reload. Correct the reported errors and run `/tnkloot reload` again. The last valid in-memory settings remain active; a first launch with no valid configuration disables identification.

## A quest grants the wrong tier

Use Command Rewards with `tnkloot tier unlock @s <tier>` at permission level 2. Rewards execute when claimed; enable auto-claim if appropriate. Use `tnkloot unidentified @s` for tier-aware tokens. Bare Item Rewards default to rank 0.

## A reset is undone

Advancement mode can restore tiers from completed advancements on login or reload. Use manual or quest mode for a lasting administrative rollback. Shared team ranks and members' personal ranks are separate; review [Progression and Teams](https://github.com/Mikaaah/Unidentified-Loot/wiki/Progression-and-Teams).

## Report a problem

Use [GitHub Issues](https://github.com/Mikaaah/Unidentified-Loot/issues/new?template=bug_report.yml). Include mod/runtime versions, reproduction steps, relevant configuration, command output and logs. Team integration still requires validation against your pack's installed FTB Teams version.
