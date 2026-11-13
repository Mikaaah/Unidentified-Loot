"""Build one editable reward definition per Spiral floor for the reworked Loot 1.1.3."""
from pathlib import Path
import argparse
import json
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('spiral_jar', type=Path)
parser.add_argument('output', type=Path)
args = parser.parse_args()
files = {}
def put(path, value):
    files[path] = (json.dumps(value, indent=2) + '\n').encode()
def preset(name, data):
    put(f'data/tnk_spiral/tnkloot/equipment_presets/{name}.json', data)

put('pack.mcmeta', {'pack': {'pack_format': 48, 'description': 'TNK / Spiral: editable floor rewards, exact gear, presets and golden prizes'}})
preset('adventurer', {
    'enchantments': {'minecraft:protection': 2, 'minecraft:unbreaking': 3},
    'attributes': [{'attribute': 'minecraft:generic.max_health', 'amount': {'min': 1, 'max': 3}, 'operation': 'add_value'}],
    'random_affixes': 1,
    'affix_pool': ['tnkloot:armor/attribute/blessed', 'tnkloot:armor/attribute/stalwart', 'tnkloot:armor/attribute/ironforged']})
preset('arcane_helmet', {
    'required_mods': ['irons_spellbooks'],
    'enchantments': {'minecraft:protection': 3, 'minecraft:unbreaking': 3},
    'attributes': [
        {'attribute': 'irons_spellbooks:spell_power', 'amount': {'min': .08, 'max': .12}, 'operation': 'add_multiplied_base', 'slot': 'head'},
        {'attribute': 'irons_spellbooks:max_mana', 'amount': 25, 'operation': 'add_value', 'slot': 'head'}],
    'random_affixes': 1,
    'affix_pool': ['tnkloot:dungeon/armor/spell_power', 'tnkloot:dungeon/armor/max_mana']})
preset('rare_blade', {
    'enchantments': {'minecraft:sharpness': 5, 'minecraft:unbreaking': 3},
    'attributes': [{'attribute': 'minecraft:generic.attack_damage', 'amount': 2, 'operation': 'add_value', 'slot': 'mainhand'}],
    'random_affixes': 1})
preset('rare_crown', {
    'enchantments': {'minecraft:protection': 4, 'minecraft:unbreaking': 3},
    'attributes': [{'attribute': 'minecraft:generic.max_health', 'amount': 4, 'operation': 'add_value', 'slot': 'head'}],
    'fixed_affixes': {'tnkloot:armor/attribute/blessed': .65},
    'random_affixes': 1,
    'affix_pool': ['tnkloot:armor/attribute/stalwart', 'tnkloot:armor/attribute/ironforged']})
preset('rare_bow', {'enchantments': {'minecraft:power': 5, 'minecraft:unbreaking': 3}, 'random_affixes': 2})
themes = [
    ('Arcane', 1, 20, [('irons_spellbooks:wizard_helmet', 'irons_spellbooks', 'arcane_helmet'), ('ars_nouveau:arcanist_hood', 'ars_nouveau', 'adventurer')]),
    ('Aether', 2, 40, [('aether:zanite_helmet', 'aether', 'adventurer'), ('aether:zanite_chestplate', 'aether', 'adventurer')]),
    ('Aquatic', 3, 60, []),
    ('Twilight', 4, 80, [('twilightforest:ironwood_helmet', 'twilightforest', 'adventurer'), ('twilightforest:steeleaf_chestplate', 'twilightforest', 'adventurer')]),
    ('Infernal', 6, 94, [('cataclysm:bone_reptile_helmet', 'cataclysm', 'adventurer'), ('cataclysm:bone_reptile_chestplate', 'cataclysm', 'adventurer')]),
    ('End', 7, 100, [('enderscape:drift_leggings', 'enderscape', 'adventurer'), ('the_beyond:pathfinder_boots', 'the_beyond', 'adventurer')])]
