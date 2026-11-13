# Dungeon rewards — Loot 1.1.3 TNK

Target: Minecraft 1.21.1, NeoForge 21.1.262, Java 21 and the original Spiral Abyss 2.0.5 JAR. Install this Loot build on clients and server. Spiral is optional; normal TNK equipment pools, identification GUIs and crafting remain available without it.

The dungeon selects the actual item first. Equipment generation then applies the floor's tier/rarity and optional preset once. Identified delivery returns that item directly; unidentified delivery saves the complete result inside a regular TNK token. The NPC and Mechanical Identifier reveal the saved result without choosing different equipment or applying a later player's cap.

## Install and rotate

Build the opt-in example with:

```sh
python3 tools/build_spiral_example.py SpiralAbyss-neoforge-1.21.1-2.0.5.jar TNK_Spiral_Abyss_Rewards_Example_1.1.3.zip
```

Put the ZIP in `<world>/datapacks`, restart after replacing Loot, and use `/reload` for later datapack edits between attempts. Spiral cancels active attempts on reload. The example contains six reward definitions, six thin vanilla loot tables and six floor references, plus shared equipment presets. Its challenges and XP are copied from the supplied Spiral JAR. Themes and weights are editable examples.

For a monthly equipment rotation, edit only:

```text
data/tnk_spiral/tnkloot/dungeon_rewards/chests/floor_1.json
```

Keep the table ID and wrapper stable. Each floor's file contains both its normal pool and rare prizes. Edit shared presets when several floors should share a stat profile. Existing tokens, identified equipment and already-rolled Spiral payouts retain their saved result. Keep the actual item registrations, enchantments and affix definitions available; removing an underlying mod/registry entry is a separate migration.

## A floor's reward definition

This belongs at `data/example/tnkloot/dungeon_rewards/chests/magic.json`, with resource ID `example:chests/magic`:

```json
{
  "tier": 4,
  "source_level": 80,
  "delivery": "unidentified",
  "rarities": {"rare": 60, "epic": 35, "legendary": 5},
  "normal": [
    {"id": "currency", "item": "minecraft:emerald", "weight": 40, "count": {"min": 2, "max": 6}},
    {"id": "food", "item": "minecraft:cooked_beef", "weight": 20, "count": 3},
    {"id": "healing", "item": "minecraft:potion", "weight": 20,
      "components": {"minecraft:potion_contents": {"potion": "minecraft:strong_healing"}}},
    {"id": "scholar", "item": "minecraft:diamond_helmet", "weight": 15,
      "name": "Scholar's Hood", "preset": "example:scholar"},
    {"id": "wizard", "item": "irons_spellbooks:wizard_helmet", "weight": 5,
      "required_mods": ["irons_spellbooks"], "preset": "example:arcane"}
  ],
  "rare": {
    "chance": 0.10,
    "party_scaling": "per_player",
    "rarity": "legendary",
    "delivery": "identified",
    "entries": [
      {"id": "champion", "item": "minecraft:diamond_helmet", "weight": 3,
        "name": "Archmage's Crown", "preset": "example:scholar"},
      {"id": "blade", "item": "minecraft:diamond_sword", "weight": 2}
    ]
  }
}
```

Tier is 0–7; source level is 1–100. Use either `rarity` or relative `rarities` weights with common/uncommon/rare/epic/legendary/mythic. The floor supplies strength directly, without player context. These authored rewards intentionally do not enforce an individual recipient's progression cap.

Each entry inherits tier, source level, rarity and delivery; all can be overridden per entry. Resources, food and potions remain ordinary items. Equipment defaults to unidentified. Use `delivery: "identified"` for an affixed item that can be equipped immediately. Counts are positive integers or inclusive min/max ranges; equipment and other nonstackable items must have count 1. Resource stacks split at the item's native maximum.

Use stable, distinct `id` paths for variants, including two presets of the same helmet. Their previews stay distinct. `required_mods` filters optional entries before calculating displayed odds; add it to optional presets too. At most 128 total preview variants are supported, including inherited prizes and independent bonuses.

`item` plus `components`, or a full vanilla `stack` object, supports native component data. Components merge in this order: preset, stack, entry. Do not use blank Iron's Jewelry ring/necklace items: they require valid native pattern/material data. Such blanks are rejected rather than awarded broken. Existing global item exclusions still apply to dungeon entries.

## Connect a floor

Create the matching vanilla loot table at `data/example/loot_table/chests/magic.json`:

```json
{
  "type": "minecraft:chest",
  "pools": [{"rolls": 1, "entries": [{
    "type": "minecraft:item", "name": "minecraft:paper",
    "functions": [{"function": "tnkloot:dungeon_reward", "table": "example:chests/magic"}]
  }]}]
}
```

The paper is a carrier replaced by the function. In the chosen Spiral `floor.json`, point `rewards.entries` to it:

```json
[{"loot_table": "example:chests/magic", "weight": 1, "chance": 1.0}]
```

Use one dungeon reward definition per floor. On opted-in floors the definition owns the weighted table choices; other weighted loot-table references are replaced. Native independent item bonuses, challenges, XP, claims and mob-drop suppression remain under Spiral. Floors without a matching definition retain their original behavior. The wrapper also generates one normal reward in ordinary Minecraft loot contexts, independently of Spiral.

