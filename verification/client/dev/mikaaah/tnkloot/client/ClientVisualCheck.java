package dev.mikaaah.tnkloot.client;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.menu.*;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import dev.mikaaah.tnkloot.workshop.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;

/** Development-only source set: never included in the release JAR. */
@EventBusSubscriber(modid="tnkloot",value=Dist.CLIENT)
public final class ClientVisualCheck {
 private static int tick,stage;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  try{
   if(tick++==0){
    for(var type:java.util.List.of(TnkMenus.IDENTIFY.get(),TnkMenus.ADMIN.get(),TnkMenus.SALVAGE.get(),TnkMenus.PREVIEW.get(),TnkMenus.COLLECTION.get()))
     if(MenuScreens.getScreenFactory(type).isEmpty())throw new AssertionError("Missing menu screen registration");
    var m=new TnkMenu(TnkMenus.IDENTIFY.get(),700,mc.player.getInventory(),false);String[] rarities={"common","uncommon","rare","epic","legendary","mythic","legendary"};
    for(int i=0;i<7;i++){var token=UnidentifiedWeaponItem.create(TnkConfig.Category.values()[i],1);RaritySupport.force(token,rarities[i]);m.getSlot(i).set(token);}
    m.setData(1,42);m.setData(4,7);m.setData(6,1);m.setData(15,1);m.setData(16,128);m.setData(18,8);
    mc.setScreen(new IdentifierScreen(m,mc.player.getInventory(),Component.literal("Item Identifier")));
   }
   if(tick==50){
    Path out=Path.of("visual-check");Files.createDirectories(out);String[] names={"identifier","salvage","preview","collection","ponder"};
    try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(names[stage]+".png"));}
    stage++;tick=1;
    if(stage==1){var m=new SalvageMenu(701,mc.player.getInventory());for(int i=0;i<7;i++)m.getSlot(i).set(new ItemStack(Items.IRON_SWORD));mc.setScreen(new SalvageScreen(m,mc.player.getInventory(),Component.literal("Salvage Anvil")));}
    else if(stage==2){var m=new CatalogMenu(702,mc.player.getInventory(),false);mc.setScreen(new PreviewScreen(m,mc.player.getInventory(),Component.literal("Reward Preview")));}
    else if(stage==3){var m=new CatalogMenu(703,mc.player.getInventory(),true);mc.setScreen(new CollectionScreen(m,mc.player.getInventory(),Component.literal("Collection")));}
    else if(stage==4&&net.neoforged.fml.ModList.get().isLoaded("create")){
     var ponder=net.createmod.ponder.foundation.ui.PonderUI.of(new ItemStack(TnkLoot.IDENTIFIER_ITEM.get()));
     if(ponder==null)throw new AssertionError("Identifier has no installed Ponder scenes");
     mc.setScreen(ponder);ponder.seekToTime(140);
    }
    else{TnkLoot.LOG.info("TNKLOOT_CLIENT_VISUAL_OK: real client resource load, five screen registrations, four rendered GUIs and optional Ponder playback");mc.stop();}
   }
  }catch(Throwable error){TnkLoot.LOG.error("TNKLOOT_CLIENT_VISUAL_FAILED",error);mc.stop();}
 }
}
