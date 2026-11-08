"""Rebuild all Loot menus from CleanLook, preserving real item slot coordinates.
Usage: python tools/build_menu_assets.py /path/CleanLook_GUI_Pack.zip
"""
from pathlib import Path
import sys,subprocess
from cleanlook_assets import Assets,Canvas
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/tnkloot'
subprocess.run([sys.executable,str(Path(__file__).with_name('build_salvage_cleanlook.py')),sys.argv[1]],check=True)
a=Assets(sys.argv[1]);a.export(root,'tnkloot')
c=Canvas(a,264,222);c.shell((41,182));c.panel(15,42,186,62);c.panel(199,126,49,86,True);c.grid(25,57,7,1,24,24);c.rect(22,88,172,9,'#20293F');c.inventory(25,134);c.save(root,'identifier_counter')
c=Canvas(a,312,222);c.shell((49,214));c.panel(16,67,280,122);c.grid(32,78,9,4,29,27);c.save(root,'reward_preview')
c=Canvas(a,312,222);c.shell((17,127));c.panel(12,43,134,145,True);c.panel(164,17,138,172);c.grid(174,47,6,6,20,23);c.save(root,'collection')
c=Canvas(a,304,250);c.shell((8,288));c.save(root,'tier_vault')
c=Canvas(a,304,250);c.shell((8,288));c.panel(8,44,176,80);c.grid(16,49,9,4,18,18);c.inventory(16,166);c.save(root,'tier_vault_items')
c=Canvas(a,312,260);c.shell((43,226),[(14,10),(20,4),(40,4),(40,-10),(272,-10),(272,4),(298,4),(304,10),(304,137),(298,143),(304,149),(304,248),(298,254),(205,254),(199,248),(193,254),(20,254),(14,248),(14,149),(8,143),(14,137)])
c.panel(19,29,179,98);c.grid(27,48,7,1,24,24);c.grid(27,91,7,1,24,24);c.rect(26,117,162,7,'#20293F');c.rect(22,129,174,13,'#20293F');c.panel(209,29,84,112,True);c.panel(233,47,36,63,True);c.rect(235,49,32,59,'#20293F');c.inventory(27,166);c.panel(209,149,84,99,True);c.rect(216,168,70,1,'#729AA1');c.rect(216,214,70,1,'#729AA1');c.save(root,'mechanical_identifier')
print('CLEANLOOK_ALL_LOOT_OK: native inventories, item frames and widgets')