weights = [[65,25,9,1,0,0],[45,32,18,5,0,0],[25,35,28,10,2,0],[12,25,35,23,5,0],[5,15,30,35,13,2],[0,5,20,40,28,7]]
rarities = ['common','uncommon','rare','epic','legendary','mythic']
with zipfile.ZipFile(args.spiral_jar) as source:
    for index, (theme, tier, level, themed) in enumerate(themes):
        floor = index + 1
        table = f'tnk_spiral:chests/floor_{floor}'
        original = json.loads(source.read(f'data/spiral_abyss/floor_{floor}/floor.json'))
        original['rewards']['entries'] = [{'loot_table': table, 'weight': 1, 'chance': 1.0}]
        put(f'data/spiral_abyss/floor_{floor}/floor.json', original)
        put(f'data/tnk_spiral/loot_table/chests/floor_{floor}.json', {
            'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:paper',
            'functions': [{'function': 'tnkloot:dungeon_reward', 'table': table}]}]}]})
        normal = [
            {'id': 'materials', 'item': 'minecraft:emerald', 'weight': 25, 'count': {'min': 2, 'max': 5 + floor}},
            {'id': 'food', 'item': 'minecraft:cooked_beef', 'weight': 25, 'count': {'min': 2, 'max': 4}},
            {'id': 'healing', 'item': 'minecraft:potion', 'weight': 15, 'components': {'minecraft:potion_contents': {'potion': 'minecraft:strong_healing'}}},
            {'id': 'helmet', 'item': 'minecraft:iron_helmet' if floor < 3 else 'minecraft:diamond_helmet', 'weight': 15, 'preset': 'tnk_spiral:adventurer'},
            {'id': 'weapon', 'item': 'minecraft:iron_sword' if floor < 3 else 'minecraft:diamond_sword', 'weight': 10},
            {'id': 'bow', 'item': 'minecraft:bow', 'weight': 10}]
        for n, (item, mod, profile) in enumerate(themed):
            normal.append({'id': f'themed_{n}', 'item': item, 'required_mods': [mod], 'weight': 35, 'preset': 'tnk_spiral:' + profile})
        put(f'data/tnk_spiral/tnkloot/dungeon_rewards/chests/floor_{floor}.json', {
            'tier': tier, 'source_level': level, 'delivery': 'unidentified',
            'rarities': {r:w for r,w in zip(rarities,weights[index]) if w}, 'normal': normal,
            'rare': {'chance': .10, 'party_scaling': 'per_player', 'rarity': 'legendary', 'delivery': 'identified', 'entries': [
                {'id': 'blade', 'item': 'minecraft:diamond_sword', 'name': theme + " Champion's Blade", 'weight': 50, 'preset': 'tnk_spiral:rare_blade'},
                {'id': 'crown', 'item': 'minecraft:diamond_helmet', 'name': theme + " Champion's Crown", 'weight': 30, 'preset': 'tnk_spiral:rare_crown'},
                {'id': 'longbow', 'item': 'minecraft:bow', 'name': theme + " Champion's Longbow", 'weight': 20, 'preset': 'tnk_spiral:rare_bow'}]}})
files['README.txt'] = '''TNK / Spiral Abyss editable floor rewards - 1.1.3 rework

Requires Minecraft 1.21.1 / NeoForge, the REWORKED Loot 1.1.3 and its normal
companions, plus the unmodified supplied Spiral Abyss 2.0.5 JAR.
OPTIONAL SERVER-SIDE DATAPACK: install in <world>/datapacks. Restart after
replacing Loot. Apply monthly edits with /reload between floor attempts.
Spiral cancels active attempts on reload.

EDIT ONE REWARD FILE PER FLOOR:
 data/tnk_spiral/tnkloot/dungeon_rewards/chests/floor_1.json
 ... floor_2.json through floor_6.json

Each file contains normal resources/food/potions/equipment and golden rare
prizes. Weights are relative. The overview derives exact per-roll odds from
available entries, including optional-mod filtering. Wrapper loot tables and
Spiral's floor.json references stay unchanged across monthly reward edits.
Shared stats live under tnkloot/equipment_presets. Attribute bonuses append
to native armour/damage; enchantments, component data, fixed affixes and
optional random affix pools are supported. Real potion variants are shown.

Regular gear defaults to unidentified. The exact equipment and its stats
are generated once at chest opening and saved on the token. Identification
and later monthly edits cannot replace that result. delivery: identified
can be set per floor or item. Identifier hints show guaranteed bonuses and
ranges, without revealing hidden random affix rolls. No global gear pool
is consulted. Source tier/rarity does not borrow a later player's cap.

Nine-star golden chests alone roll rare prizes. chance is the overall bonus
probability, followed by weighted selection of one prize. At most ONE rare
prize is awarded across the party. With 10% and weights 50/30/20, solo odds
are 5%/3%/2%. per_player scaling uses 1-(1-p)^N; shared uses p once. The UI
labels solo odds and party scaling. Announcements still happen on pickup.
Rare prizes must use count 1 and identified delivery. Omit rare.entries to
inherit encountered enemies' linked prizes; omit rare.chance to use Spiral's
configured base chance. Adding prize candidates does not increase that chance.

Copper/iron/gold retain 1/2/3 normal rolls per eligible player. Independent
native floor item bonuses, challenge lists and XP are retained. The custom
normal list replaces other weighted table choices on its floor. Waves,
claims, arena drop suppression and saved payouts remain Spiral's systems.
Once rolled, bundles and unidentified outcomes remain fixed.

These six themes/values are EXAMPLES for editing, not final pack balance.
Vanilla fallback entries work without theme mods. Arcane/Aether/Aquatic/
Twilight/Infernal/End entries activate only with their required_mods.
Fixed bonuses and rarity survive reforging; use the matching rarity material.
Random affixes reroll using the saved floor tier and pool. Updating or deleting
the original preset does not rewrite
an existing item's reforge plan. No separate monthly scripts or Spiral JAR
replacement are needed.
'''.encode()
args.output.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(args.output,'w',zipfile.ZIP_DEFLATED) as z:
    for path,body in sorted(files.items()): z.writestr(path,body)
print('SPIRAL_EXAMPLE_OK:',args.output.name,'six editable reward files with shared presets')
