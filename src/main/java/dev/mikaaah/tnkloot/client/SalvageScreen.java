package dev.mikaaah.tnkloot.client;
import dev.mikaaah.tnkloot.workshop.SalvageMenu;
import static dev.mikaaah.tnkloot.workshop.SalvageLayout.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class SalvageScreen extends AbstractContainerScreen<SalvageMenu> {
 private Button confirm;
 public SalvageScreen(SalvageMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=WIDTH;imageHeight=HEIGHT;inventoryLabelY=titleLabelY=10000;}
 private Button button(int x,int y,int w,int h,String label,int id,String help){var button=new SalvageButton(leftPos+x,topPos+y,w,h,label,id<0,false,b->send(id));button.setTooltip(Tooltip.create(Component.literal(help)));return addRenderableWidget(button);}
 @Override protected void init(){
  super.init();topPos=StationHeader.top(height);
  button(8,StationHeader.NAV_Y,57,StationHeader.NAV_HEIGHT,"Identify",90,"Return unconsumed gear and open the Identifier");
  button(69,StationHeader.NAV_Y,57,StationHeader.NAV_HEIGHT,"Salvage",-1,"Recover identification credit from unwanted gear");
  button(130,StationHeader.NAV_Y,57,StationHeader.NAV_HEIGHT,"Preview",92,"Inspect the rewards at this counter");
  button(ACTION_X,StationHeader.NAV_Y,ACTION_W,StationHeader.NAV_HEIGHT,"Journal",91,"Open the Loot Journal and collection");
  confirm=button(ACTION_X,ACTION_Y,ACTION_W,ACTION_H,"Salvage",0,"Calculate salvage rewards; click again to confirm");update();
 }
 private void send(int id){if(id>=0&&minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
 private void update(){if(confirm==null)return;confirm.active=menu.ready();confirm.setMessage(Component.literal("Salvage"));var text=new StringBuilder(menu.reviewed()?"Final strike · equipment is permanently destroyed":"Calculate rewards; click Salvage again to confirm");for(int i=0;i<7;i++)if(menu.getSlot(i).hasItem())text.append("\n• ").append(menu.getSlot(i).getItem().getHoverName().getString());text.append("\nCost: ").append(menu.xpCost()).append(" XP points\nReturn: ").append(menu.payout()).append(" credit");if(menu.xpBalance()<menu.xpCost())text.append("\nYou need more XP");else if(menu.count()>0&&!menu.ready())text.append("\nSalvage unavailable: check the credit limit or config");confirm.setTooltip(Tooltip.create(Component.literal(text.toString())));}
 @Override protected void containerTick(){super.containerTick();update();}
 @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){g.blit(net.minecraft.resources.ResourceLocation.parse("tnkloot:textures/gui/salvage/background.png"),leftPos-8,topPos-16,0F,0F,272,280,272,280);}
 private void right(GuiGraphics g,String text,int x,int y,int colour){g.drawString(font,text,x-font.width(text),y,colour,false);}
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){
  StationHeader.draw(g,font,"Salvage Anvil",43,170);
  g.drawString(font,"XP cost",88,48,MarketTheme.LIGHT,false);
  right(g,Integer.toString(menu.xpCost()),236,48,menu.xpBalance()<menu.xpCost()?0xFFFFB093:MarketTheme.LIGHT);
  g.drawString(font,"Credit return",88,65,MarketTheme.LIGHT,false);
  right(g,"+"+menu.payout(),236,65,MarketTheme.GREEN);
  g.drawString(font,"Equipment",20,88,MarketTheme.SOFT,false);
  String status=menu.count()==0?"Place unwanted equipment in the slots":!menu.ready()?menu.xpBalance()<menu.xpCost()?"Not enough XP points":"Salvage unavailable":menu.reviewed()?"Ready - click Salvage again to confirm":menu.count()+" items selected - click Salvage";
  g.drawString(font,MarketTheme.fit(font,status,220),18,128,MarketTheme.SOFT,false);
  g.drawString(font,"Inventory",14,158,MarketTheme.SOFT,false);
  MarketTheme.center(g,font,"Balance",BALANCE_CENTER,158,MarketTheme.SOFT);
  MarketTheme.center(g,font,"XP",BALANCE_CENTER,182,MarketTheme.LIGHT);
  MarketTheme.center(g,font,MarketTheme.fit(font,Integer.toString(menu.xpBalance()),42),BALANCE_CENTER,194,MarketTheme.GREEN);
  MarketTheme.center(g,font,"Credit",BALANCE_CENTER,212,MarketTheme.LIGHT);
  MarketTheme.center(g,font,MarketTheme.fit(font,Integer.toString(menu.balance()),42),BALANCE_CENTER,224,MarketTheme.LIGHT);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float tick){super.render(g,mx,my,tick);renderTooltip(g,mx,my);}
}
