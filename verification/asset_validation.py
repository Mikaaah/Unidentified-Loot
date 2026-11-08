"""Validate the dependency graph traversed by client BlockModel.resolveParents."""
import json,zipfile
from pathlib import PurePosixPath

def validate(data,texture_pack):
    models={}
    for path,raw in data.items():
        if path.startswith('assets/tnkloot/models/') and path.endswith('.json'):
            models['tnkloot:'+path.removeprefix('assets/tnkloot/models/').removesuffix('.json')]=json.loads(raw)
    graph={}
    for key,model in models.items():
        refs=[model.get('parent','')]+[o['model'] for o in model.get('overrides',[])]
        graph[key]=[ref for ref in refs if ref.startswith('tnkloot:')]
        for ref in graph[key]:
            if ref not in models:raise ValueError(f'Missing model: {key} -> {ref}')
        for ref in model.get('textures',{}).values():
            if ref.startswith('tnkloot:') and 'assets/tnkloot/textures/'+ref.split(':',1)[1]+'.png' not in data:
                raise ValueError(f'Missing texture: {key} -> {ref}')
    visiting=set();visited=set()
    def walk(key,chain):
        if key in visiting:raise ValueError('Model cycle: '+' -> '.join(chain+[key]))
        if key in visited:return
        visiting.add(key)
        for ref in graph[key]:walk(ref,chain+[key])
        visiting.remove(key);visited.add(key)
    for key in graph:walk(key,[])
    count=0
    with zipfile.ZipFile(texture_pack) as source:
        for name in source.namelist():
            if name.count('/')!=1 or not name.endswith('.png'):continue
            rarity,filename=name.split('/');stem=PurePosixPath(filename).stem
            path='assets/tnkloot/textures/item/rarity/'+name
            if data.get(path)!=source.read(name):raise ValueError('Modified PNG: '+name)
            key=f'tnkloot:item/rarity/{rarity}/{stem}'
            expected={'parent':'minecraft:item/generated','textures':{'layer0':f'tnkloot:item/rarity/{rarity}/{stem}'}}
            if models.get(key)!=expected:raise ValueError('Invalid rarity leaf: '+key)
            count+=1
    if count!=48:raise ValueError(f'Expected 48 rarity leaves, got {count}')
    return len(models),count
