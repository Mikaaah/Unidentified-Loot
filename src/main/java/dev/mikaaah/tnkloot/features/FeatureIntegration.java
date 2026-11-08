package dev.mikaaah.tnkloot.features;
import com.mojang.authlib.GameProfile;
import com.google.gson.*;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.menu.*;
import dev.mikaaah.tnkloot.rarity.*;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.*;
import java.util.*;
public final class FeatureIntegration {
 static final java.util.concurrent.atomic.AtomicBoolean ONCE=new java.util.concurrent.atomic.AtomicBoolean();
 static void require(boolean b,String text){if(!b)throw new AssertionError(text);}
 public static void run(ServerStartedEvent event){
  if(!Boolean.getBoolean("tnkloot.featuresSmokeTest")||!ONCE.compareAndSet(false,true))return;
  var server=event.getServer();var p=new FakePlayer(server.overworld(),new GameProfile(UUID.randomUUID(),"TNKFeatureTest")){
   @Override public boolean hasDisconnected(){return false;}@Override public boolean hasPermissions(int level){return true;}
  };
  String cfg=null,features=null;
  try(var payment=new dev.mikaaah.tnkloot.workshop.LegacyPaymentScope()){
   cfg=Files.readString(TnkConfig.path());features=Files.readString(FeatureConfig.path());int rolls=0;
   // Genuine signature affixes, restricted to boss provenance, bounded by tier.
   for(String theme:List.of("ember","frost","storm","venom","void","earth"))for(int tier:new int[]{0,3,7})for(var q:RarityPolicy.get().qualities())for(Item item:List.of(Items.DIAMOND_SWORD,Items.DIAMOND_HELMET,Items.BOW,Items.DIAMOND_PICKAXE)){
    var token=UnidentifiedWeaponItem.create(TnkConfig.Category.WEAPON,tier);RaritySupport.force(token,q.id());var tag=UnidentifiedWeaponItem.data(token);
    tag.putString("boss_pool",switch(theme){case "ember"->"test/ignis";case "frost"->"test/frostmaw";case "storm"->"test/storm";case "venom"->"test/spider";case "void"->"test/ender";default->"test/stone";});tag.putString("boss_name","Test Boss");token.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    var base=RaritySupport.base(new ItemStack(item),token);var result=ApotheosisCompat.apply(base,tier,p,89);
    var holder=AffixRegistry.INSTANCE.holder(ResourceLocation.parse("tnkloot:boss/"+theme));
    require(result.getOrDefault(Apoth.Components.AFFIXES,ItemAffixes.EMPTY).keySet().contains(holder),"Missing real boss affix "+theme+" "+item);
    require(UnidentifiedWeaponItem.data(result).getString("tnk_signature").equals(theme),"Wrong signature");rolls++;
   }
   var plain=ApotheosisCompat.apply(UnidentifiedWeaponItem.baseResult(p.level(),UnidentifiedWeaponItem.create(1)),1,p,1);require(!UnidentifiedWeaponItem.data(plain).contains("tnk_signature"),"Ordinary item gained exclusive signature");
   var target=EntityType.ZOMBIE.create(p.level());BossSignatures.trigger("frost",p,target);require(target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),"Frost has no combat effect");BossSignatures.trigger("ember",p,target);require(target.getRemainingFireTicks()>0,"Ember has no combat effect");
   // No token may be consumed or preview seed changed by opening read-only menus.
   var t=UnidentifiedWeaponItem.create(1);var before=t.copy();require(!FeatureMenus.options(t).isEmpty(),"Empty normal preview");require(ItemStack.isSameItemSameComponents(t,before),"Preview changed token");
   var ui=new FeatureMenus(60,p.getInventory(),p,new SimpleContainer(54),"hub");for(var click:ClickType.values())ui.clicked(11,0,click,p);require(ui.getCarried().isEmpty(),"Journal display extracted");ui.removed(p);
   // Salvage is staged, confirmed once, preserved through a serialized player ledger round trip.
   p.getInventory().clearContent();p.getInventory().setItem(9,plain.copy());var salvage=new FeatureMenus(61,p.getInventory(),p,new SimpleContainer(54),"salvage");
   salvage.clicked(54,0,ClickType.QUICK_MOVE,p);require(salvage.stagedCount()==1&&!p.getInventory().getItem(9).isEmpty(),"Staging consumed gear");salvage.removed(p);require(!p.getInventory().getItem(9).isEmpty()&&PlayerLedger.credit(p)==0,"Closing salvage lost items or granted credit");
   salvage=new FeatureMenus(62,p.getInventory(),p,new SimpleContainer(54),"salvage");salvage.select(9);int earned=PlayerLedger.salvageValue(plain);salvage.salvage();salvage.salvage();require(PlayerLedger.credit(p)==earned&&p.getInventory().getItem(9).isEmpty(),"Salvage duplicated credit or kept gear");
   var snapshot=PlayerLedger.read(p);PlayerLedger.write(p,new net.minecraft.nbt.CompoundTag());PlayerLedger.write(p,snapshot);require(PlayerLedger.credit(p)==earned,"Ledger serialization lost credit");
   p.getInventory().setItem(9,plain.copy());salvage=new FeatureMenus(63,p.getInventory(),p,new SimpleContainer(54),"salvage");salvage.select(9);p.getInventory().setItem(9,new ItemStack(Items.STONE));try{salvage.salvage();throw new AssertionError("Stale salvage accepted");}catch(IllegalArgumentException expected){}require(PlayerLedger.credit(p)==earned&&p.getInventory().getItem(9).is(Items.STONE),"Stale salvage changed currency or items");
   // Fully credit-funded, mixed-funded and insufficient batches; closing paid rolls still returns gear once.
   for(int mode=0;mode<3;mode++){
    p.getInventory().clearContent();PlayerLedger.write(p,new net.minecraft.nbt.CompoundTag());var token=UnidentifiedWeaponItem.create(1);var menu=new TnkMenu(TnkMenus.IDENTIFY.get(),70+mode,p.getInventory(),false);menu.getSlot(0).set(token);UnidentifiedWeaponItem.bindRollSeed(token);
    int price=UnidentifiedWeaponItem.cost(token,UnidentifiedWeaponItem.baseResult(p.level(),token));require(price>0,"Test needs enabled identification costs");int credit=mode==0?price:Math.max(0,price/2);PlayerLedger.addCredit(p,credit);int coins=mode==2?0:price-credit;if(coins>0)p.getInventory().add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(TnkConfig.get().costs().item())),coins));
    FeatureHooks.quote(menu,p);require(menu.quotedCost()==price-credit,"Quote ignored credit");
    if(mode==2&&price-credit>0){require(!menu.affordable(),"Insufficient batch accepted");menu.clickMenuButton(p,0);require(!menu.busy()&&PlayerLedger.credit(p)==credit,"Failed batch spent fragments");menu.removed(p);continue;}
    require(menu.clickMenuButton(p,0)&&menu.busy(),"Funded batch failed");require(PlayerLedger.credit(p)==0,"Credit not consumed");menu.removed(p);menu.removed(p);
    require(p.getInventory().items.stream().filter(s->!s.isEmpty()).count()==1,"Paid close lost or duplicated output");require(PlayerLedger.read(p).getCompound("discoveries").getAllKeys().size()==1,"Paid identification not discovered");
   }
   // Guaranteed dungeon injection into chest context, with a controlled accessory pool.
   require(TnkConfig.addWeaponToTier("minecraft:shield",TnkConfig.Category.ACCESSORY,0),"Could not create test accessory pool");
   var j=JsonParser.parseString(features).getAsJsonObject();j.addProperty("dungeon_accessory_chance",1);Files.writeString(FeatureConfig.path(),j.toString());require(FeatureConfig.reload(),"Feature reload failed");
   var params=new LootParams.Builder(p.serverLevel()).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withOptionalParameter(LootContextParams.THIS_ENTITY,p).create(LootContextParamSets.CHEST);
   var context=new LootContext.Builder(params).create(Optional.empty());context.setQueriedLootTableId(ResourceLocation.parse("minecraft:chests/simple_dungeon"));
   var loot=new ObjectArrayList<ItemStack>();FeatureHooks.dungeon(loot,context);FeatureHooks.dungeon(loot,context);require(loot.size()==1&&loot.getFirst().getItem() instanceof UnidentifiedWeaponItem,"Dungeon accessory missing or duplicated");
   var mobContext=new LootContext.Builder(params).create(Optional.empty());mobContext.setQueriedLootTableId(ResourceLocation.parse("minecraft:entities/zombie"));require(FeatureHooks.dungeon(new ObjectArrayList<>(),mobContext).isEmpty(),"Mob loot got dungeon injection");
   // Player commands remain usable without operator permissions.
   for(String command:List.of("journal","collection","salvage","preview")){
    var parsed=server.getCommands().getDispatcher().parse("tnkloot "+command,p.createCommandSourceStack().withPermission(0));
    require(!parsed.getReader().canRead()&&parsed.getContext().getCommand()!=null,"Player command requires operator: "+command);
   }
   require(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("tnkloot:loot_journal")),"Journal item not registered");
   // A fixed-power native accessory has no rarity surcharge, so salvage must not assume one.
   var nativeToken=UnidentifiedWeaponItem.create(7);RaritySupport.force(nativeToken,"mythic");var nativeReward=RaritySupport.base(new ItemStack(Items.CLOCK),nativeToken);
   int nativePrice=UnidentifiedWeaponItem.cost(nativeToken,nativeReward);require(PlayerLedger.salvageValue(nativeReward)<=Math.max(1,(int)(nativePrice*FeatureConfig.get().refundCap())),"Fixed native item yields a salvage profit");
   var saved=PlayerLedger.read(p);var clone=new FakePlayer(server.overworld(),new GameProfile(UUID.randomUUID(),"TNKCloneTest"));
   PlayerLedger.clonePlayer(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone,p,true));require(PlayerLedger.read(clone).equals(saved),"Death clone lost journal or fragments");
   // Cosmetic code path must run server-side without requiring client classes.
   for(var q:RarityPolicy.get().qualities()){var sample=UnidentifiedWeaponItem.create(1);RaritySupport.force(sample,q.id());FeatureHooks.reveal(p,RaritySupport.base(new ItemStack(Items.DIAMOND_SWORD),sample));}
   TnkLoot.LOG.info("TNKLOOT_FEATURES_OK: {} real boss-affix rolls, combat effects, preview/ghost protection, staged salvage, persistence, credit payment and close recovery, discovery, chest-only accessory injection and six rarity reveals",rolls);
  }catch(Throwable ex){TnkLoot.LOG.error("TNKLOOT_FEATURES_FAILED",ex);}finally{try{if(cfg!=null){Files.writeString(TnkConfig.path(),cfg);TnkConfig.reload();}if(features!=null){Files.writeString(FeatureConfig.path(),features);FeatureConfig.reload();}}catch(Exception ex){TnkLoot.LOG.error("Test config restore failed",ex);}server.halt(false);}
 }
}
