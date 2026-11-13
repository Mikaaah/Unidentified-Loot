package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.rarity.NativeQuality;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import dev.shadowsoffire.apotheosis.Apoth.Components;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.LootRule;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.loot.LootRule.AffixLootRule;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ApotheosisCompat {
   private ApotheosisCompat() {
   }

   public static ItemStack apply(ItemStack stack, int rank, Player player) {
      return apply(stack, rank, player, player.getRandom().nextLong());
   }

   public static ItemStack apply(ItemStack stack, int rank, Player player, long seed) {
      if (dev.mikaaah.tnkloot.dungeon.DungeonEquipment.authored(stack)) return stack;
      stack = NativeQuality.prepare(stack, rank, player, seed);
      TnkConfig.get().tierForRank(rank).apotheosisRarity();
      ResourceLocation wanted = RaritySupport.resolve(stack, rank, seed);
      LootRarity rarity = RarityRegistry.getSortedRarities()
         .stream()
         .filter(r -> wanted.equals(RarityRegistry.INSTANCE.getKey(r)))
         .findFirst()
         .orElseThrow(() -> new IllegalStateException("Unknown Apotheosis rarity " + wanted));
      return applyContext(stack, rank, LootCategory.forItem(stack), rarity, GenContext.forPlayer(RandomSource.create(seed ^ 23729983353474900L), player), seed);
   }

   public static ItemStack applyContext(ItemStack stack,int rank,LootCategory category,LootRarity rarity,GenContext context,long seed) {
      ResourceLocation wanted = RarityRegistry.INSTANCE.getKey(rarity);
      if (category.isNone()) {
         return RaritySupport.finish(stack, rank, seed);
      } else {
         stack.set(Components.AFFIXES, ItemAffixes.EMPTY);
         AffixHelper.setRarity(stack, rarity);

         for (LootRule rule : rarity.getRules(category)) {
            if (!(rule instanceof AffixLootRule affix)
               || !LootController.getWeightedAffixes(stack, rarity, affix.type(), context).stream().noneMatch(a -> a.getWeight().asInt() > 0)) {
               rule.execute(stack, rarity, context);
            }
         }

         ItemAffixes affixes = (ItemAffixes)stack.getOrDefault(Components.AFFIXES, ItemAffixes.EMPTY);
         if (affixes.size() == 0) {
            throw new IllegalStateException("No eligible affixes for " + stack.getItem() + " at rarity " + wanted);
         } else {
            ArrayList<DynamicHolder<Affix>> names = new ArrayList<>(affixes.keySet());
            String key = names.size() > 1 ? "misc.apotheosis.affix_name.three" : "misc.apotheosis.affix_name.two";
            AffixHelper.setName(
               stack,
               Component.translatable(
                     key,
                     new Object[]{((Affix)names.getFirst().get()).getName(true), "", names.size() > 1 ? ((Affix)names.getLast().get()).getName(false) : ""}
                  )
                  .withStyle(Style.EMPTY.withColor(rarity.color()).withItalic(false))
            );
            stack.remove(Components.TOUCHED_BY_MALICE);
            return RaritySupport.finish(stack, rank, seed);
         }
      }
   }
}
