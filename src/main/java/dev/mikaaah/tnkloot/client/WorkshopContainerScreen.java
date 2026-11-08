package dev.mikaaah.tnkloot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** TNK's item-based editors retain vanilla chest geometry and their existing server actions. */
@EventBusSubscriber(modid="tnkloot",value=Dist.CLIENT)
public final class WorkshopContainerScreen extends ContainerScreen {
 private static final ResourceLocation TEXTURE=ResourceLocation.parse("tnkloot:textures/gui/cleanlook/generic_54.png");
 public WorkshopContainerScreen(ChestMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
 @SubscribeEvent public static void opening(ScreenEvent.Opening event){
  if(event.getNewScreen() instanceof ContainerScreen screen&&!(screen instanceof WorkshopContainerScreen)&&screen.getTitle().getString().startsWith("TNK Loot · ")){
   var player=Minecraft.getInstance().player;
   if(player!=null&&screen.getMenu().getRowCount()==6)event.setNewScreen(new WorkshopContainerScreen(screen.getMenu(),player.getInventory(),screen.getTitle()));
  }
 }
 @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){
  // Source row 140 becomes player row 139, exactly as ContainerScreen renders this pack.
  g.blit(TEXTURE,leftPos,topPos,0f,0f,imageWidth,125,256,256);
  g.blit(TEXTURE,leftPos,topPos+125,0f,126f,imageWidth,96,256,256);
 }
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){
  String heading=title.getString().contains("Boss")?"Boss Editor":"Loot Journal";
  g.drawString(font,heading,8,6,MarketTheme.LIGHT,false);
  g.drawString(font,"Inventory",8,128,MarketTheme.SOFT,false);
 }
}
