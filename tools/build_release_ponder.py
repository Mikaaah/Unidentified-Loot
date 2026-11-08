from pathlib import Path
import nbtlib
from nbtlib import Compound,List,Int,String,Double,Float
root=Path(__file__).resolve().parents[1]
dest=root/'src/main/resources/assets/tnkloot/ponder';dest.mkdir(parents=True,exist_ok=True)
def structure(name,blocks,size):
 palette=[];entries=[]
 for pos,block,props in blocks:
  value=Compound({'Name':String(block)})
  if props:value['Properties']=Compound({k:String(v) for k,v in props.items()})
  if value not in palette:palette.append(value)
  entries.append(Compound({'pos':List[Int](pos),'state':Int(palette.index(value))}))
 nbt=nbtlib.File({'DataVersion':Int(3955),'size':List[Int](size),'palette':List[Compound](palette),'blocks':List[Compound](entries),'entities':List[Compound]([])})
 nbt.save(dest/(name+'.nbt'),gzipped=True)
base=[((x,0,z),'minecraft:dark_oak_planks',{}) for x in range(5) for z in range(5)]
work=base+[((2,1,2),'tnkloot:identifier',{'facing':'north'}),((0,1,2),'create:depot',{'waterlogged':'false'}),((4,1,2),'create:depot',{'waterlogged':'false'}),((0,1,4),'minecraft:oak_fence',{'east':'false','west':'false','north':'false','south':'false','waterlogged':'false'}),((0,2,4),'minecraft:lantern',{'hanging':'false','waterlogged':'false'})]
structure('loot_workshop',work,[5,3,5])
mechanical=base+[((2,2,2),'tnkloot:mechanical_identifier',{'facing':'north'}),((2,3,2),'minecraft:hopper',{'facing':'down','enabled':'true'}),((2,4,2),'minecraft:chest',{'facing':'north','type':'single','waterlogged':'false'}),((2,1,2),'minecraft:hopper',{'facing':'west','enabled':'true'}),((1,1,2),'create:depot',{'waterlogged':'false'}),((3,2,2),'create:fluid_pipe',{'east':'true','west':'true','up':'false','down':'false','north':'false','south':'false','waterlogged':'false'}),((4,2,2),'create:fluid_tank',{'top':'true','bottom':'true','shape':'window'}),((4,1,2),'minecraft:cut_copper',{})]
structure('mechanical_workshop',mechanical,[5,5,5])
print('Built compact Identifier and Mechanical Identifier scenes')
# Ponder numbers generated text components from 1, including the final caption.
import json,re
source=(root/'src/main/java/dev/mikaaah/tnkloot/client/ponder/LootScenes.java').read_text()
lang=root/'src/main/resources/assets/tnkloot/lang/en_us.json';values=json.loads(lang.read_text())
for method,title in [('loot','loot_basics'),('identify','identifying'),('salvage','salvaging'),('mechanical','mechanical_identifying')]:
 body=source.split('public static void '+method+'(',1)[1].split('\n    public static void ',1)[0]
 for key in list(values):
  if key.startswith('tnkloot.ponder.'+title+'.text_'):del values[key]
 header=re.search(r'scene.title\("[^"]+", "([^"]+)"\)',body).group(1)
 values['tnkloot.ponder.'+title+'.header']=header
 for number,caption in enumerate(re.findall(r'caption\(scene, "((?:[^"\\]|\\.)*)"',body),1):
  values[f'tnkloot.ponder.{title}.text_{number}']=caption
lang.write_text(json.dumps(values,indent=2,ensure_ascii=False)+'\n')
