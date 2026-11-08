"""Download the actual Beta 1.0 companion for CI; never substitute the legacy bootstrap."""
from pathlib import Path
import hashlib,urllib.request
path=Path(__file__).resolve().parents[1]/'build-deps/tnkprogression-1.2.0-private.1.jar'
expected='739fed011982d9f9f236ab37531f9c23fdb63f1173e2f1537f9ebd5fdfc8d6a0'
if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest()!=expected:
 path.parent.mkdir(exist_ok=True)
 with urllib.request.urlopen('https://www.cursemaven.com/curse/maven/mod-1789363/9497972/mod-1789363-9497972.jar',timeout=60) as response:data=response.read()
 if hashlib.sha256(data).hexdigest()!=expected:raise RuntimeError('Companion checksum mismatch')
 path.write_bytes(data)
print('PINNED_PROGRESSION_OK: actual TNK Progression 1.2.0-private.1')
