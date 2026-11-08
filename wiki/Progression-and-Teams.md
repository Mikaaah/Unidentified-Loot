[← Guide home](https://github.com/Mikaaah/Unidentified-Loot/wiki) · [Project](https://github.com/Mikaaah/Unidentified-Loot) · [Get help](https://github.com/Mikaaah/Unidentified-Loot/issues)

# Progression and Teams

## Progression modes

| Mode | Behavior |
| --- | --- |
| `ADVANCEMENTS` | Unlock from configured advancements; checks existing progress on login and successful reload. Any listed advancement unlocks its tier. |
| `FTB_QUESTS` | Only commands grant progress. Use quest Command Rewards; no hard FTB dependency. |
| `MANUAL` | Only commands grant progress. Suitable for another quest or scripting system. |
| `OFF` | Every player has the highest configured tier. Identification remains available. |

Set `identification_enabled` to `false` to disable redemption. Set `share_ftb_teams` to `false` for individual progression. Optional team integration falls back to personal progress if the FTB Teams API is unavailable and logs a warning.


## Behavior and boundaries

- Tokens keep their recorded **rank and display name** after player progression changes. Weapon definitions are evaluated when identified, so editing a pool also changes the potential result of existing tokens.
- Failed identification keeps the token. A full inventory drops newly awarded tokens/results safely when necessary.
- Tier grants are persistent per world, personal UUID and optionally current FTB team UUID. Leaving a team retains your own earned progress; a team-only rank does not become personal progress.
- Resets affect the current team's shared rank, but do not erase other members' personal progress. In advancement mode, qualifying advancements can restore ranks on login/reload. Use manual/quest mode for a persistent administrative rollback.
- Numeric tier ranks are save identities. Renaming a tier is safe; renumbering ranks changes the meaning of existing player data and tokens. Back up the world before changing rank assignments.
- Client tooltips use the tier name stored on awarded tokens. Server configuration determines redemption; clients do not need a copy of the server's weapon configuration.
