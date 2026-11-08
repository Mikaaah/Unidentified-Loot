package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import dev.mikaaah.tnkloot.features.FeatureHooks;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** A single regular-equipment roll; explicit boss rewards use the full boss path. */
public final class BetaLoot {
 private BetaLoot(){}
 private static boolean special(ItemStack s){return s.getOrDefault(Apoth.Components.FROM_BOSS,false)||UnidentifiedWeaponItem.data(s).getBoolean("apotheosis_invader")||BossRewards.exclusive(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())||(s.getItem() instanceof UnidentifiedWeaponItem item&&item.boss());}
 private static boolean equipment(ItemStack s){
  if(s.getItem() instanceof UnidentifiedWeaponItem item)return !item.boss();
  return dev.mikaaah.tnkloot.compat.PackItemPolicy.allowed(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())&&!FinalLoot.own(s)&&!dev.mikaaah.tnkloot.injection.LootProtection.keepNative(s)&&(AffixHelper.hasAffixes(s)||FinalLoot.category(s)!=null);
 }
 public static ServerPlayer player(LootContext ctx){
  var killer=ctx.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);if(killer instanceof ServerPlayer p)return p;
  var actor=ctx.getParamOrNull(LootContextParams.THIS_ENTITY);if(actor instanceof ServerPlayer p)return p;
  var attacker=ctx.getParamOrNull(LootContextParams.ATTACKING_ENTITY);return attacker instanceof ServerPlayer p?p:null;
 }
 public static boolean table(ObjectArrayList<ItemStack> loot,LootContext ctx){
  if(DimensionLoot.rewardPool(ctx).isPresent()||loot.stream().anyMatch(BetaLoot::special))return false;
  var entity=ctx.getParamOrNull(LootContextParams.THIS_ENTITY);
  if(entity instanceof Mob&&entity instanceof Enemy){loot.removeIf(BetaLoot::equipment);return true;}
  var table=ctx.getQueriedLootTableId();if(table==null||!table.getPath().startsWith("chests/"))return false;
  if(loot.stream().anyMatch(s->UnidentifiedWeaponItem.data(s).getBoolean("tnk_progression_sealed")))return true;
  var player=player(ctx);
  loot.removeIf(BetaLoot::equipment);
  if(player!=null&&ctx.getRandom().nextDouble()<TnkBetaConfig.current.chestEquipmentChance){
   var token=TnkLootRolls.roll(player,TnkLevelBridge.level(player),"tnk_chest");if(!token.isEmpty())loot.add(token);
  }
  FeatureHooks.dungeon(loot,ctx);
  for(var token:loot)if(token.getItem() instanceof UnidentifiedWeaponItem){DimensionLoot.bindOrigin(token,ctx.getLevel());if(player!=null)TnkLootRolls.cap(token,player,TnkLevelBridge.level(player));}
  return true;
 }
 public static boolean drops(LivingDropsEvent event){
  var mob=event.getEntity();
  if(!(mob instanceof Mob)||!(mob instanceof Enemy)||BossRewards.source(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString()).isPresent()||event.getDrops().stream().anyMatch(d->special(d.getItem())))return false;
  var player=event.getSource().getEntity() instanceof ServerPlayer p?p:mob.getKillCredit() instanceof ServerPlayer p?p:null;
  if(player==null)return true;
  if(mob.getPersistentData().getBoolean("tnk_beta_loot_done"))return true;
  mob.getPersistentData().putBoolean("tnk_beta_loot_done",true);
  event.getDrops().removeIf(d->equipment(d.getItem()));
  if(player.getRandom().nextDouble()<TnkBetaConfig.current.equipmentDropChance){
   var token=TnkLootRolls.roll(player,TnkLevelBridge.mobLevel(mob),"tnk_entity_drop");
   if(!token.isEmpty())event.getDrops().add(new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),token));
  }
  return true;
 }
}
