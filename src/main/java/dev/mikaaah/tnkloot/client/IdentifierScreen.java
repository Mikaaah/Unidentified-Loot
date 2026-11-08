package dev.mikaaah.tnkloot.client;
import dev.mikaaah.tnkloot.menu.TnkMenu;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.workshop.ExperienceCosts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class IdentifierScreen extends AbstractContainerScreen<TnkMenu> {
 private Button identify,salvage,preview,guide;
 public IdentifierScreen(TnkMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=264;imageHeight=222;inventoryLabelY=titleLabelY=10000;}
 private Button button(int x,int y,int w,int h,String label,int action,String help){var widget=new SalvageButton(leftPos+x,topPos+y,w,h,label,action<0,false,b->send(action));widget.setTooltip(Tooltip.create(Component.literal(help)));return addRenderableWidget(widget);}
 @Override protected void init(){super.init();topPos=StationHeader.top(height);var active=button(16,StationHeader.NAV_Y,55,StationHeader.NAV_HEIGHT,"Identify",-1,"Reveal equipment at this counter");active.active=false;salvage=button(74,StationHeader.NAV_Y,55,StationHeader.NAV_HEIGHT,"Salvage",90,"Return the counter's gear and open the salvage anvil");preview=button(132,StationHeader.NAV_Y,55,StationHeader.NAV_HEIGHT,"Preview",92,"Inspect the actual reward pool without consuming your drop");guide=button(190,StationHeader.NAV_Y,58,StationHeader.NAV_HEIGHT,"Journal",91,"Open the Loot Journal and collection");identify=button(207,53,48,20,"Identify",0,"Confirm identification");update();}
 private void send(int id){if(id>=0&&minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
 private void update(){if(identify==null)return;identify.active=!menu.busy()&&menu.affordable()&&menu.inputCount()>0;salvage.active=preview.active=guide.active=!menu.busy();identify.setTooltip(Tooltip.create(Component.literal("Identify "+menu.inputCount()+" item(s) for "+menu.quotedCost()+(ExperienceCosts.xpMode(menu)?" XP points":" "+menu.currency().getDescription().getString())+". Salvage credit has already been applied.")));}
 @Override protected void containerTick(){super.containerTick();update();}
 @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){MarketTheme.counter(g,leftPos,topPos,false);if(menu.busy()){int i=Math.clamp(menu.rollingIndex(),0,6);g.renderOutline(leftPos+23+i*24,topPos+55,20,20,0xFFFFDD70);}}
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){
  StationHeader.draw(g,font,"Item Identifier",41,182);
  String status=menu.busy()?"Revealing equipment…":menu.inputCount()>0?"Cost: "+menu.quotedCost()+(ExperienceCosts.xpMode(menu)?" XP points":" currency"):"Place unidentified items in the chest";
  MarketTheme.center(g,font,MarketTheme.fit(font,status,232),132,110,menu.inputCount()>0&&!menu.affordable()&&!menu.busy()?0xFFFFB093:MarketTheme.LIGHT);

  MarketTheme.center(g,font,menu.busy()?"Identifying...":menu.inputCount()>0?menu.inputCount()+" items prepared":"Ready for unidentified loot",108,89,MarketTheme.SOFT);
  g.drawString(font,"Inventory",25,122,MarketTheme.SOFT,false);
  MarketTheme.center(g,font,"XP",223,139,MarketTheme.INK);MarketTheme.center(g,font,MarketTheme.fit(font,Integer.toString(ExperienceCosts.xp(menu)),41),223,152,MarketTheme.GREEN);
  MarketTheme.center(g,font,"Credit",223,176,MarketTheme.INK);MarketTheme.center(g,font,MarketTheme.fit(font,Integer.toString(ExperienceCosts.credit(menu)),41),223,190,MarketTheme.INK);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float tick){super.render(g,mx,my,tick);if(!menu.busy())renderTooltip(g,mx,my);}
}
