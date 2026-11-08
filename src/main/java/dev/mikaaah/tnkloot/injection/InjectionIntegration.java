package dev.mikaaah.tnkloot.injection;
import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.rarity.*;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.common.loot.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.*;
public final class InjectionIntegration {
 private static final java.util.concurrent.atomic.AtomicBoolean ONCE=new java.util.concurrent.atomic.AtomicBoolean();
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void run(ServerStartedEvent start){if(!Boolean.getBoolean("tnkloot.injectionSmokeTest")||!ONCE.compareAndSet(false,true))return;
  var server=start.getServer();var level=server.overworld();var p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"TNKInjection"));String config=null,bossConfig=null;Object originalMods=null;java.lang.reflect.Field field=null;Object manager=null;java.util.function.Consumer<LivingDropsEvent> late=null;
  try{
   config=java.nio.file.Files.readString(TnkConfig.path());
   var rarity=RarityRegistry.getSortedRarities().stream().filter(r->RarityRegistry.INSTANCE.getKey(r).toString().equals("apotheosis:epic")).findFirst().orElseThrow();
   var affix=Invader.modifyBossItem(new ItemStack(Items.DIAMOND_SWORD),Component.literal("Test Invader"),GenContext.forPlayer(RandomSource.create(91),p),rarity,15,level.registryAccess(),false);
   require(affix.getOrDefault(Apoth.Components.FROM_BOSS,false)&&affix.has(Apoth.Components.AFFIXES),"Test did not generate real Apotheosis invader gear");
   // Install one real GLM after the pack's existing modifiers, then invoke NeoForge itself.
   var getter=NeoForgeEventHandler.class.getDeclaredMethod("getLootModifierManager");getter.setAccessible(true);manager=getter.invoke(null);field=LootModifierManager.class.getDeclaredField("registeredLootModifiers");field.setAccessible(true);originalMods=field.get(manager);
   @SuppressWarnings("unchecked") var copy=new LinkedHashMap<ResourceLocation,IGlobalLootModifier>((Map<ResourceLocation,IGlobalLootModifier>)originalMods);
   copy.put(ResourceLocation.parse("tnkloot:late_probe"),new LootModifier(new net.minecraft.world.level.storage.loot.predicates.LootItemCondition[0]){
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> list,LootContext context){list.add(affix.copy());list.add(new ItemStack(Items.DIAMOND,2));return list;}
    @Override public com.mojang.serialization.MapCodec<? extends IGlobalLootModifier> codec(){return WeaponLootModifier.CODEC;}
   });field.set(manager,copy);
   var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withOptionalParameter(LootContextParams.THIS_ENTITY,p).create(LootContextParamSets.CHEST);var context=new LootContext.Builder(params).create(Optional.empty());
   var output=CommonHooks.modifyLoot(ResourceLocation.parse("minecraft:chests/simple_dungeon"),new ObjectArrayList<ItemStack>(),context);
   require(output.stream().noneMatch(s->s.getOrDefault(Apoth.Components.FROM_BOSS,false)),"Late GLM gear escaped final mixin");
   var bossToken=output.stream().filter(s->UnidentifiedWeaponItem.data(s).getBoolean("apotheosis_invader")).findFirst().orElseThrow();require(bossToken.getItem() instanceof UnidentifiedWeaponItem&&!UnidentifiedWeaponItem.baseResult(level,bossToken).isEmpty(),"Invader token not identifiable");
   require(output.stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()>=2,"Material rewards removed");field.set(manager,originalMods);originalMods=null;
   var zombie=EntityType.ZOMBIE.create(level);var drops=new ArrayList<ItemEntity>();drops.add(new ItemEntity(level,0,100,0,new ItemStack(Items.DIAMOND,3)));
   late=event->{if(event.getEntity()==zombie){event.getDrops().add(new ItemEntity(level,0,100,0,affix.copy()));event.getDrops().add(new ItemEntity(level,0,100,0,new ItemStack(Items.GOLDEN_SWORD,3)));}};
   NeoForge.EVENT_BUS.addListener(EventPriority.LOW,late);NeoForge.EVENT_BUS.post(new LivingDropsEvent(zombie,p.damageSources().playerAttack(p),drops,true));NeoForge.EVENT_BUS.unregister(late);late=null;
   require(drops.stream().noneMatch(d->d.getItem().is(Items.DIAMOND_SWORD)||d.getItem().is(Items.GOLDEN_SWORD)),"Late direct equipment escaped");
   require(drops.stream().filter(d->d.getItem().getItem() instanceof UnidentifiedWeaponItem).count()==4,"Equipment count changed");require(drops.stream().anyMatch(d->d.getItem().is(Items.DIAMOND)&&d.getItem().getCount()==3),"Mob materials lost");
   int size=drops.size();FinalLoot.drops(new LivingDropsEvent(zombie,p.damageSources().playerAttack(p),drops,true));require(drops.size()==size,"Final pass duplicated rewards");
   var playerDrops=new ArrayList<ItemEntity>();playerDrops.add(new ItemEntity(level,0,100,0,affix.copy()));FinalLoot.drops(new LivingDropsEvent(p,p.damageSources().generic(),playerDrops,true));require(ItemStack.matches(playerDrops.getFirst().getItem(),affix),"Player death equipment rewritten");
   var pool=dev.mikaaah.tnkloot.compat.BossRewards.find("aether/slider").orElse(null);
   if(pool==null){
    bossConfig=java.nio.file.Files.readString(dev.mikaaah.tnkloot.compat.BossRewards.path());
    java.nio.file.Files.writeString(dev.mikaaah.tnkloot.compat.BossRewards.path(),"""
     {"enabled":true,"require_player_kill":true,"pools":[{"id":"aether/slider","mod":"tnkloot","entity":"minecraft:skeleton","name":"Cache Test Boss","tier":1,"enabled":true,"chance":1,"additional_if_no_equipment":true,"items":[{"item":"minecraft:diamond_sword","category":"weapon","weight":1}]}]}
     """);
    require(dev.mikaaah.tnkloot.compat.BossRewards.reload(),"Fixture boss reload failed");pool=dev.mikaaah.tnkloot.compat.BossRewards.find("aether/slider").orElseThrow();
   }
   var bossEntity=(net.minecraft.world.entity.LivingEntity)net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(pool.entity())).create(level);
   var nativeReward=pool.items().stream().filter(w->net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(w.item()))).findFirst().orElseThrow();
   var preserved=RaritySupport.base(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(nativeReward.item()))),dev.mikaaah.tnkloot.compat.BossRewards.token(pool,TnkConfig.Category.WEAPON,level,ItemStack.EMPTY));
   var grouped=new ArrayList<ItemEntity>();grouped.add(new ItemEntity(level,0,100,0,preserved.copy()));for(int i=0;i<2;i++)grouped.add(new ItemEntity(level,0,100,0,dev.mikaaah.tnkloot.compat.BossRewards.token(pool,TnkConfig.Category.WEAPON,level,ItemStack.EMPTY)));
   grouped.add(new ItemEntity(level,0,100,0,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(nativeReward.item())))));grouped.add(new ItemEntity(level,0,100,0,new ItemStack(Items.DIAMOND)));
   FinalLoot.drops(new LivingDropsEvent(bossEntity,p.damageSources().playerAttack(p),grouped,true));require(grouped.size()==3&&grouped.stream().anyMatch(d->ItemStack.matches(d.getItem(),preserved))&&grouped.stream().filter(d->d.getItem().getItem() instanceof UnidentifiedWeaponItem).count()==1,"Boss rewards not consolidated after late raw prizes");
   var cacheLoot=new ObjectArrayList<ItemStack>();for(var d:grouped)cacheLoot.add(d.getItem().copy());cacheLoot.add(dev.mikaaah.tnkloot.compat.BossRewards.token(pool,TnkConfig.Category.WEAPON,level,ItemStack.EMPTY));cacheLoot.add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(nativeReward.item()))));var cacheContext=new LootContext.Builder(params).create(Optional.empty());cacheContext.setQueriedLootTableId(ResourceLocation.parse("aether:chests/dungeon/bronze/bronze_dungeon_treasure"));FinalLoot.table(cacheLoot,cacheContext);
   require(cacheLoot.size()==3&&cacheLoot.stream().anyMatch(s->ItemStack.matches(s,preserved))&&cacheLoot.stream().filter(s->s.getItem() instanceof UnidentifiedWeaponItem).count()==1,"Nested boss cache consolidation failed: "+cacheLoot.stream().map(s->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem())+" "+UnidentifiedWeaponItem.data(s)).toList());
   for(String flag:List.of("identification_enabled","replace_weapon_loot")){var disabled=com.google.gson.JsonParser.parseString(config).getAsJsonObject();disabled.addProperty(flag,false);java.nio.file.Files.writeString(TnkConfig.path(),disabled.toString());require(TnkConfig.reload(),"Flag reload failed");require(FinalLoot.replacement(affix,level,p,"test").isEmpty(),"Disabled replacement changed gear");java.nio.file.Files.writeString(TnkConfig.path(),config);require(TnkConfig.reload(),"Flag restore failed");}
   var identified=ApotheosisCompat.apply(UnidentifiedWeaponItem.baseResult(level,UnidentifiedWeaponItem.create(1)),1,p,77);require(FinalLoot.replacement(identified,level,p,"test").isEmpty(),"Previously identified gear rewritten");
   TnkLoot.LOG.info("TNKLOOT_INJECTION_OK: real Apotheosis invader gear, appended GLM order, final CommonHooks mixin, late direct mob drops, unconfigured gold inputs, exact counts, preserved materials/player gear, boss consolidation, disable flags, idempotence and identifiable results");
  }catch(Throwable e){TnkLoot.LOG.error("TNKLOOT_INJECTION_FAILED",e);}finally{try{if(bossConfig!=null){java.nio.file.Files.writeString(dev.mikaaah.tnkloot.compat.BossRewards.path(),bossConfig);dev.mikaaah.tnkloot.compat.BossRewards.reload();}if(config!=null){java.nio.file.Files.writeString(TnkConfig.path(),config);TnkConfig.reload();}if(originalMods!=null)field.set(manager,originalMods);if(late!=null)NeoForge.EVENT_BUS.unregister(late);}catch(Exception e){TnkLoot.LOG.error("Injection test restore failed",e);}server.halt(false);}
 }
}
