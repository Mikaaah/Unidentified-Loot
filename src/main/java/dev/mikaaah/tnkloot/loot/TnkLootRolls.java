package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.Arrays;

/** Generation uses progression; a token's subsequent owner cannot reroll it. */
public final class TnkLootRolls {
 public static final String[] RARITIES={"common","uncommon","rare","epic","legendary","mythic"};
 public static int effective(ServerPlayer p,int source){return Math.max(1,Math.min(100,Math.min(source,TnkLevelBridge.ceiling(p))));}
 public static int tier(ServerPlayer p,int source){return LevelTierRoll.roll(effective(p,source),p.getRandom().nextInt(100));}
 public static ItemStack roll(ServerPlayer p,int sourceLevel,String source){
  int effective=effective(p,sourceLevel),tier=tier(p,sourceLevel);
  String origin=p.level().dimension().location().toString();
  var categories=Arrays.stream(TnkConfig.Category.values()).filter(c->!DimensionLoot.pool(tier,c,origin).isEmpty()).toList();
  if(categories.isEmpty())return ItemStack.EMPTY;
  ItemStack token=UnidentifiedWeaponItem.create(categories.get(p.getRandom().nextInt(categories.size())),tier,"",source);
  DimensionLoot.bindOrigin(token,p.level());seal(token,p,effective);return token;
 }
 public static void seal(ItemStack token,ServerPlayer p,int source){
  int max=Math.max(0,Math.min(5,TnkLevelBridge.rarityCap(p)));long total=0;int[] weights=TnkBetaConfig.current.rarityWeights;
  for(int i=0;i<=max;i++)total+=weights[i];long ticket=total>0?new java.util.Random(p.getRandom().nextLong()).nextLong(total):0;int chosen=0;
  for(int i=0;i<=max;i++){ticket-=weights[i];if(ticket<0){chosen=i;break;}}
  RaritySupport.force(token,RARITIES[chosen]);stamp(token,p,source);
 }
 /** Preserve the boss distribution while keeping it within the generating player's cap. */
 public static void cap(ItemStack token,ServerPlayer p,int source){
  if(!(token.getItem() instanceof UnidentifiedWeaponItem)||UnidentifiedWeaponItem.data(token).getBoolean("tnk_progression_sealed"))return;
  var original=UnidentifiedWeaponItem.data(token);
  if(!original.getString("source_type").equals("boss_reward")&&!original.contains("boss_pool")&&!original.getBoolean("apotheosis_invader")) {seal(token,p,source);return;}
  String rarity=UnidentifiedWeaponItem.data(token).getString("tnk_rarity").replace("tnkloot:","");
  int index=java.util.List.of(RARITIES).indexOf(rarity),max=Math.max(0,Math.min(5,TnkLevelBridge.rarityCap(p)));
  if(index>max){
   var settings=dev.mikaaah.tnkloot.rarity.RarityPolicy.get();long total=0;
   for(int i=0;i<=max;i++)total+=settings.quality(RARITIES[i]).bossWeight();
   int chosen=0;if(total>0){long ticket=new java.util.Random(UnidentifiedWeaponItem.data(token).getLong("roll_seed")^0x544e4b434150L).nextLong(total);
    for(int i=0;i<=max;i++){ticket-=settings.quality(RARITIES[i]).bossWeight();if(ticket<0){chosen=i;break;}}
   }RaritySupport.force(token,RARITIES[chosen]);
  }
  stamp(token,p,source);RaritySupport.ensure(token);
 }
 private static void stamp(ItemStack token,ServerPlayer p,int source){
  var tag=UnidentifiedWeaponItem.data(token);tag.putInt("tnk_source_level",effective(p,source));tag.putBoolean("tnk_progression_sealed",true);
  token.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
 }
}
