"""Fetch the exact Beta 1.0 optional loot integrations for server regression checks."""
from pathlib import Path
import hashlib,json,urllib.request
ROOT=Path(__file__).resolve().parents[1]
destination=ROOT/'build-deps/loot-audit-runtime';destination.mkdir(exist_ok=True)
for pin in json.loads((ROOT/'verification/loot-pools/runtime-pins.json').read_text()):
    target=destination/(pin['name']+'.jar')
    if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest()==pin['sha256']:continue
    project,file=pin['project_id'],pin['file_id']
    with urllib.request.urlopen(f'https://www.cursemaven.com/curse/maven/mod-{project}/{file}/mod-{project}-{file}.jar',timeout=120) as response:data=response.read()
    if hashlib.sha256(data).hexdigest()!=pin['sha256']:raise RuntimeError('Loot audit dependency checksum mismatch: '+pin['name'])
    target.write_bytes(data)
print('PINNED_LOOT_AUDIT_RUNTIME_OK')
