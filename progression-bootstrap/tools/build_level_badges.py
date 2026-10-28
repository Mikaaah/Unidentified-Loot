#!/usr/bin/env python3
"""Exact 0–100 font glyphs. Original icon-free pixel art; Pillow is build-time only."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/tnkprogression'
BITS = {
'L':'100 100 100 100 111', 'V':'101 101 101 101 010',
'0':'111 101 101 101 111', '1':'010 110 010 010 111',
'2':'110 001 010 100 111', '3':'110 001 010 001 110',
'4':'101 101 111 001 001', '5':'111 100 110 001 110',
'6':'011 100 111 101 111', '7':'111 001 010 010 010',
'8':'111 101 111 101 111', '9':'111 101 111 001 110'}
def badge(level):
    im = Image.new('RGBA', (36, 9)); d = ImageDraw.Draw(im)
    d.rectangle((0,0,34,8), fill='#2b2017'); d.rectangle((1,1,33,7), fill='#886b3f')
    d.line((1,1,33,1), fill='#bd9c5d'); d.line((1,7,33,7), fill='#55402a')
    d.rectangle((2,2,32,6), fill='#50402d')
    label = f'LVL {level}'; width = sum(3 if c == ' ' else 4 for c in label)-1; x=(35-width)//2
    for c in label:
        if c == ' ': x+=3; continue
        for y,row in enumerate(BITS[c].split()):
            for dx,bit in enumerate(row):
                if bit=='1': d.point((x+dx,y+2), fill='#fff0c9')
        x+=4
    return im
atlas=Image.new('RGBA',(36*16,9*7))
for n in range(101): atlas.paste(badge(n),((n%16)*36,(n//16)*9))
p=ASSETS/'textures/font/level_badges.png'; p.parent.mkdir(parents=True,exist_ok=True); atlas.save(p)
rows=[''.join(chr(0xE000+n) if n<=100 else '\x00' for n in range(r*16,r*16+16)) for r in range(7)]
p=ASSETS/'font/level_badges.json'; p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(json.dumps({'providers':[{'type':'bitmap','file':'tnkprogression:font/level_badges.png','height':9,'ascent':7,'chars':rows}]},indent=2)+'\n')
preview=Image.new('RGB',(760,410),'#252720'); d=ImageDraw.Draw(preview)
def font(size,mono=False): return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans'+('Mono' if mono else '')+'.ttf',size)
d.text((28,20),'TNK character levels',font=font(18),fill='#e8d9b9')
d.text((28,49),'Placement mockup · actual badge pixels enlarged',font=font(13),fill='#a79f8b')
d.rounded_rectangle((22,89,738,167),radius=5,fill='#151712')
d.text((33,98),'CHAT',font=font(13),fill='#a79f8b')
p=badge(42).resize((108,27),Image.Resampling.NEAREST); preview.paste(p,(35,124),p)
d.text((153,124),'Mikaaah: Ready for the next quest?',font=font(17,True),fill='#eee9dc')
d.text((33,191),'ABOVE PLAYER',font=font(13),fill='#a79f8b')
d.rectangle((220,224,520,260),fill='#171a15'); preview.paste(p,(238,227),p)
d.text((356,226),'Mikaaah',font=font(21,True),fill='#eee9dc')
d.rectangle((354,274,386,305),fill='#ad8e69'); d.rectangle((345,309,395,347),fill='#63756d')
d.text((33,367),'All 101 values: 0–100 · no icons · same simple bronze style',font=font(13),fill='#b9ae98')
preview.save(ROOT/'Level-Badges-Preview.png')
print('Built 101 level badges, font and placement preview.')
