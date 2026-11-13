package dev.mikaaah.tnkloot.client;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.menu.*;
import dev.mikaaah.tnkloot.machine.MechanicalIdentifierMenu;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import dev.mikaaah.tnkloot.workshop.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;

/** Development-only visual receipt; excluded from every release JAR. */
@EventBusSubscriber(modid="tnkloot",value=Dist.CLIENT)
public final class ClientVisualCheck {
 private static int tick,stage,scale=2;
 private static boolean opening;
 private static final String[] NAMES={"identifier","salvage","preview","collection","mechanical","groups","vault","boss_editor"};
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();
  try{
   if(mc.player==null||mc.level==null){
    if(!opening&&mc.screen instanceof TitleScreen){
     mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
     opening=true;
     mc.createWorldOpenFlows().createFreshLevel("TNKLootVisual-1.1.2",
      new LevelSettings("TNK GUI Verification",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
      new WorldOptions(12345L,false,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
    }
    return;
   }
   if(tick++==0){
    for(var type:java.util.List.of(TnkMenus.IDENTIFY.get(),TnkMenus.ADMIN.get(),TnkMenus.SALVAGE.get(),TnkMenus.PREVIEW.get(),TnkMenus.COLLECTION.get()))
     if(MenuScreens.getScreenFactory(type).isEmpty())throw new AssertionError("Missing menu screen registration");
    mc.options.guiScale().set(scale);mc.resizeDisplay();
    for(int i=0;i<36;i++)mc.player.getInventory().setItem(i,new ItemStack(i%2==0?Items.IRON_INGOT:Items.EMERALD,i+1));
    open(mc);
   }
   if(tick==40){
    Path out=Path.of("visual-check");Files.createDirectories(out);
    try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(NAMES[stage]+"-scale-"+scale+".png"));}
    if(++stage==NAMES.length){
     if(scale==2){scale=3;stage=0;tick=0;return;}
     TnkLoot.LOG.info("TNKLOOT_CLIENT_VISUAL_OK: eight rendered GUIs at GUI scales 2 and 3, Progression 1.1.0 loaded");
     mc.stop();return;
    }
    tick=1;open(mc);
   }
  }catch(Throwable error){TnkLoot.LOG.error("TNKLOOT_CLIENT_VISUAL_FAILED",error);mc.stop();}
 }
 private static ItemStack token(int i){var s=UnidentifiedWeaponItem.create(TnkConfig.Category.values()[i%8],1);RaritySupport.force(s,new String[]{"common","uncommon","rare","epic","legendary","mythic"}[i%6]);return s;}
 private static void open(Minecraft mc){
  var inv=mc.player.getInventory();
  switch(stage){
   case 0->{var m=new TnkMenu(TnkMenus.IDENTIFY.get(),700,inv,false);for(int i=0;i<7;i++)m.getSlot(i).set(token(i));m.setData(1,42);m.setData(4,7);m.setData(6,1);m.setData(15,1);m.setData(16,128);m.setData(18,8);mc.setScreen(new IdentifierScreen(m,inv,Component.literal("Item Identifier")));}
   case 1->{var m=new SalvageMenu(701,inv);for(int i=0;i<7;i++)m.getSlot(i).set(new ItemStack(i%2==0?Items.IRON_SWORD:Items.IRON_CHESTPLATE));mc.setScreen(new SalvageScreen(m,inv,Component.literal("Salvage Anvil")));}
   case 2,3->{boolean book=stage==3;var m=new CatalogMenu(702,inv,book);for(int i=0;i<36;i++)m.getSlot(i).set(new ItemStack(i%2==0?Items.IRON_SWORD:Items.IRON_HELMET));var h=new CompoundTag();h.putInt("pages",2);h.putInt("size",72);h.putInt("known",36);h.putInt("total",72);h.putInt("category",-1);h.putString("selection","Novice equipment · Common");var paper=new ItemStack(Items.PAPER);paper.set(DataComponents.CUSTOM_DATA,CustomData.of(h));m.getSlot(36).set(paper);mc.setScreen(book?new CollectionScreen(m,inv,Component.literal("Collection")):new PreviewScreen(m,inv,Component.literal("Reward Preview")));}
   case 4->{var m=new MechanicalIdentifierMenu(704,inv,BlockPos.ZERO);for(int i=0;i<7;i++){m.getSlot(i).set(token(i));m.getSlot(7+i).set(new ItemStack(Items.IRON_SWORD));}m.setData(0,1000);m.setData(2,4000);m.setData(4,12);m.setData(6,24);m.setData(8,1);mc.setScreen(new MechanicalIdentifierScreen(m,inv,Component.literal("Mechanical Identifier")));}
   case 5,6->{var m=new TnkMenu(TnkMenus.ADMIN.get(),705,inv,true);if(stage==6){m.setData(12,2);for(int i=0;i<36;i++)m.getSlot(i).set(new ItemStack(Items.IRON_SWORD));}mc.setScreen(new TnkMenuScreen(m,inv,Component.literal("Tier Vault")));}
   case 7->{var m=ChestMenu.sixRows(706,inv);for(int i=0;i<54;i++)m.getSlot(i).set(new ItemStack(i%2==0?Items.GOLD_INGOT:Items.IRON_SWORD));mc.setScreen(new WorkshopContainerScreen(m,inv,Component.literal("TNK Loot · Boss Editor")));}
  }
 }
}
