"""Native CleanLook parts: inventory and slot pixels are never scaled."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFilter,ImageChops
import io,json,zipfile,hashlib
class Assets:
 def __init__(self,pack):
  self.zip=zipfile.ZipFile(pack);self.anvil=self.read('assets/minecraft/textures/gui/container/anvil.png');self.widget=self.read('assets/minecraft/textures/gui/sprites/widget/button.png')
 def read(self,name):return Image.open(io.BytesIO(self.zip.read(name))).convert('RGBA')
 def export(self,root,namespace):
  folder=Path(root)/'textures/gui/sprites/cleanlook';folder.mkdir(parents=True,exist_ok=True)
  for target,source in [('button','button'),('button_hover','button_highlighted'),('button_disabled','button_disabled')]:
   (folder/(target+'.png')).write_bytes(self.zip.read('assets/minecraft/textures/gui/sprites/widget/'+source+'.png'))
   (folder/(target+'.png.mcmeta')).write_text(json.dumps({'gui':{'scaling':{'type':'nine_slice','width':200,'height':20,'border':3}}},indent=2)+'\n')
  copper=Image.new('RGBA',(48,32),'#4D2B32');d=ImageDraw.Draw(copper);d.rectangle((1,1,46,30),fill='#C09473');copper.paste(self.anvil.crop((168,17,169,41)).resize((44,28),Image.Resampling.NEAREST),(2,2));d.line((2,29,45,29),fill='#693D38');d.line((45,2,45,29),fill='#693D38');copper.save(folder/'panel_copper.png')
  (folder/'panel_copper.png.mcmeta').write_text(json.dumps({'gui':{'scaling':{'type':'nine_slice','width':48,'height':32,'border':2}}},indent=2)+'\n')
  gui=Path(root)/'textures/gui/cleanlook';gui.mkdir(parents=True,exist_ok=True)
  (gui/'generic_54.png').write_bytes(self.zip.read('assets/minecraft/textures/gui/container/generic_54.png'))
  self.anvil.crop((25,45,46,67)).save(gui/'slot.png');self.anvil.crop((8,84,24,100)).save(gui/'inventory_cell.png');self.anvil.crop((8,142,24,158)).save(gui/'hotbar_cell.png')
  (gui/'provenance.json').write_text(json.dumps({'source':'CleanLook_GUI_Pack.zip','anvil_sha256':hashlib.sha256(self.zip.read('assets/minecraft/textures/gui/container/anvil.png')).hexdigest(),'namespace':namespace,'resampling':'Panel interiors only; inventory and item frames use native pixels'},indent=2)+'\n')
def nine(tile,w,h,b=2):
 dest=Image.new('RGBA',(w,h));sw,sh=tile.size
 for sx,ex,x,ww in [(0,b,0,b),(b,sw-b,b,w-2*b),(sw-b,sw,w-b,b)]:
  for sy,ey,y,hh in [(0,b,0,b),(b,sh-b,b,h-2*b),(sh-b,sh,h-b,b)]:dest.paste(tile.crop((sx,sy,ex,ey)).resize((ww,hh),Image.Resampling.NEAREST),(x,y))
 return dest
class Canvas:
 def __init__(self,assets,w,h):self.assets=assets;self.im=Image.new('RGBA',(w+16,h+28));self.w=w;self.h=h
 def rect(self,x,y,w,h,c):ImageDraw.Draw(self.im).rectangle((x+8,y+16,x+w+7,y+h+15),fill=c)
 def paste(self,tile,x,y):self.im.alpha_composite(tile,(x+8,y+16))
 def shell(self,title=(42,180),outline=None):
  x,pw=title;w,h=self.w,self.h
  pts=outline or [(0,10),(8,2),(x-3,2),(x-3,-10),(x+pw+3,-10),(x+pw+3,2),(w-8,2),(w,10),(w,h//2-6),(w-4,h//2),(w,h//2+6),(w,h-6),(w-6,h),(w//2+6,h),(w//2,h-4),(w//2-6,h),(6,h),(0,h-6)]
  mask=Image.new('L',self.im.size);ImageDraw.Draw(mask).polygon([(a+8,b+16) for a,b in pts],fill=255)
  shadow=Image.new('L',mask.size);shadow.paste(mask.filter(ImageFilter.MaxFilter(9)),(1,2));self.im.paste('#20293F',(0,0),shadow);self.im.paste('#FBFCF8',(0,0),mask.filter(ImageFilter.MaxFilter(7)));self.im.paste('#20293F',(0,0),mask)
  inner=mask.filter(ImageFilter.MinFilter(3));surface=Image.new('RGBA',mask.size);d=ImageDraw.Draw(surface);base=self.assets.anvil.getpixel((48,66))[:3]
  for y in range(surface.height):
   shift=round(5-10*y/max(1,surface.height-1));d.line((0,y,surface.width,y),fill=tuple(max(0,min(255,v+shift)) for v in base)+(255,))
  self.im.paste(surface,(0,0),inner)
  for dx,dy,color in [(1,0,'#86A6AE'),(0,1,'#86A6AE'),(-1,0,'#304358'),(0,-1,'#304358')]:
   moved=Image.new('L',mask.size);moved.paste(inner,(dx,dy));self.im.paste(color,(0,0),ImageChops.subtract(inner,moved))
  self.paste(nine(self.assets.anvil.crop((57,4,159,16)),pw,19),x,-7)
 def panel(self,x,y,w,h,blue=False):
  if blue:self.paste(nine(self.assets.widget,w,h,3),x,y);return
  self.rect(x,y,w,h,'#4D2B32');self.rect(x+1,y+1,w-2,h-2,'#C09473');self.paste(self.assets.anvil.crop((168,17,169,41)).resize((w-4,h-4),Image.Resampling.NEAREST),x+2,y+2);self.rect(x+2,y+h-3,w-4,1,'#693D38');self.rect(x+w-3,y+2,1,h-4,'#693D38')
 def inventory(self,x,y):
  self.paste(self.assets.anvil.crop((0,68,176,166)),x-8,y-16)
  self.rect(x-6,y-16,174,1,'#000000');self.rect(x-5,y-15,172,1,'#729AA1');self.rect(x-4,y-14,170,1,'#A9C3C8')
 def slot(self,x,y):self.paste(self.assets.anvil.crop((25,45,46,67)),x-2,y-2)
 def grid(self,x,y,cols,rows,pitchx,pitchy):
  for r in range(rows):
   for c in range(cols):self.slot(x+c*pitchx,y+r*pitchy)
 def save(self,root,name):
  p=Path(root)/'textures/gui'/f'{name}.png';p.parent.mkdir(parents=True,exist_ok=True);self.im.save(p)
