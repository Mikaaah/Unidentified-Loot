"""Build only Salvage from the supplied vanilla anvil texture at native resolution.
Usage: python tools/build_salvage_cleanlook.py CleanLook_GUI_Pack.zip
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import zipfile, io, sys, json, hashlib
root=Path(__file__).resolve().parents[1]
out=root/'src/main/resources/assets/tnkloot/textures/gui/salvage';out.mkdir(parents=True,exist_ok=True)
pack=zipfile.ZipFile(sys.argv[-1])
def read(name):return Image.open(io.BytesIO(pack.read(name))).convert('RGBA')
path='assets/minecraft/textures/gui/container/anvil.png';anvil=read(path)
# These source positions are the vanilla AnvilMenu / ItemCombinerMenu coordinates.
inventory=anvil.crop((0,68,176,166))
input_frame=anvil.crop((25,45,46,67)) # item origin is 27,47 inside this source frame
W,H=256,252;im=Image.new('RGBA',(W+16,H+28))
mask=Image.new('L',im.size);md=ImageDraw.Draw(mask)
outline=[(0,10),(8,2),(40,2),(40,-10),(216,-10),(216,2),(248,2),(256,10),
 (256,134),(250,140),(250,150),(256,156),(256,246),(250,252),
 (204,252),(194,246),(184,252),(8,252),(0,244)]
# One connected shell: retain the small right and bottom dents, with no seam around Balance.
md.polygon([(x+8,y+16) for x,y in outline],fill=255)
shadow=Image.new('L',im.size);shadow.paste(mask.filter(ImageFilter.MaxFilter(9)),(1,2))
im.paste('#20293F',(0,0),shadow);im.paste('#FBFCF8',(0,0),mask.filter(ImageFilter.MaxFilter(7)));im.paste('#20293F',(0,0),mask)
inner=mask.filter(ImageFilter.MinFilter(3));surface=Image.new('RGBA',im.size);d=ImageDraw.Draw(surface)
base=anvil.getpixel((48,66))[:3]
for y in range(im.height):
 shift=round(5-10*y/(im.height-1));d.line((0,y,im.width,y),fill=tuple(max(0,min(255,v+shift)) for v in base)+(255,))
im.paste(surface,(0,0),inner)
for dx,dy,color in [(1,0,'#86A6AE'),(0,1,'#86A6AE'),(-1,0,'#304358'),(0,-1,'#304358')]:
 moved=Image.new('L',im.size);moved.paste(inner,(dx,dy));im.paste(color,(0,0),ImageChops.subtract(inner,moved))
def paste(tile,x,y):im.alpha_composite(tile,(x+8,y+16))
def nine(tile,w,h,b=2):
 dest=Image.new('RGBA',(w,h));sw,sh=tile.size
 for sx,ex,x,ww in [(0,b,0,b),(b,sw-b,b,w-2*b),(sw-b,sw,w-b,b)]:
  for sy,ey,y,hh in [(0,b,0,b),(b,sh-b,b,h-2*b),(sh-b,sh,h-b,b)]:
   dest.paste(tile.crop((sx,sy,ex,ey)).resize((ww,hh),Image.Resampling.NEAREST),(x,y))
 return dest
# Actual native title plate; only its middle is resized, its border stays native.
paste(nine(anvil.crop((57,4,159,16)),170,19),43,-7)
# Copy the native hammer pixels, retaining the anvil identity without a fake icon.
hammer=anvil.crop((7,10,38,42));alpha=hammer.getchannel('A')
for y in range(hammer.height):
 for x in range(hammer.width):
  rgb=hammer.getpixel((x,y))[:3]
  if rgb==(68,96,119):alpha.putpixel((x,y),0)
hammer.putalpha(alpha);paste(hammer,18,45)
# The copper quote plate follows the diagonal shoulder of the original anvil.
quote_mask=Image.new('L',im.size);qd=ImageDraw.Draw(quote_mask)
poly=[(62,41),(248,41),(248,83),(84,83),(62,61)]
qd.polygon([(x+8,y+16) for x,y in poly],fill=255)
im.paste('#4D2B32',(0,0),quote_mask)
edge=quote_mask.filter(ImageFilter.MinFilter(3));im.paste('#AD7757',(0,0),edge)
face=edge.filter(ImageFilter.MinFilter(3));copper=Image.new('RGBA',im.size)
strip=anvil.crop((168,17,169,41)).resize((186,42),Image.Resampling.NEAREST);copper.paste(strip,(70,57));im.paste(copper,(0,0),face)
# All work wells are copied from the source anvil frame, including their border.
for i in range(7):paste(input_frame,18+i*24,101)
# Whole inventory section, no crop-and-rebuild of individual cells, no resizing.
paste(inventory,6,154)
# Close the blank header strip while retaining all native item cells and the title tab.
d=ImageDraw.Draw(im)
for xx,yy,end,color in [(8,154,181,'#000000'),(9,155,180,'#729AA1'),(10,156,179,'#A9C3C8')]:d.line((xx+8,yy+16,end+8,yy+16),fill=color)
# Balance remains an inset panel in the same connected outer shell.
widget=read('assets/minecraft/textures/gui/sprites/widget/button.png');paste(nine(widget,57,90,3),191,154)
im.save(out/'background.png')
meta={'source':path,'source_sha256':hashlib.sha256(pack.read(path)).hexdigest(),'logical_size':[256,252],'texture_size':[272,280],'outline':outline,'balance_attached':True,'inventory_header_closed':True,'right_column':[191,248],
 'inventory_source':[0,68,176,166],'inventory_destination':[6,154],'inventory_translation':[6,86],
 'player_first_cell':[14,170],'hotbar_first_cell':[14,228],'input_source_frame':[25,45,46,67],'input_source_item_origin':[27,47],
 'input_first_cell':[20,103],'input_pitch':24}
(out/'provenance.json').write_text(json.dumps(meta,indent=2)+'\n')
sprites=root/'src/main/resources/assets/tnkloot/textures/gui/sprites/salvage';sprites.mkdir(parents=True,exist_ok=True)
for target,original in [('button','button'),('button_hover','button_highlighted'),('button_disabled','button_disabled')]:
 data=pack.read('assets/minecraft/textures/gui/sprites/widget/'+original+'.png');(sprites/(target+'.png')).write_bytes(data)
 (sprites/(target+'.png.mcmeta')).write_text(json.dumps({'gui':{'scaling':{'type':'nine_slice','width':200,'height':20,'border':3}}},indent=2)+'\n')
print('SALVAGE_NATIVE_ANVIL_OK: native inventory and source anvil work frames, no resampling')
