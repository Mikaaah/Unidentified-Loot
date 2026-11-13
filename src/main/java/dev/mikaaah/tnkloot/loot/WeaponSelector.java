package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class WeaponSelector {
   private WeaponSelector() {
   }

   public static boolean available(TnkConfig.Weapon weapon) {
      ResourceLocation id = ResourceLocation.parse(weapon.id());
      if(dev.mikaaah.tnkloot.compat.PresetCleanup.excluded(weapon.id()) || !weapon.enabled() || !BuiltInRegistries.ITEM.containsKey(id) || BuiltInRegistries.ITEM.get(id)==Items.AIR)return false;
      // These generic trinkets get their entire effect from affixes. Without a
      // matching Curios slot/category they would identify into unusable blanks.
      if(id.getNamespace().equals("apothic_compats")) {
         try { if(dev.shadowsoffire.apotheosis.loot.LootCategory.forItem(new ItemStack(BuiltInRegistries.ITEM.get(id))).isNone())return false; }
         catch(UnsupportedOperationException notRegisteredYet) { /* Re-evaluate after registry baking. */ }
      }
      return true;
   }

   public static WeightedPool<Item> pool(int tier) {
      return pool(tier, TnkConfig.Category.WEAPON);
   }

   public static WeightedPool<Item> pool(int tier, TnkConfig.Category category) {
      List<WeightedPool.Entry<Item>> entries = new ArrayList<>();

      for (TnkConfig.Weapon weapon : TnkConfig.tierWeapons(tier)) {
         if (weapon.category() == category && tier >= weapon.minTier() && tier <= weapon.maxTier() && available(weapon) && !BossRewards.exclusive(weapon.id())) {
            entries.add(new WeightedPool.Entry<>((Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(weapon.id())), weapon.weight()));
         }
      }

      return new WeightedPool<>(entries);
   }

   public static ItemStack roll(Level level, int tier) {
      return roll(level, tier, TnkConfig.Category.WEAPON);
   }

   public static ItemStack roll(Level level, int tier, TnkConfig.Category category) {
      return roll(level, tier, category, level.getRandom());
   }

   public static ItemStack roll(Level level, int tier, TnkConfig.Category category, RandomSource random) {
      return pool(tier, category).roll(random::nextLong).<ItemStack>map(ItemStack::new).orElse(ItemStack.EMPTY);
   }
}
