package dev.mikaaah.tnkloot.loot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
public final class TnkLootRolls {
 public static final String[] RARITIES={"common","uncommon","rare","epic","legendary","mythic"};
 public static ItemStack roll(ServerPlayer p,int sourceLevel,String source){
  int effective=Math.max(1,Math.min(100,Math.min(sourceLevel,TnkLevelBridge.ceiling(p))));
  int tier=LevelTierRoll.roll(effective,p.getRandom().nextInt(100));
  var categories=TnkConfig.get().weapons().stream().filter(w->tier>=w.minTier()&&tier<=w.maxTier()&&WeaponSelector.available(w))
    .map(TnkConfig.Weapon::category).filter(c->c==TnkConfig.Category.WEAPON||c==TnkConfig.Category.BOW||c==TnkConfig.Category.HELMET||c==TnkConfig.Category.CHESTPLATE||c==TnkConfig.Category.LEGGINGS||c==TnkConfig.Category.BOOTS).distinct().toList();
  if(categories.isEmpty())return ItemStack.EMPTY;
  ItemStack token=UnidentifiedWeaponItem.create(categories.get(p.getRandom().nextInt(categories.size())),tier,"",source);
  seal(token,p,effective);return token;
 }
 public static void seal(ItemStack token,ServerPlayer p,int effective){
  CompoundTag n=UnidentifiedWeaponItem.data(token);if(n.contains("tnk_rarity"))return;
  int max=Math.max(0,Math.min(5,TnkLevelBridge.rarityCap(p))),total=0;int[] weights=TnkBetaConfig.current.rarityWeights;
  for(int i=0;i<=max;i++)total+=weights[i];int roll=p.getRandom().nextInt(total),chosen=0;
  for(int i=0;i<=max;i++){roll-=weights[i];if(roll<0){chosen=i;break;}}
  n.putString("tnk_rarity","tnkloot:"+RARITIES[chosen]);n.putInt("tnk_source_level",effective);token.set(DataComponents.CUSTOM_DATA,CustomData.of(n));
 }
}