## Golden prize odds and party behavior

Copper, iron and gold retain 1/2/3 normal rolls per eligible player. Normal weights are relative **per roll**, not a chance of receiving the item at least once across a chest. Multiple normal rolls may choose the same item.

Only the nine-star golden chest makes the rare roll. It first tests the overall `rare.chance`, then chooses one prize using relative entry weights. Candidate count does not multiply the overall chance. Rare prizes must be identified, count 1, and at most one is awarded across the eligible party.

| Configuration | Solo | Party of four |
| --- | --- | --- |
| Overall 10%, `shared` | 10% | 10% |
| Overall 10%, `per_player` | 10% | 34.39% |
| A 50%-weight prize, `per_player` | 5% | 17.195% |

`per_player` uses `1 - (1 - chance)^eligiblePlayers`; `shared` tests chance once. The floor overview and rare journal show actual items, solo prize odds and the party-scaling rule. Announcement remains Spiral's native pickup behavior, using the completed item's name. Saving/loading a payout retains the rare marker.

Omit `rare.entries` to inherit the floor's encountered-enemy linked prizes, including their native enchantments. Omit `rare.chance` to use Spiral's configured rare chance. An empty entries list explicitly disables prizes. The custom one-proc weighted policy applies only to opted-in floors; native floors keep Spiral's native algorithm.

## Reusable equipment presets

Store this at `data/example/tnkloot/equipment_presets/scholar.json`:

```json
{
  "enchantments": {"minecraft:protection": 4, "minecraft:unbreaking": 3},
  "attributes": [{
    "attribute": "minecraft:generic.max_health",
    "amount": {"min": 2, "max": 4},
    "operation": "add_value", "slot": "head"
  }],
  "fixed_affixes": {"tnkloot:armor/attribute/blessed": 0.65},
  "random_affixes": 1,
  "affix_pool": ["tnkloot:armor/attribute/stalwart", "tnkloot:armor/attribute/ironforged"]
}
```

Attribute bonuses append to native armour/damage modifiers. `amount` is a fixed number or min/max range rolled once. `operation` uses vanilla `add_value`, `add_multiplied_base` or `add_multiplied_total`; omitted slots follow the equipment's natural slot. Optional modifier IDs should be unique for bonuses intended to stack. Enchantment levels are explicit; native components and existing enchantments are retained unless a preset deliberately overrides the same component/enchantment.

`fixed_affixes` values are Apotheosis normalized roll levels, 0–1, not direct attribute amounts. Their actual effect also depends on rarity. For a specific visible stat such as +25 mana, use an `attributes` bonus. Fixed-affix ranges roll once and stay saved. `random_affixes` selects distinct compatible affixes, bounded by the floor's tier. An explicit `affix_pool` restricts those rolls. Without a pool, selection stays within ordinary active TNK affixes. An impossible pool fails rather than silently substituting unrelated stats; verify changed presets before running a floor.

Without a preset, normal TNK rarity rules generate affixes. With a preset, only its explicit fixed/random settings are used; omitted random count means zero. Native affixes already on a component-authored item become fixed. Names can be assigned per entry or preset.

For Iron's Spells, the example `arcane_helmet` combines 8–12% spell power, +25 mana, Protection III, Unbreaking III and one magic-only random affix. `tnkloot:dungeon/armor/spell_power` and `tnkloot:dungeon/armor/max_mana` have zero global weight and activate only with Iron's Spells. Explicit dungeon pools can select them without changing the ordinary equipment balance.

Reforging changes the random affix portion using the saved floor tier and pool. It preserves rarity, native modifiers, enchants, fixed affix levels and names. Use reforging material matching the item's original rarity; normal progression requirements still apply to accessing reforging. The plan lives on the item, so changing/removing its preset does not rewrite it. Other systems that intentionally edit equipment, such as augmenting or changing an affix definition, retain their own behavior.

## Generic vanilla loot function

An ordinary vanilla loot table can select a real item and apply:

```json
{"function":"tnkloot:seal_reward","tier":4,"source_level":80,"rarity":"epic","preset":"example:scholar","identified":false}
```

This saves that exact item, rather than choosing global equipment at identification. Set `identified: true` to deliver it finished. Optional `category` uses an existing TNK category when automatic classification is insufficient; it does not invent an Apotheosis equipment category or native state. Standard function conditions are supported. Apply component-setting functions before sealing. Use pool rolls for multiple independent rewards.

Applying the same function to a regular legacy TNK unidentified item preserves the earlier 1.1.3 generic-token behavior. Generic tokens still select equipment through their normal pools; use actual equipment or the dungeon definitions when exact previews and monthly control matter.

## Validation scope

The supplied unmodified Spiral 2.0.5, Iron's Jewelry 2.0.2 and pack-pinned Iron's Spells 3.16.3 were exercised in an isolated NeoForge runtime with Loot's required companions. Tests cover real roll/preview methods, variant IDs, native potion components, saved outcomes, NPC payment/reveal, fixed bonuses after reforging and a catalog rotation, golden party caps/weighted prizes, journal entries and invalid-catalog rejection. The full 307-mod pack has not been run together. Optional thematic entries beyond the loaded test mods require combined-pack validation before final balancing.
