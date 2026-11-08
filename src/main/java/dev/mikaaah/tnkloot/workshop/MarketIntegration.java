package dev.mikaaah.tnkloot.workshop;
import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.features.*;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.menu.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.*;
public final class MarketIntegration {
 private static final java.util.concurrent.atomic.AtomicBoolean ONCE=new java.util.concurrent.atomic.AtomicBoolean();
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 private static void budget(Player p,int points){p.experienceLevel=0;p.experienceProgress=0;p.totalExperience=0;p.giveExperiencePoints(points);require(ExperienceCosts.points(p)==points,"XP budget mismatch");}
 public static void run(ServerStartedEvent event){if(!Boolean.getBoolean("tnkloot.marketSmokeTest")||!ONCE.compareAndSet(false,true))return;var server=event.getServer();var level=server.overworld();var pos=new BlockPos(0,100,0);var old=level.getBlockState(pos);String config=null;
  var p=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"TNKMarket"),ClientInformation.createDefault()){@Override public boolean hasDisconnected(){return false;}@Override public boolean hasPermissions(int n){return true;}};p.connection=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"TNKMarketSink")).connection;p.setPos(.5,100,.5);
  try{
   var channel=net.minecraft.network.Connection.class.getDeclaredField("channel");channel.setAccessible(true);channel.set(p.connection.getConnection(),new io.netty.channel.embedded.EmbeddedChannel());
   config=java.nio.file.Files.readString(ExperienceCosts.path());var settings=com.google.gson.JsonParser.parseString(config).getAsJsonObject();settings.addProperty("identification_payment","xp_points");settings.addProperty("salvage_costs_xp",true);java.nio.file.Files.writeString(ExperienceCosts.path(),settings.toString());require(ExperienceCosts.reload(),"XP config rejected");
   for(int l=0;l<200;l++){p.experienceLevel=l;p.experienceProgress=0;require(ExperienceCosts.xpAtLevel(l+1)-ExperienceCosts.xpAtLevel(l)==p.getXpNeededForNextLevel(),"Wrong vanilla XP curve "+l);}
   budget(p,1000);require(ExperienceCosts.spendPoints(p,731)&&ExperienceCosts.points(p)==269,"Cross-level XP deduction wrong");require(!ExperienceCosts.spendPoints(p,270)&&!ExperienceCosts.spendPoints(p,-1)&&ExperienceCosts.points(p)==269,"Invalid XP payment altered balance");
   ItemStack identified=ItemStack.EMPTY;
   for(int mode=0;mode<4;mode++){
    p.getInventory().clearContent();PlayerLedger.write(p,new CompoundTag());var token=UnidentifiedWeaponItem.create(1);var menu=new TnkMenu(TnkMenus.IDENTIFY.get(),100+mode,p.getInventory(),false);menu.getSlot(0).set(token);int total=(int)FeatureHooks.total(menu,p);require(total>0,"Test needs priced identification");int credit=mode==1?total/2:mode==2?total:0;PlayerLedger.addCredit(p,credit);int xp=mode==3?total-1:total-credit;budget(p,xp);FeatureHooks.quote(menu,p);require(menu.quotedCost()==total-credit&&ExperienceCosts.xpMode(menu),"Wrong XP quote");var before=token.copy();menu.clickMenuButton(p,0);
    if(mode==3){require(!menu.busy()&&ExperienceCosts.points(p)==xp&&ItemStack.matches(menu.getSlot(0).getItem(),before),"Unaffordable batch changed XP/input");menu.removed(p);continue;}
    require(menu.busy()&&ExperienceCosts.points(p)==0&&PlayerLedger.credit(p)==0,"XP/credit payment failed");menu.removed(p);menu.removed(p);require(p.getInventory().items.stream().filter(s->!s.isEmpty()).count()==1,"Paid batch close lost or duplicated gear");identified=p.getInventory().items.stream().filter(s->!s.isEmpty()).findFirst().orElseThrow().copy();
   }
   p.getInventory().clearContent();PlayerLedger.write(p,new CompoundTag());budget(p,0);var salvage=new SalvageMenu(110,p.getInventory());salvage.getSlot(0).set(identified.copy());salvage.broadcastChanges();int fee=salvage.xpCost(),reward=salvage.payout();require(fee>0&&!salvage.ready(),"Unfunded salvage allowed");salvage.clickMenuButton(p,0);require(salvage.getSlot(0).hasItem()&&PlayerLedger.credit(p)==0,"Failed salvage consumed gear");budget(p,fee);salvage.broadcastChanges();salvage.clickMenuButton(p,0);require(salvage.reviewed()&&ExperienceCosts.points(p)==fee,"Review charged XP");salvage.clickMenuButton(p,0);salvage.clickMenuButton(p,0);require(ExperienceCosts.points(p)==0&&PlayerLedger.credit(p)==reward&&!salvage.getSlot(0).hasItem(),"Salvage fee/reward not atomic");salvage.removed(p);
   salvage=new SalvageMenu(111,p.getInventory());salvage.getSlot(0).set(identified.copy());budget(p,1000);salvage.clickMenuButton(p,0);salvage.getSlot(0).getItem().setDamageValue(1);salvage.clickMenuButton(p,0);require(ExperienceCosts.points(p)==1000&&salvage.getSlot(0).hasItem()&&!salvage.reviewed(),"Stale salvage review spent XP");salvage.removed(p);
   p.getInventory().clearContent();level.setBlock(pos,TnkLoot.IDENTIFIER_BLOCK.get().defaultBlockState(),3);TnkMenuProvider.openIdentifier(p,pos);var station=(TnkMenu)p.containerMenu;var previewToken=UnidentifiedWeaponItem.create(1);station.getSlot(0).set(previewToken);var snapshot=previewToken.copy();int xp=ExperienceCosts.points(p);station.clickMenuButton(p,92);require(p.containerMenu instanceof CatalogMenu,"Preview tab did not open custom menu");var preview=(CatalogMenu)p.containerMenu;require(CatalogMenu.header(preview).getBoolean("picker")&&CatalogMenu.header(preview).getInt("size")==1,"Counter input not returned before preview");preview.clicked(0,0,ClickType.PICKUP,p);require(!CatalogMenu.header(preview).getBoolean("picker")&&CatalogMenu.header(preview).getInt("size")>0,"Reward preview empty");for(var click:ClickType.values())preview.clicked(0,0,click,p);require(preview.getCarried().isEmpty()&&ExperienceCosts.points(p)==xp,"Preview extracted icons or spent XP");require(p.getInventory().items.stream().filter(s->s.getItem() instanceof UnidentifiedWeaponItem).count()==1&&p.getInventory().items.stream().anyMatch(s->ItemStack.matches(s,snapshot)),"Preview rerolled/consumed token");preview.clickMenuButton(p,90);require(p.containerMenu instanceof TnkMenu,"Preview return did not reopen counter");p.closeContainer();
   PlayerLedger.discovered(p,identified);CatalogMenu.open(p,true,null);var book=(CatalogMenu)p.containerMenu;require(CatalogMenu.header(book).getInt("known")>=1,"Collection lost discoveries");for(var click:ClickType.values())book.clicked(0,0,click,p);require(book.getCarried().isEmpty(),"Book yielded a free item");book.clickMenuButton(p,3);require(CatalogMenu.header(book).getInt("filter")==1,"Collection filters broken");book.clickMenuButton(p,100);require(CatalogMenu.header(book).getInt("category")==0,"Collection group filter broken");p.closeContainer();
   TnkLoot.LOG.info("TNKLOOT_MARKET_OK: 200 XP level boundaries, exact point spend, XP-only/mixed-credit/free-credit/insufficient identification, paid-close recovery, XP salvage review/stale/failure/one-shot commit, station preview navigation and read-only pool, book discovery and filters");
  }catch(Throwable e){TnkLoot.LOG.error("TNKLOOT_MARKET_FAILED",e);}finally{p.closeContainer();level.setBlock(pos,old,3);try{if(config!=null){java.nio.file.Files.writeString(ExperienceCosts.path(),config);ExperienceCosts.reload();}}catch(Exception e){TnkLoot.LOG.error("XP config restore failed",e);}server.halt(false);}
 }
}
