package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ProgressionRewards {
   private ProgressionRewards() {
   }

   public static ItemStack equipment(ServerPlayer recipient, TnkConfig.Category category) {
      int level = TnkLevelBridge.level(recipient);
      int tier = TnkLootRolls.tier(recipient, level);
      Level origin = recipient.level();
      tier = DimensionLoot.eligibleTier(tier, category, origin.dimension().location().toString());
      if (tier < 0) {
         return ItemStack.EMPTY;
      } else {
         ItemStack token = UnidentifiedWeaponItem.create(category, tier, "", "tnk_quest_reward");
         DimensionLoot.bindOrigin(token, origin);
         TnkLootRolls.seal(token, recipient, level);
         return token;
      }
   }

   public static ItemStack boss(ServerPlayer recipient, BossRewards.Pool pool) {
      if (BossRewards.options(pool, TnkConfig.Category.WEAPON).isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack token = BossRewards.token(pool, TnkConfig.Category.WEAPON, recipient.level(), ItemStack.EMPTY);
         TnkLootRolls.cap(token, recipient, TnkLevelBridge.level(recipient));
         return token;
      }
   }
}
