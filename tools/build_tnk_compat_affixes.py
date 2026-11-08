"""Generate TNK-rarity adapters from the exact installed compatibility JARs.
Only definitions and translations under their declared MIT / CC-BY licences are adapted;
no textures, models or third-party classes are copied.
"""
from pathlib import Path
import json,zipfile,sys,copy
root=Path(__file__).resolve().parents[1]
index=json.loads(Path(sys.argv[1]).read_text())
langpath=root/'src/main/resources/assets/tnkloot/lang/en_us.json';lang=json.loads(langpath.read_text())
quality={'common':'common','uncommon':'uncommon','rare':'rare','epic':'epic','legendary':'mythic','mythic':'mythic'}
count=0
for namespace in ['irons_apothic','apothic_compats']:
 with zipfile.ZipFile(index[namespace]['path']) as z:
  original_lang=json.loads(z.read('assets/'+namespace+'/lang/en_us.json'))
  prefix='data/'+namespace+'/affixes/'
  for name in z.namelist():
   if not name.startswith(prefix) or not name.endswith('.json'):continue
   path=name[len(prefix):-5]
   if '/ancient/' in path or path.startswith('ancient/'):continue
   data=json.loads(z.read(name));new=copy.deepcopy(data)
   if 'values' not in data and 'rarities' not in data:continue
   if isinstance(data.get('values'),dict):
    values={};source=data['values']
    fallback=next((source['apotheosis:'+q] for q in ['common','uncommon','rare','epic','mythic'] if 'apotheosis:'+q in source),None)
    for target,old in quality.items():
     if 'apotheosis:'+old in source:values['tnkloot:'+target]=copy.deepcopy(source['apotheosis:'+old])
     elif target=='common' and data.get('definition',{}).get('affix_type')=='stat' and fallback is not None:values['tnkloot:'+target]=copy.deepcopy(fallback)
    if not values:continue
    new['values']=values
   if 'rarities' in data:new['rarities']=['tnkloot:'+q for q,old in quality.items() if 'apotheosis:'+old in data['rarities']]
   new['neoforge:conditions']=[{'type':'neoforge:mod_loaded','modid':namespace}]+data.get('neoforge:conditions',[])
   def remap(value):
    if isinstance(value,list):return [remap(x) for x in value]
    if isinstance(value,dict):return {k:remap(v) if k=='exclusive_set' else v for k,v in value.items()}
    if isinstance(value,str) and value.startswith(namespace+':'):return 'tnkloot:compat/'+namespace+'/'+value.split(':',1)[1]
    return value
   if 'definition' in new:new['definition']=remap(new['definition'])
   out=root/'src/main/resources/data/tnkloot/affixes/compat'/namespace/(path+'.json');out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(new,indent=2)+'\n')
   oldkey='affix.'+namespace+':'+path;newkey='affix.tnkloot:compat/'+namespace+'/'+path
   for key,value in original_lang.items():
    if key==oldkey or key.startswith(oldkey+'.'):lang[newkey+key[len(oldkey):]]=value
   count+=1
langpath.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')
print('Generated',count,'optional TNK-rarity affix definitions')
