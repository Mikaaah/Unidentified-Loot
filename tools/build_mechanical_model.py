"""Build original stepped machine and socket geometry aligned with Create pipe connections."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
textures={'particle':'create:block/copper_casing','copper':'create:block/copper_casing','metal':'create:block/andesite_casing','top':'create:block/depot_top','bottom':'create:block/copper_underside','lens':'create:block/smart_observer_front','pipe':'create:block/pipes'}
elements=[]
def box(name,a,b,t,faces=None):
 f={side:{'texture':'#'+t} for side in ['north','south','east','west','up','down']}
 if faces:f.update(faces)
 elements.append({'name':name,'from':a,'to':b,'faces':f})
box('cast base',[0,0,0],[16,2,16],'metal')
box('copper plinth',[1,2,1],[15,4,15],'copper')
box('inset chamber',[2,4,3],[14,12,14],'metal')
box('back casing',[2,4,12],[14,14,14],'copper')
for x in [1,12]:box('front copper pillar',[x,4,2],[x+2,13,4],'copper')
box('upper shoulder',[1,12,2],[14,14,14],'copper')
box('raised scanning tray',[3,14,4],[12,15,12],'bottom',{'up':{'texture':'#top','uv':[1,1,15,15]}})
box('left tray rail',[2,14,3],[3,16,13],'copper')
box('right tray rail',[12,14,3],[13,16,13],'copper')
box('rear tray rail',[3,14,12],[12,16,13],'copper')
box('scanner bezel',[3,6,1.5],[12,11.5,2.5],'bottom')
box('scanner lens',[4,7,1.4],[11,10.5,1.5],'metal',{'north':{'texture':'#lens','uv':[3,4,13,12]}})
box('output lip',[3,4,0.5],[12,5,3],'bottom',{'up':{'texture':'#top','uv':[2,7,14,11]}})
# Original hollow socket: pipe center (8,8), recessed neck and a four-part copper flange.
# Textures are resolved from the installed Create mod; none of its image bytes are copied.
for name,a,b,uv in [
 ('socket upper flange',[14,11,3],[16,13,13],[11,11,16,12]),
 ('socket lower flange',[14,3,3],[16,5,13],[11,15,16,16]),
 ('socket front flange',[14,5,3],[16,11,5],[15,12,16,15]),
 ('socket back flange',[14,5,11],[16,11,13],[11,12,12,15])]:
 box(name,a,b,'pipe',{'east':{'texture':'#pipe','uv':uv}})
for name,a,b in [
 ('socket upper neck',[12,11,4],[14,12,12]),('socket lower neck',[12,4,4],[14,5,12]),
 ('socket front neck',[12,5,4],[14,11,5]),('socket back neck',[12,5,11],[14,11,12])]:
 box(name,a,b,'pipe')
model={'parent':'minecraft:block/block','ambientocclusion':True,'textures':textures,'elements':elements,'display':{'gui':{'rotation':[25,225,0],'translation':[0,0,0],'scale':[.68,.68,.68]}}}
p=root/'src/main/resources/assets/tnkloot/models/block/mechanical_identifier.json';p.write_text(json.dumps(model,indent=2)+'\n')
print('MECHANICAL_MODEL_OK:',len(elements),'elements; hollow pipe socket, inset chamber and raised tray')
