"""Build the current CleanLook machine GUI and import the supplied mixed-case font.
Usage: python tools/build_mechanical_gui_assets.py CleanLook_GUI_Pack.zip
The former SVG generator is superseded; this entry point uses the shared native builder.
"""
from pathlib import Path
from PIL import Image,ImageDraw
import subprocess,sys,json,shutil
root=Path(__file__).resolve().parents[1]
if len(sys.argv)!=2:raise SystemExit(__doc__)
subprocess.run([sys.executable,str(root/'tools/build_menu_assets.py'),sys.argv[1]],check=True)
assets=root/'src/main/resources/assets/tnkloot'
glass=Image.new('RGBA',(32,59));d=ImageDraw.Draw(glass)
for y in (10,22,34,46):d.line((28,y,31,y),fill=(114,154,161,136))
glass.save(assets/'textures/gui/mechanical_identifier_glass.png')
shutil.copyfile(root/'tools/references/menu-font.png',assets/'textures/font/menu.png')
font=json.loads((root/'tools/references/menu-font.json').read_text())
for provider in font['providers']:
 if provider['type']=='bitmap':provider['file']='tnkloot:font/menu.png'
(assets/'font/menu.json').write_text(json.dumps(font,indent=2)+'\n')
