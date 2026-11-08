package dev.mikaaah.tnkloot.dimension;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.menu.TnkMenu;
import dev.mikaaah.tnkloot.features.*;
import dev.mikaaah.tnkloot.rarity.*;
import dev.mikaaah.tnkloot.progression.ProgressionService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.*;
public final class DimensionLoot {
 public static String origin(ItemStack token){String s=UnidentifiedWeaponItem.data(token).getString("origin_dimension");return s.isBlank()?"minecraft:overworld":s;}
 public static void bindOrigin(ItemStack token,Level level){if(!(token.getItem() instanceof UnidentifiedWeaponItem)||level==null)return;var tag=UnidentifiedWeaponItem.data(token);if(tag.contains("origin_dimension"))return;String inferred=DimensionRules.infer(tag.getString("source_item"));tag.putString("origin_dimension",inferred.isBlank()?level.dimension().location().toString():inferred);token.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
 public static List<TnkConfig.Weapon> pool(int tier,TnkConfig.Category category,String origin){return TnkConfig.tierWeapons(tier).stream().filter(w->w.category()==category&&tier>=w.minTier()&&tier<=w.maxTier()&&WeaponSelector.available(w)&&!BossRewards.exclusive(w.id())&&DimensionRules.allowed(w.id(),origin)).toList();}
 public static ItemStack baseResult(Level level,ItemStack token){
  if(!(token.getItem() instanceof UnidentifiedWeaponItem item))return ItemStack.EMPTY;bindOrigin(token,level);ItemStack result;
  if(item.boss())result=BossRewards.baseResult(level,token);
  else{var entries=pool(UnidentifiedWeaponItem.tier(token),item.category(),origin(token)).stream().map(w->new WeightedPool.Entry<Item>(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(w.id())),w.weight())).toList();var random=new Random(UnidentifiedWeaponItem.data(token).getLong("roll_seed"));result=new WeightedPool<Item>(entries).roll(random::nextLong).map(ItemStack::new).orElse(ItemStack.EMPTY);}
  return RaritySupport.base(result,token);
 }
 public static List<FeatureMenus.Option> options(ItemStack token){
  if(!(token.getItem() instanceof UnidentifiedWeaponItem item))return List.of();var tag=UnidentifiedWeaponItem.data(token);
  if(item.boss()){
   var pool=BossRewards.find(tag.getString("boss_pool"));if(pool.isEmpty())return List.of();
   if(tag.contains("boss_result")){String id=tag.getCompound("boss_result").getString("id");return id.isBlank()?List.of():List.of(new FeatureMenus.Option(id,1,pool.get().name()));}
   return pool.get().items().stream().filter(w->(tag.getBoolean("boss_universal")||w.category()==item.category())&&BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.parse(w.item()))).map(w->new FeatureMenus.Option(w.item(),w.weight(),pool.get().name())).toList();
  }
  return pool(UnidentifiedWeaponItem.tier(token),item.category(),origin(token)).stream().map(w->new FeatureMenus.Option(w.id(),w.weight(),origin(token)+" · Tier "+UnidentifiedWeaponItem.tier(token))).toList();
 }
 @SuppressWarnings({"rawtypes","unchecked"}) public static Object setPreview(List previews,int index,Object ignored,TnkMenu menu,ServerPlayer player){
  var token=menu.getSlot(index).getItem();bindOrigin(token,player.level());var list=options(token).stream().map(o->NativeDisplays.prepare(new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(o.id()))),player,UnidentifiedWeaponItem.tier(token),UnidentifiedWeaponItem.data(token).getLong("roll_seed"))).toList();
  if(list.isEmpty())throw new IllegalStateException("No equipment in the origin dimension pool");return previews.set(index,list);
 }
 public static int eligibleTier(int requested,TnkConfig.Category category,String origin){return TnkConfig.get().tiers().stream().map(TnkConfig.Tier::rank).filter(t->t<=requested).sorted(Comparator.reverseOrder()).filter(t->!pool(t,category,origin).isEmpty()).findFirst().orElse(-1);}
 public static java.util.Optional<BossRewards.Pool> rewardPool(LootContext ctx){
  var found=BossRewards.source(ctx);if(found.isPresent())return found;
  String table=ctx.getQueriedLootTableId()==null?"":ctx.getQueriedLootTableId().toString();
  String id=table.startsWith("aether:chests/dungeon/bronze/")&&(table.contains("treasure")||table.contains("reward"))?"aether/slider":table.startsWith("aether:chests/dungeon/silver/")&&(table.contains("treasure")||table.contains("reward")||table.contains("valkyrie"))?"aether/valkyrie_queen":table.startsWith("aether:chests/dungeon/gold/")&&(table.contains("treasure")||table.contains("reward"))?"aether/sun_spirit":table.startsWith("deep_aether:chests/dungeon/brass/")&&(table.contains("treasure")||table.contains("reward")||table.contains("stormforged"))?"deep_aether/eots_controller":table.equals("twilightforest:darktower_boss")?"twilightforest/ur_ghast":"";
  return id.isBlank()?java.util.Optional.empty():BossRewards.find(id);
 }
 public static ObjectArrayList<ItemStack> route(ObjectArrayList<ItemStack> loot,LootContext ctx){
  if(!TnkConfig.get().enableIdentification()||!TnkConfig.get().replaceWeaponLoot())return loot;
  var boss=rewardPool(ctx);
  if(boss.isPresent()){
   var p=boss.get();var ids=p.items().stream().map(BossRewards.Reward::item).collect(java.util.stream.Collectors.toSet());
   boolean already=loot.stream().anyMatch(s->s.getItem() instanceof UnidentifiedWeaponItem&&UnidentifiedWeaponItem.data(s).getString("boss_pool").equals(p.id()));
   if(!already&&loot.stream().anyMatch(s->!UnidentifiedWeaponItem.data(s).contains("tnk_rarity")&&!dev.mikaaah.tnkloot.injection.LootProtection.keepNative(s)&&ids.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()))){loot.removeIf(s->!UnidentifiedWeaponItem.data(s).contains("tnk_rarity")&&!dev.mikaaah.tnkloot.injection.LootProtection.keepNative(s)&&ids.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));loot.add(BossRewards.token(p,TnkConfig.Category.WEAPON,ctx.getLevel(),ItemStack.EMPTY));}
   for(var s:loot)if(s.getItem() instanceof UnidentifiedWeaponItem)bindOrigin(s,ctx.getLevel());return loot;
  }
  for(var s:loot)if(s.getItem() instanceof UnidentifiedWeaponItem)bindOrigin(s,ctx.getLevel());
  String dimension=ctx.getLevel().dimension().location().toString();var route=DimensionRules.get().routes().get(dimension);
  if(!DimensionRules.get().enabled()||route==null||loot.stream().anyMatch(s->s.getItem() instanceof UnidentifiedWeaponItem))return loot;
  var entity=ctx.getParamOrNull(LootContextParams.THIS_ENTITY);var killer=ctx.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);ServerPlayer player=entity instanceof ServerPlayer p?p:killer instanceof ServerPlayer p?p:null;
  String table=ctx.getQueriedLootTableId()==null?"":ctx.getQueriedLootTableId().toString();boolean chest=FeatureHooks.matches(table)&&(entity==null||entity instanceof ServerPlayer);
  boolean hostile=entity instanceof LivingEntity living&&living.getType().getCategory()==MobCategory.MONSTER&&player!=null&&BossRewards.source(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString()).isEmpty();
  if(!chest&&!hostile)return loot;double chance=chest?route.chestChance():route.mobChance();if(ctx.getRandom().nextDouble()>=chance)return loot;
  int tier=player==null?0:ProgressionService.getTier(player);List<TnkConfig.Category> categories=Arrays.stream(TnkConfig.Category.values()).filter(c->!pool(tier,c,dimension).isEmpty()).toList();if(categories.isEmpty())return loot;
  var token=UnidentifiedWeaponItem.create(categories.get(ctx.getRandom().nextInt(categories.size())),tier,"","dimension_bonus");bindOrigin(token,ctx.getLevel());loot.add(token);return loot;
 }
}
