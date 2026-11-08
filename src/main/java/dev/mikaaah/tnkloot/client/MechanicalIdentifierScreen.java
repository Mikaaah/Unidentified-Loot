package dev.mikaaah.tnkloot.client;

import dev.mikaaah.tnkloot.machine.MechanicalIdentifierMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class MechanicalIdentifierScreen extends AbstractContainerScreen<MechanicalIdentifierMenu> {
 private static final ResourceLocation BACKGROUND=ResourceLocation.parse("tnkloot:textures/gui/mechanical_identifier.png");
 private static final ResourceLocation GLASS=ResourceLocation.parse("tnkloot:textures/gui/mechanical_identifier_glass.png");
 private static final int TANK_X=235,TANK_Y=49,TANK_WIDTH=32,TANK_HEIGHT=59;
 private static final int TEXT=0xFFF4EBE5,EDGE=0xFFA9C3C8;
 public MechanicalIdentifierScreen(MechanicalIdentifierMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=312;imageHeight=260;inventoryLabelY=titleLabelY=10000;}
 @Override protected void init(){super.init();topPos=StationHeader.top(height);}
 @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){
  g.blit(BACKGROUND,leftPos-8,topPos-16,0f,0f,328,288,328,288);
  int duration=Math.max(1,menu.value(3));
  int progressWidth=(int)Math.clamp(160L*menu.value(2)/duration,0,160);
  if(progressWidth>0)g.fillGradient(leftPos+27,topPos+118,leftPos+27+progressWidth,topPos+123,EDGE,0xFF729AA1);
  int fill=Math.clamp((int)(TANK_HEIGHT*(long)menu.value(0)/Math.max(1,menu.value(1))),0,TANK_HEIGHT);
  if(fill>0)renderFluid(g,leftPos+TANK_X,topPos+TANK_Y+TANK_HEIGHT,TANK_WIDTH,fill);
  g.blit(GLASS,leftPos+TANK_X,topPos+TANK_Y,0f,0f,TANK_WIDTH,TANK_HEIGHT,TANK_WIDTH,TANK_HEIGHT);
 }
 private void renderFluid(GuiGraphics g,int x,int bottom,int width,int height){
  var fluid=menu.fluid();if(fluid.isEmpty())return;
  var visual=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
  var texture=visual.getStillTexture(fluid);if(texture==null)return;
  var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture);
  int tint=visual.getTintColor(fluid);
  float red=((tint>>16)&255)/255f,green=((tint>>8)&255)/255f,blue=(tint&255)/255f,alpha=((tint>>>24)&255)/255f;
  g.enableScissor(x,bottom-height,x+width,bottom);
  for(int dx=0;dx<width;dx+=16)for(int dy=0;dy<height;dy+=16)
   g.blit(x+dx,bottom-dy-16,0,16,16,sprite,red,green,blue,alpha);
  g.disableScissor();
 }
 private void centered(GuiGraphics g,String text,int x,int y,int color){
  g.drawString(font,text,x-font.width(text)/2,y,color,false);
 }
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){
  StationHeader.draw(g,font,title,43,226);
  g.drawString(font,"Unidentified Input",27,35,TEXT,false);
  g.drawString(font,"Identified Output",27,78,TEXT,false);
  centered(g,"Liquid XP",251,35,EDGE);
  centered(g,String.format(java.util.Locale.ROOT,"%,d mB",menu.value(0)),251,115,0xFFCCE3AD);
  centered(g,"/ "+String.format(java.util.Locale.ROOT,"%,d",menu.value(1)),251,127,EDGE);
  String status=switch(menu.value(4)){case 1->"Identifying…";case 2->"Waiting for Liquid XP";case 3->"Output Full";case 4->"Reward Pool Unavailable";case 5->"Identification Disabled";default->"Ready for Unidentified Loot";};
  g.drawString(font,status,27,132,menu.value(4)>=2?0xFFF0C49A:TEXT,false);
  g.drawString(font,"Inventory",27,154,EDGE,false);
  centered(g,"Automation",251,157,EDGE);
  centered(g,"In: Top / Sides",251,174,0xFFD0DDE0);
  centered(g,"Out: Bottom",251,188,0xFFD0DDE0);
  centered(g,"XP: Right Port",251,202,0xFFD0DDE0);
  int cost=menu.value(5);
  centered(g,cost==Integer.MAX_VALUE?"Add Liquid XP":cost>0?cost+" mB / Item":"",251,220,0xFFD0DDE0);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float tick){super.render(g,mx,my,tick);renderTooltip(g,mx,my);}
}
