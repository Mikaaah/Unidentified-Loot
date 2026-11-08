"""Fetch the pinned, unmodified Create build dependency and extract its bundled Ponder API.
These dependencies are for compilation/tests only and are never embedded in tnkloot.
"""
from pathlib import Path
import hashlib,urllib.request,zipfile
ROOT=Path(__file__).resolve().parents[1];dest=ROOT/'build-deps';dest.mkdir(parents=True,exist_ok=True)
p=dest/'create-1.21.1-6.0.10.jar'
expected='11cc8fc049d2f67f6548c7abfada6b82a3adb5c7ca410a742de04bbca76e03862c518721b88d806f6e6d768a4d68531fdb903a85859b25d1484d550cc7bafd4b'
if not p.exists() or hashlib.sha512(p.read_bytes()).hexdigest()!=expected:
 with urllib.request.urlopen('https://cdn.modrinth.com/data/LNytGWDc/versions/UjX6dr61/create-1.21.1-6.0.10.jar',timeout=90) as r:data=r.read()
 assert hashlib.sha512(data).hexdigest()==expected;p.write_bytes(data)
with zipfile.ZipFile(p) as z:(dest/'ponder-1.0.82.jar').write_bytes(z.read('META-INF/jarjar/ponder-neoforge-1.0.82+mc1.21.1.jar'))
print('PONDER_BUILD_DEPENDENCIES_OK: Create 6.0.10 / bundled Ponder 1.0.82')
