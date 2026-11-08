package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public final class UnidentifiedWeaponItem extends Item {
   private final TnkConfig.Category category;
   private final boolean boss;

   public UnidentifiedWeaponItem(Properties properties, TnkConfig.Category category) {
      this(properties, category, false);
   }

   public UnidentifiedWeaponItem(Properties properties, TnkConfig.Category category, boolean boss) {
      super(properties);
      this.category = category;
      this.boss = boss;
   }

   public boolean boss() {
      return this.boss;
   }

   public TnkConfig.Category category() {
      return this.category;
   }

   public static ItemStack create(int rank) {
      return create(TnkConfig.Category.WEAPON, rank);
   }

   public static ItemStack create(TnkConfig.Category category, int rank) {
      return create(category, rank, "", "manual");
   }

   public static ItemStack create(TnkConfig.Category category, int rank, String sourceItem, String sourceType) {
      ItemStack stack = new ItemStack((ItemLike)TnkLoot.token(category).get());
      CompoundTag tag = new CompoundTag();
      int tier = TnkConfig.get().tierForRank(rank).rank();
      tag.putInt("tier", tier);
      tag.putString("tier_name", TnkConfig.get().tierForRank(tier).name());
      tag.putBoolean("seed_bound", true);
      tag.putLong("roll_seed", ThreadLocalRandom.current().nextLong());
      if (!sourceItem.isBlank()) {
         tag.putString("source_item", sourceItem);
      }

      tag.putString("source_type", sourceType);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return RaritySupport.ensure(stack);
   }

   public static ItemStack creativeSample(TnkConfig.Category category, int rank) {
      ItemStack stack = create(category, rank, "", "creative");
      CompoundTag tag = data(stack);
      tag.remove("roll_seed");
      tag.putBoolean("seed_bound", false);
      tag.putBoolean("creative_template", true);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return RaritySupport.template(stack);
   }

   public static boolean bindRollSeed(ItemStack stack) {
      RaritySupport.beforeBind(stack);
      if (!(stack.getItem() instanceof UnidentifiedWeaponItem)) {
         return RaritySupport.bound(stack, false);
      } else {
         CompoundTag tag = data(stack);
         if (tag.getBoolean("seed_bound") && tag.contains("roll_seed", 4) && !tag.getBoolean("creative_template")) {
            return RaritySupport.bound(stack, false);
         } else {
            if (tag.getBoolean("creative_template")
               || !tag.contains("roll_seed", 4)
               || tag.getString("source_type").equals("manual")
               || tag.getString("source_type").equals("creative")) {
               tag.putLong("roll_seed", ThreadLocalRandom.current().nextLong());
            }

            tag.remove("creative_template");
            tag.putBoolean("seed_bound", true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return RaritySupport.bound(stack, true);
         }
      }
   }

   public static CompoundTag data(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? new CompoundTag() : data.copyTag();
   }

   public static int tier(ItemStack stack) {
      return Math.max(0, data(stack).getInt("tier"));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      return InteractionResultHolder.pass(player.getItemInHand(hand));
   }

   public static ItemStack baseResult(Level var0, ItemStack var1) {
      return DimensionLoot.baseResult(var0, var1);
   }

   public static int cost(ItemStack token, ItemStack result) {
      TnkConfig.Costs costs = TnkConfig.get().costs();
      return RaritySupport.cost(costs.enabled() ? costs.amount(tier(token), BuiltInRegistries.ITEM.getKey(result.getItem()).toString()) : 0, token, result);
   }

   public static boolean has(ServerPlayer player, Item item, int amount) {
      int found = 0;

      for (ItemStack stack : player.getInventory().items) {
         if (stack.is(item)) {
            found += stack.getCount();
         }
      }

      for (ItemStack stackx : player.getInventory().offhand) {
         if (stackx.is(item)) {
            found += stackx.getCount();
         }
      }

      return found >= amount;
   }

   public static void remove(ServerPlayer player, Item item, int amount) {
      for (ItemStack stack : player.getInventory().items) {
         int taken = Math.min(amount, stack.is(item) ? stack.getCount() : 0);
         if (taken > 0) {
            stack.shrink(taken);
            amount -= taken;
         }

         if (amount == 0) {
            return;
         }
      }

      for (ItemStack stack : player.getInventory().offhand) {
         int takenx = Math.min(amount, stack.is(item) ? stack.getCount() : 0);
         if (takenx > 0) {
            stack.shrink(takenx);
            amount -= takenx;
         }

         if (amount == 0) {
            return;
         }
      }
   }

   public void appendHoverText(ItemStack var1, TooltipContext var2, List<Component> var3, TooltipFlag var4) {
      RaritySupport.tooltip(var1, var3);
   }
}
