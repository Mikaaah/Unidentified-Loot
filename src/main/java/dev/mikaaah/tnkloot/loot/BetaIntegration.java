package dev.mikaaah.tnkloot.loot;

import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.*;

/** Opt-in checks of the real companion API and merged loot paths. */
public final class BetaIntegration {
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void run(ServerStartedEvent event){
  if(!Boolean.getBoolean("tnkloot.betaSmokeTest"))return;
  var server=event.getServer();var world=server.overworld();var previous=TnkBetaConfig.current;
  try{
   var player=new FakePlayer(world,new GameProfile(UUID.randomUUID(),"TNKBeta")){@Override public boolean hasDisconnected(){return false;}@Override public boolean hasPermissions(int level){return true;}};
   var profile=(CompoundTag)Class.forName("dev.mikaaah.tnkprogression.ProgressionAPI").getMethod("profile",ServerPlayer.class).invoke(null,player);
   profile.putInt("level",100);profile.putInt("ceiling",100);int rolls=0;
   for(int source:new int[]{1,10,11,30,31,50,51,70,71,80,81,94,95,100})for(int i=0;i<32;i++){
    var token=TnkLootRolls.roll(player,source,"tnk_test");require(!token.isEmpty(),"Missing tier pool");
    int tier=UnidentifiedWeaponItem.tier(token);require(tier>=LevelTierRoll.roll(source,0)&&tier<=LevelTierRoll.roll(source,99),"Wrong level band");
    require(!DimensionLoot.options(token).isEmpty(),"Empty reward preview");var before=token.copy();
    UnidentifiedWeaponItem.bindRollSeed(token);require(ItemStack.matches(before,token),"Seed/rarity changed on bind");
    var result=ApotheosisCompat.apply(UnidentifiedWeaponItem.baseResult(world,token),tier,player,UnidentifiedWeaponItem.data(token).getLong("roll_seed"));
    require(UnidentifiedWeaponItem.data(result).getString("tnk_rarity").equals(UnidentifiedWeaponItem.data(token).getString("tnk_rarity")),"Identification changed rarity");rolls++;
   }
   profile.putInt("level",1);profile.putInt("ceiling",10);
   for(int i=0;i<64;i++){
    var token=TnkLootRolls.roll(player,100,"tnk_test");require(UnidentifiedWeaponItem.tier(token)<=1,"Milestone ceiling bypassed");
    require(List.of(TnkLootRolls.RARITIES).indexOf(UnidentifiedWeaponItem.data(token).getString("tnk_rarity"))<=TnkLevelBridge.rarityCap(player),"Rarity cap bypassed");
   }
   var legacy=UnidentifiedWeaponItem.create(1);var data=UnidentifiedWeaponItem.data(legacy);data.putString("tnk_rarity","tnkloot:legendary");legacy.set(DataComponents.CUSTOM_DATA,CustomData.of(data));RaritySupport.ensure(legacy);
   require(UnidentifiedWeaponItem.data(legacy).getString("tnk_rarity").equals("legendary")&&legacy.get(DataComponents.CUSTOM_MODEL_DATA).value()==5,"Legacy beta rarity/texture migration failed");
   var locked=UnidentifiedWeaponItem.create(1);RaritySupport.force(locked,"mythic");TnkLootRolls.cap(locked,player,100);var snapshot=locked.copy();profile.putInt("level",100);profile.putInt("ceiling",100);TnkLootRolls.cap(locked,player,100);require(ItemStack.matches(snapshot,locked),"Trading/unlocking improved sealed rarity");
   var settings=new TnkBetaConfig();TnkBetaConfig.current=settings;settings.equipmentDropChance=0;
   var zombie=EntityType.ZOMBIE.create(world);var drops=new ArrayList<ItemEntity>();drops.add(new ItemEntity(world,0,0,0,new ItemStack(Items.DIAMOND_SWORD)));drops.add(new ItemEntity(world,0,0,0,new ItemStack(Items.ROTTEN_FLESH,3)));
   FinalLoot.drops(new LivingDropsEvent(zombie,player.damageSources().playerAttack(player),drops,true));require(drops.size()==1&&drops.getFirst().getItem().is(Items.ROTTEN_FLESH),"Zero chance/mob materials failed");
   settings.equipmentDropChance=1;zombie=EntityType.ZOMBIE.create(world);zombie.getPersistentData().putInt("tnk_level",100);drops=new ArrayList<>();drops.add(new ItemEntity(world,0,0,0,new ItemStack(Items.DIAMOND_SWORD,3)));
   var death=new LivingDropsEvent(zombie,player.damageSources().playerAttack(player),drops,true);FinalLoot.drops(death);require(drops.size()==1&&drops.getFirst().getItem().getItem() instanceof UnidentifiedWeaponItem,"Mob did not produce exactly one token");var saved=drops.getFirst().getItem().copy();FinalLoot.drops(death);require(drops.size()==1&&ItemStack.matches(saved,drops.getFirst().getItem()),"Repeated death pass rerolled loot");
   var params=new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withOptionalParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.CHEST);
   var context=new LootContext.Builder(params).create(Optional.empty());context.setQueriedLootTableId(ResourceLocation.parse("minecraft:chests/simple_dungeon"));
   settings.chestEquipmentChance=1;var loot=new ObjectArrayList<ItemStack>();loot.add(new ItemStack(Items.GOLDEN_SWORD));loot.add(new ItemStack(Items.DIAMOND,2));FinalLoot.table(loot,context);
   require(loot.stream().noneMatch(s->s.is(Items.GOLDEN_SWORD))&&loot.stream().anyMatch(s->s.is(Items.DIAMOND)&&s.getCount()==2)&&loot.stream().filter(s->UnidentifiedWeaponItem.data(s).getString("source_type").equals("tnk_chest")).count()==1,"Chest chance/materials failed");
   var chest=loot.stream().filter(s->UnidentifiedWeaponItem.data(s).getString("source_type").equals("tnk_chest")).findFirst().orElseThrow().copy();FinalLoot.table(loot,context);require(loot.stream().anyMatch(s->ItemStack.matches(s,chest)),"Nested table rerolled sealed loot");
   dev.mikaaah.tnkloot.boss.BossIntegration.verify(player);
   TnkLoot.LOG.info("TNKLOOT_BETA_OK: {} level-based identifications, milestone/rarity caps, old-beta texture migration, sealed trading, 0/100% mob/chest frequency, idempotence, materials and mixed boss editor",rolls);
  }catch(Throwable error){TnkLoot.LOG.error("TNKLOOT_BETA_FAILED",error);}finally{TnkBetaConfig.current=previous;server.halt(false);}
 }
}
