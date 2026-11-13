"""Verify release contents against the actual TNK Beta 1.0 / 0.19.15 assets."""
from pathlib import Path
import hashlib,json,sys,zipfile,re
from asset_validation import validate
ROOT=Path(__file__).resolve().parents[1]
jar=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'build/libs/tnkloot-1.1.3-tnk-neoforge-1.21.1.jar'
with zipfile.ZipFile(jar) as archive:
 data={n:archive.read(n) for n in archive.namelist() if not n.endswith('/')}
models,textures=validate(data,ROOT/'verification/reference/rarity-textures.zip')
reference=json.loads((ROOT/'verification/reference/0.19.15-images.json').read_text())
gui_reference=json.loads((ROOT/'verification/reference/1.1.2-gui-images.json').read_text())
for name,digest in reference.items():
 assert name in data,f'Missing Beta 1.0 artwork: {name}'
 assert hashlib.sha256(data[name]).hexdigest()==gui_reference.get(name,digest),f'Unexpected artwork change: {name}'
for name in ['client/IdentifierScreen','client/SalvageScreen','client/PreviewScreen','client/CollectionScreen',
 'client/ponder/OptionalPonderClient','client/ponder/LootPonderPlugin','client/ponder/LootScenes',
 'boss/BossEditor','boss/BossItems','features/BossSignatures','workshop/LootJournal',
 'dimension/DimensionLoot','injection/mixin/CommonHooksMixin','injection/mixin/LootParamsMixin',
 'injection/mixin/LootParamsBuilderMixin','injection/mixin/LootContextAccess','loot/BetaLoot',
 'machine/MechanicalIdentifierEntity','compat/NativeLootTemplates','compat/EquipmentAudit']:
 assert f'dev/mikaaah/tnkloot/{name}.class' in data,name
lang=json.loads(data['assets/tnkloot/lang/en_us.json'])
source=(ROOT/'src/main/java/dev/mikaaah/tnkloot/client/ponder/LootScenes.java').read_text()
for method,title in [('loot','loot_basics'),('identify','identifying'),('salvage','salvaging'),('mechanical','mechanical_identifying')]:
 body=source.split('public static void '+method+'(',1)[1].split('\n    public static void ',1)[0]
 captions=re.findall(r'caption\(scene, "((?:[^"\\]|\\.)*)"',body)
 for number,caption in enumerate(captions,1):assert lang.get(f'tnkloot.ponder.{title}.text_{number}')==caption,f'Ponder caption {title}/{number}'
mixins=json.loads(data['tnkloot.loot.mixins.json'])
assert {'CommonHooksMixin','LootContextAccess','LootParamsMixin','LootParamsBuilderMixin'}.issubset(mixins['mixins'])
assert len([name for name in mixins['mixins'] if name.startswith('Apoth')])==13
assert {'ApothAffixIngredientMixin','ApothAffixLootModifierMixin','ApothAffixLootRegistryMixin','ApothAffixTradeMixin'}.issubset(mixins['mixins'])
legendary=json.loads(data['data/tnkloot/recipe/salvaging/affix_item/legendary.json'])
assert legendary['input']['rarity']=='tnkloot:legendary' and legendary['outputs'][0]['stack']['id']=='minecraft:echo_shard'
assert not any(n.startswith(('net/minecraft/','net/neoforged/','com/simibubi/','net/createmod/','dev/mikaaah/tnkprogression/')) for n in data)
assert not any('PonderClientCheck' in n or 'ClientVisualCheck' in n for n in data)
assert sum(n.startswith('data/tnkloot/affixes/compat/') and n.endswith('.json') for n in data)==342
assert 'META-INF/licenses/irons_apothic-LICENSE' in data and 'THIRD_PARTY_NOTICES.md' in data
print(f'RELEASE_ASSETS_OK: {len(reference)-len(gui_reference)} unchanged reference images; {len(gui_reference)} corrected GUI textures; {models} valid models, {textures} exact rarity icons, four origin mixins, thirteen Apotheosis integration mixins, Legendary salvage, 342 optional affixes, no bundled APIs/test client')
