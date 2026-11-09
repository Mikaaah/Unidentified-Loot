package dev.mikaaah.tnkloot.injection;
import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.*;
import dev.mikaaah.tnkloot.features.FeatureHooks;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.progression.ProgressionService;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import java.util.*;

/** Final loot-table output and captured death drops, never arbitrary world item spawns. */
public final class FinalLoot {
 private record Lookup(Object settings,Map<String,TnkConfig.Weapon> items){}
 private static volatile Lookup cache;
 private record BossLookup(Object definitions,Map<String,List<BossRewards.Pool>> owners){}
 private static volatile BossLookup bossCache;
 private static List<BossRewards.Pool> owners(String id){var definitions=BossRewards.all();var c=bossCache;if(c==null||c.definitions()!=definitions){var map=new HashMap<String,List<BossRewards.Pool>>();for(var pool:BossRewards.active())for(var reward:pool.items()){var list=map.computeIfAbsent(reward.item(),key->new ArrayList<>());if(!list.contains(pool))list.add(pool);}var frozen=new HashMap<String,List<BossRewards.Pool>>();map.forEach((key,value)->frozen.put(key,List.copyOf(value)));bossCache=c=new BossLookup(definitions,Map.copyOf(frozen));}return c.owners().getOrDefault(id,List.of());}
 public static void register(){NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,FinalLoot::drops);NeoForge.EVENT_BUS.addListener(InjectionIntegration::run);NeoForge.EVENT_BUS.addListener(AdvancementRewardIntegration::run);}
 private static Map<String,TnkConfig.Weapon> items(){var s=TnkConfig.get();var c=cache;if(c==null||c.settings()!=s){var map=new HashMap<String,TnkConfig.Weapon>();for(var w:s.weapons())map.merge(w.id(),w,(a,b)->!a.enabled()&&b.enabled()?b:a);cache=c=new Lookup(s,Map.copyOf(map));}return c.items();}
 public static boolean enabled(){return TnkConfig.get().enableIdentification()&&TnkConfig.get().replaceWeaponLoot();}
 public static boolean own(ItemStack s){return s.getItem() instanceof UnidentifiedWeaponItem||(UnidentifiedWeaponItem.data(s).contains("tnk_rarity")&&!UnidentifiedWeaponItem.data(s).getBoolean("tnk_external_generated"));}
 public static TnkConfig.Category category(ItemStack s){
  if(!dev.mikaaah.tnkloot.compat.PackItemPolicy.allowed(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()))return null;
  var defined=items().get(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());if(defined!=null)return defined.category();
  // Decorative blocks are not equipment unless explicitly entered in a reward pool.
  if(s.getItem() instanceof BlockItem)return null;
  var found=BaseCompat.category(s.getItem());if(found!=null)return found;
  var apoth=LootCategory.forItem(s);if(apoth.isMelee())return TnkConfig.Category.WEAPON;if(apoth.isRanged())return TnkConfig.Category.BOW;if(apoth.isBreaker()||apoth.isDefensive())return TnkConfig.Category.TOOL;return null;
 }
 /** An empty replacement means the original drop remains, including its components. */
 public static ItemStack replacement(ItemStack original,ServerLevel level,ServerPlayer player,String source){
  if(!enabled()||original.isEmpty()||!dev.mikaaah.tnkloot.compat.PackItemPolicy.allowed(BuiltInRegistries.ITEM.getKey(original.getItem()).toString()))return ItemStack.EMPTY;
  if(dev.mikaaah.tnkloot.apotheosis.InvaderRewards.raw(original))return dev.mikaaah.tnkloot.apotheosis.InvaderRewards.token(original,level,player);
  if(own(original)||LootProtection.keepNative(original))return ItemStack.EMPTY;
  String id=BuiltInRegistries.ITEM.getKey(original.getItem()).toString();
  // Unique native boss prizes still produce the corresponding universal crate.
  var owners=owners(id);
  if(owners.size()==1)return BossRewards.token(owners.getFirst(),TnkConfig.Category.WEAPON,level,ItemStack.EMPTY);
  var category=category(original);if(category==null)return ItemStack.EMPTY;
  var def=items().get(id);int rank=player==null?0:ProgressionService.getTier(player);int[] band=def==null?BaseCompat.band(id,original.getItem(),category):new int[]{def.minTier(),def.maxTier()};
  int wanted=Math.min(rank,band[1]);String origin=DimensionRules.infer(id);if(origin.isBlank())origin=level.dimension().location().toString();
  int tier=DimensionLoot.eligibleTier(wanted,category,origin);if(tier<0)return ItemStack.EMPTY;
  boolean invader=original.getOrDefault(Apoth.Components.FROM_BOSS,false);
  var token=UnidentifiedWeaponItem.create(category,tier,id,invader?"boss_reward":source);var tag=UnidentifiedWeaponItem.data(token);tag.putString("origin_dimension",origin);
  if(invader){tag.putString("boss_name","Apotheosis Invader");tag.putBoolean("apotheosis_invader",true);}
  token.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));RaritySupport.ensure(token);if(player==null&&!invader)TnkLootRolls.sealStarter(token);return token;
 }
 public static void table(ObjectArrayList<ItemStack> loot,LootContext ctx){
  if(!enabled())return;
  // Advancement tables may even reuse chest IDs. Check the actual origin before
  // replacement, beta rolls, bonus accessories, boss routing or tier capping.
  var params=((dev.mikaaah.tnkloot.injection.mixin.LootContextAccess)ctx).tnkloot$params();
  if(((LootParamsSource)params).tnkloot$isAdvancementReward())return;
  if(DimensionLoot.rewardPool(ctx).isEmpty()) {
   var generatedContext=dev.mikaaah.tnkloot.apotheosis.NativeLootPools.context(ctx);
   loot.removeIf(stack->stack.getOrDefault(Apoth.Components.FROM_CHEST,false)&&!own(stack)
    &&UnidentifiedWeaponItem.data(stack).getBoolean("tnk_external_generated")&&UnidentifiedWeaponItem.data(stack).getBoolean("tnk_random_pool")
    &&!dev.mikaaah.tnkloot.apotheosis.NativeLootPools.allowed(stack,generatedContext));
  }
  if(BetaLoot.table(loot,ctx))return;
  var entity=ctx.getParamOrNull(LootContextParams.THIS_ENTITY);if(entity instanceof Player&&ctx.hasParam(LootContextParams.DAMAGE_SOURCE))return;
  ServerPlayer player=ctx.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) instanceof ServerPlayer p?p:entity instanceof ServerPlayer p?p:ctx.getParamOrNull(LootContextParams.ATTACKING_ENTITY) instanceof ServerPlayer p?p:null;
  // Earned caches are consolidated before the generic equipment pass.
  if(DimensionLoot.rewardPool(ctx).isPresent())DimensionLoot.route(loot,ctx);
  var result=new ObjectArrayList<ItemStack>();
  for(var stack:loot){var token=replacement(stack,ctx.getLevel(),player,"loot_table");if(token.isEmpty())result.add(stack);else for(int n=0;n<stack.getCount();n++)result.add(n==0?token:replacement(stack,ctx.getLevel(),player,"loot_table"));}
  loot.clear();loot.addAll(result);
  var reward=DimensionLoot.rewardPool(ctx);
  if(reward.isPresent()&&loot.stream().anyMatch(s->s.getItem() instanceof UnidentifiedWeaponItem&&UnidentifiedWeaponItem.data(s).getString("boss_pool").equals(reward.get().id()))){
   boolean kept=false;var ids=reward.get().items().stream().map(BossRewards.Reward::item).collect(java.util.stream.Collectors.toSet());
   for(var it=loot.iterator();it.hasNext();){var s=it.next();if(s.getItem() instanceof UnidentifiedWeaponItem&&UnidentifiedWeaponItem.data(s).getString("boss_pool").equals(reward.get().id())){if(kept)it.remove();else{s.setCount(1);kept=true;}}else if(!own(s)&&!LootProtection.keepNative(s)&&ids.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()))it.remove();}
  }
  FeatureHooks.dungeon(loot,ctx);DimensionLoot.route(loot,ctx);
  if(player!=null)for(var token:loot)TnkLootRolls.cap(token,player,TnkLevelBridge.level(player));
 }
 public static void drops(LivingDropsEvent event){
  if(event.isCanceled()||!enabled()||event.getEntity() instanceof Player||!(event.getEntity().level() instanceof ServerLevel level))return;
  if(BetaLoot.drops(event))return;
  BossRewards.drops(event);
  var boss=BossRewards.source(BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString());
  if(boss.isPresent()&&event.getDrops().stream().anyMatch(d->d.getItem().getItem() instanceof UnidentifiedWeaponItem&&UnidentifiedWeaponItem.data(d.getItem()).getString("boss_pool").equals(boss.get().id()))){
   boolean kept=false;var ids=boss.get().items().stream().map(BossRewards.Reward::item).collect(java.util.stream.Collectors.toSet());
   for(var it=event.getDrops().iterator();it.hasNext();){var s=it.next().getItem();if(s.getItem() instanceof UnidentifiedWeaponItem&&UnidentifiedWeaponItem.data(s).getString("boss_pool").equals(boss.get().id())){if(kept)it.remove();else{s.setCount(1);kept=true;}}else if(!own(s)&&!LootProtection.keepNative(s)&&ids.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()))it.remove();}
  }
  var player=event.getSource().getEntity() instanceof ServerPlayer p?p:event.getEntity().getKillCredit() instanceof ServerPlayer p?p:null;
  var extra=new ArrayList<ItemEntity>();
  for(var entity:event.getDrops()){
   var original=entity.getItem();var token=replacement(original,level,player,"entity_drop");if(token.isEmpty())continue;
   int count=original.getCount();entity.setItem(token);
   for(int n=1;n<count;n++){var copy=new ItemEntity(level,entity.getX(),entity.getY(),entity.getZ(),replacement(original,level,player,"entity_drop"));copy.setDeltaMovement(entity.getDeltaMovement());copy.setPickUpDelay(10);extra.add(copy);}
  }event.getDrops().addAll(extra);
  if(player!=null)for(var drop:event.getDrops())TnkLootRolls.cap(drop.getItem(),player,TnkLevelBridge.mobLevel(event.getEntity()));
 }
}
