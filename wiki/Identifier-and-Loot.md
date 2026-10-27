# Identifier and Loot

Each category token records its Guild tier and a stable roll seed when awarded. Bring tokens to a Guild Identifier. Its one-row inventory accepts up to seven items, shows the total cost, and reveals each result with a short item roll and note-block sounds after confirmation. Results retain the original registered item's behavior and moveset integrations. This mod does not change item crafting or who can equip an item.

Find the Identifier block and all eight unidentified item types in the **T&K Loot** Creative inventory tab.


## Using the counter

Place `tnkloot:identifier` and right-click the block or its attendant. Insert up to seven tokens, one per slot. Confirm the displayed total cost to start the item rolls. Note-block ticks accompany each roll and a final chime announces the result. Right-click identification while holding a token has been removed.

Payment and validation happen for the whole batch before animation begins. Inventory actions lock during rolling. Closing the screen, disconnecting or dying returns the already-paid results. Breaking the counter removes its protected attendant with departure particles.

## Loot replacement

Configured equipment IDs in generated loot become category tokens. With a player context, the token records the player’s effective tier; context-free loot starts at rank 0, clamped to the configured item’s inclusive tier range. Unrelated items stay unchanged. See [Configuration](https://github.com/Mikaaah/Unidentified-Loot/wiki/Configuration) for replacement switches and weighted item pools.

## Token identity

Tokens retain their recorded rank, display name and stable roll seed. Progressing later does not upgrade an existing token. Editing equipment pools changes its eligible results; retrying after failed payment does not reroll its base item. Bare tokens from ordinary `/give` default to rank 0; use `/tnkloot unidentified` for tier-aware rewards.
