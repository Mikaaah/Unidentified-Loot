"""Fetch exact Beta 1.0 development dependencies. Nothing is embedded in the mod."""
from pathlib import Path
import hashlib,urllib.request
ROOT=Path(__file__).resolve().parents[1]
PINS={'apotheosis': {'url': 'https://www.cursemaven.com/curse/maven/mod-313970/8993983/mod-313970-8993983.jar', 'sha256': '23a8b4a21d0d8744f097c77c8a96fcba14c4b95f3b1bb542412f40dab272872e'}, 'placebo': {'url': 'https://www.cursemaven.com/curse/maven/mod-283644/9153039/mod-283644-9153039.jar', 'sha256': 'd8952c019d6e54befe37155bc9876985ec57bf73dd33872115c7a65ac6bceba0'}, 'apothic_attributes': {'url': 'https://www.cursemaven.com/curse/maven/mod-898963/9006294/mod-898963-9006294.jar', 'sha256': '17c42bb6dd5c0d820fe090e1bb5959578cd5ab7b70caf98a2a59b319fcc7e1ba'}, 'patchouli': {'url': 'https://www.cursemaven.com/curse/maven/mod-306770/7730942/mod-306770-7730942.jar', 'sha256': '959af52ed6640c316c3a8469203420be4aeea11ad6603890ba83bf48f5d9f993'}, 'curios': {'url': 'https://www.cursemaven.com/curse/maven/mod-309927/6529130/mod-309927-6529130.jar', 'sha256': 'a45df2125c26219974aba7507ffc9afe7b83acc941a386af3faacb1cc0056fde'}}
for name,pin in PINS.items():
 path=ROOT/'build-deps'/(name+'.jar');path.parent.mkdir(exist_ok=True)
 if path.exists() and hashlib.sha256(path.read_bytes()).hexdigest()==pin['sha256']:continue
 with urllib.request.urlopen(pin['url'],timeout=60) as response:data=response.read()
 if hashlib.sha256(data).hexdigest()!=pin['sha256']:raise RuntimeError('Dependency hash mismatch: '+name)
 path.write_bytes(data)
print('PINNED_BETA_BUILD_DEPENDENCIES_OK')
