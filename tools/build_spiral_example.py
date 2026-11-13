"""Build an opt-in TNK floor-reward datapack from the supplied Spiral 2.0.5 JAR."""
from pathlib import Path
import argparse
import json
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("spiral_jar", type=Path)
parser.add_argument("output", type=Path)
args = parser.parse_args()

tiers = [1, 2, 3, 4, 6, 7]
levels = [20, 40, 60, 80, 94, 100]
rarities = ["common", "uncommon", "rare", "epic", "legendary", "mythic"]
weights = [[65, 25, 9, 1, 0, 0], [45, 32, 18, 5, 0, 0], [25, 35, 28, 10, 2, 0],
           [12, 25, 35, 23, 5, 0], [5, 15, 30, 35, 13, 2], [0, 5, 20, 40, 28, 7]]
files = {}

def put(path, value):
    files[path] = (json.dumps(value, indent=2) + "\n").encode()

put("pack.mcmeta", {"pack": {"pack_format": 48, "description": "TNK / Spiral Abyss example floor rewards (Unidentified Loot 1.1.3+)"}})
categories = {"weapon": 4, "bow": 2, "tool": 2, "helmet": 1, "chestplate": 1, "leggings": 1, "boots": 1, "accessory": 2}
put("data/tnk_spiral/loot_table/equipment_category.json", {
    "type": "minecraft:chest", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "tnkloot:unidentified_" + category, "weight": weight}
        for category, weight in categories.items()]}]})

with zipfile.ZipFile(args.spiral_jar) as source:
    for index in range(6):
        floor = index + 1
        definition = json.loads(source.read(f"data/spiral_abyss/floor_{floor}/floor.json"))
        bonuses = [entry for entry in definition["rewards"]["entries"] if "item" in entry]
        definition["rewards"]["entries"] = [{"loot_table": f"tnk_spiral:chests/floor_{floor}", "weight": 1, "chance": 1.0}, *bonuses]
        put(f"data/spiral_abyss/floor_{floor}/floor.json", definition)
        put(f"data/tnk_spiral/loot_table/chests/floor_{floor}.json", {
            "type": "minecraft:chest",
            "pools": [
                {"rolls": 1, "entries": [
                    {"type": "minecraft:loot_table", "value": "tnk_spiral:equipment_category", "weight": weight,
                     "functions": [{"function": "tnkloot:seal_reward", "tier": tiers[index], "source_level": levels[index], "rarity": rarity}]}
                    for rarity, weight in zip(rarities, weights[index]) if weight > 0]},
                {"rolls": 1, "entries": [
                    {"type": "minecraft:item", "name": "minecraft:emerald", "weight": 5,
                     "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1 + floor, "max": 3 + floor}}]},
                    {"type": "minecraft:item", "name": "minecraft:iron_ingot", "weight": 5},
                    {"type": "minecraft:item", "name": "minecraft:diamond", "weight": floor}
                ]}
            ]})

files["README.txt"] = ("""TNK / Spiral Abyss floor rewards - OPTIONAL BALANCING EXAMPLE

Requires Minecraft 1.21.1 / NeoForge, Unidentified Loot 1.1.3 or newer,
its normal dependencies, and Spiral Abyss 2.0.5. Server-side datapack.
Install this ZIP in <world>/datapacks and run /reload.

This example REPLACES the normal reward-table choices on floors 1-6.
The original challenge lists and XP are copied from the supplied JAR.
Enemy waves, bonus rare boss rewards, and Endless Mode are not edited.
Existing saved/opened chest bundles retain their existing contents.

Floor  Tier  Source level  Common/Uncommon/Rare/Epic/Legendary/Mythic (%)
1      1     20            65/25/9/1/0/0
2      2     40            45/32/18/5/0/0
3      3     60            25/35/28/10/2/0
4      4     80            12/25/35/23/5/0
5      6     94            5/15/30/35/13/2
6      7     100           0/5/20/40/28/7

Each table roll yields one sealed equipment reward and one material roll.
Copper/iron/gold chests use Spiral's existing 1/2/3 rolls per player.
Equipment weights: weapon 4, bow 2, tool 2, each armour piece 1,
accessory 2. Rewards use the equipment pools allowed in the origin dimension.
These values are a starting point for pack balancing, not final balance.

The seal_reward function accepts tier 0-7, source_level 1-100, and
common/uncommon/rare/epic/legendary/mythic. It applies to regular TNK
unidentified equipment; boss crates and non-equipment items are unchanged.
Use loot-pool rolls for multiple independent items. Do not stack several
unidentified items with set_count: functions operate on one stack.

Spiral does not pass an individual player in its loot context. These are
explicit floor rewards: a later player's progression does not lower or reroll
them. A personal progression cap requires separate player-aware integration.
Do not reset Spiral progression merely to install this pack.
""").encode()
args.output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(args.output, "w", zipfile.ZIP_DEFLATED) as output:
    for path, body in sorted(files.items()):
        output.writestr(path, body)
print(f"SPIRAL_EXAMPLE_OK: {args.output.name}; six floors, six rarity distributions")
