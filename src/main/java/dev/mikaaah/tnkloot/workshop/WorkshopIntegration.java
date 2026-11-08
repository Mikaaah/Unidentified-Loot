package dev.mikaaah.tnkloot.workshop;
import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.features.*;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.menu.*;
import dev.mikaaah.tnkloot.rarity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.*;
public final class WorkshopIntegration {
 private static final java.util.concurrent.atomic.AtomicBoolean ONCE=new java.util.concurrent.atomic.AtomicBoolean();
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 private static long gear(net.minecraft.world.entity.player.Player p){return p.getInventory().items.stream().filter(s->s.is(Items.DIAMOND_SWORD)).mapToInt(ItemStack::getCount).sum();}
 public static void run(ServerStartedEvent event){if(!Boolean.getBoolean("tnkloot.workshopSmokeTest")||!ONCE.compareAndSet(false,true))return;
  var server=event.getServer();var level=server.overworld();var pos=new BlockPos(0,100,0);var old=level.getBlockState(pos);
  var p=new net.minecraft.server.level.ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"TNKWorkshop"),net.minecraft.server.level.ClientInformation.createDefault()){@Override public boolean hasDisconnected(){return false;}@Override public boolean hasPermissions(int n){return true;}};p.connection=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"TNKSink")).connection;p.setPos(.5,100,.5);p.giveExperiencePoints(100000);
  try{
   var channel=net.minecraft.network.Connection.class.getDeclaredField("channel");channel.setAccessible(true);channel.set(p.connection.getConnection(),new io.netty.channel.embedded.EmbeddedChannel());
   level.setBlock(pos,TnkLoot.IDENTIFIER_BLOCK.get().defaultBlockState(),3);
   var token=UnidentifiedWeaponItem.create(1);RaritySupport.force(token,"rare");var item=RaritySupport.base(new ItemStack(Items.DIAMOND_SWORD),token);int value=PlayerLedger.salvageValue(item);require(value>0,"Test needs positive salvage value");
   var menu=new SalvageMenu(70,p.getInventory()).atStation(pos);
   require(menu.getSlot(0).mayPlace(item)&&!menu.getSlot(0).mayPlace(token)&&!menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),"Wrong input acceptance");
   p.getInventory().setItem(9,item.copy());require(!menu.quickMoveStack(p,7).isEmpty()&&p.getInventory().getItem(9).isEmpty()&&menu.getSlot(0).hasItem(),"Shift-click did not stage actual input");menu.broadcastChanges();
   require(menu.payout()==value&&menu.count()==1&&menu.ready(),"Wrong live quote");menu.clickMenuButton(p,0);require(menu.reviewed()&&menu.getSlot(0).hasItem()&&PlayerLedger.credit(p)==0,"Review consumed gear");
   menu.clickMenuButton(p,0);menu.clickMenuButton(p,0);require(PlayerLedger.credit(p)==value&&!menu.getSlot(0).hasItem(),"Repeated confirm duplicated salvage");
   menu.getSlot(0).set(item.copy());menu.clickMenuButton(p,0);menu.getSlot(0).getItem().setDamageValue(10);menu.clickMenuButton(p,0);require(!menu.reviewed()&&menu.getSlot(0).hasItem()&&PlayerLedger.credit(p)==value,"Stale review destroyed modified gear");menu.removed(p);menu.removed(p);require(gear(p)==1,"Close did not return input exactly once");
   p.getInventory().clearContent();PlayerLedger.write(p,new CompoundTag());menu=new SalvageMenu(71,p.getInventory()).atStation(pos);for(int i=0;i<7;i++)menu.getSlot(i).set(item.copy());menu.broadcastChanges();require(menu.count()==7&&menu.payout()==7*value,"Wrong seven-item quote");menu.clickMenuButton(p,0);menu.clickMenuButton(p,0);require(PlayerLedger.credit(p)==7*value&&menu.count()==0,"Seven-item transaction failed");menu.removed(p);
   PlayerLedger.write(p,new CompoundTag());PlayerLedger.addCredit(p,FeatureConfig.get().maxCredit());menu=new SalvageMenu(72,p.getInventory());menu.getSlot(0).set(item.copy());menu.broadcastChanges();require(!menu.ready(),"Credit overflow accepted");menu.clickMenuButton(p,0);require(menu.getSlot(0).hasItem()&&PlayerLedger.credit(p)==FeatureConfig.get().maxCredit(),"Overflow changed items or balance");menu.removed(p);
   p.getInventory().clearContent();PlayerLedger.write(p,new CompoundTag());
   // Real station navigation closes the old menu and returns staged equipment.
   TnkMenuProvider.openIdentifier(p,pos);require(p.containerMenu instanceof TnkMenu,"Identifier did not open");var identifier=(TnkMenu)p.containerMenu;identifier.getSlot(0).set(token.copy());require(identifier.clickMenuButton(p,90)&&p.containerMenu instanceof SalvageMenu,"Counter salvage navigation failed");require(p.getInventory().items.stream().anyMatch(s->s.getItem() instanceof UnidentifiedWeaponItem),"Switching lost unpaid token");
   menu=(SalvageMenu)p.containerMenu;menu.getSlot(0).set(item.copy());require(menu.clickMenuButton(p,90)&&p.containerMenu instanceof TnkMenu,"Return to identify failed");require(gear(p)==1,"Switching lost salvage input");p.closeContainer();SalvageMenu.open(p,null);require(p.containerMenu instanceof SalvageMenu&&p.containerMenu.clickMenuButton(p,90)&&p.containerMenu instanceof TnkMenu,"Remote salvage return failed");p.closeContainer();
   menu=new SalvageMenu(73,p.getInventory()).atStation(pos);p.setPos(20,100,20);require(!menu.stillValid(p)&&!menu.clickMenuButton(p,0),"Out-of-range station usable");p.setPos(.5,100,.5);level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);require(!menu.stillValid(p),"Removed station remained usable");menu.removed(p);
   var book=vazkii.patchouli.common.book.BookRegistry.INSTANCE.books.get(LootJournal.BOOK);require(book!=null&&book.noBook&&book.getBookItem().is(FeatureItems.JOURNAL.get()),"Patchouli custom journal registration failed");
   FeatureItems.JOURNAL.get().use(level,p,net.minecraft.world.InteractionHand.MAIN_HAND);
   var fresh=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"TNKWorkshopClone"));PlayerLedger.clonePlayer(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(fresh,p,true));require(PlayerLedger.read(fresh).equals(PlayerLedger.read(p)),"Station credit lost on death clone");
   TnkLoot.LOG.info("TNKLOOT_WORKSHOP_OK: physical slots, review/confirm, stale review, 7-item batch, capacity, close/duplicate-close, station navigation/input recovery, distance/removal, Patchouli custom item and death persistence");
  }catch(Throwable e){TnkLoot.LOG.error("TNKLOOT_WORKSHOP_FAILED",e);}finally{p.closeContainer();level.setBlock(pos,old,3);server.halt(false);}
 }
}
