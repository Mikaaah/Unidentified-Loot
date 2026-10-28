package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;

/** Required, compile-checked Apotheosis API. Missing affixes reject the transaction. */
public final class ApotheosisCompat {
    private ApotheosisCompat() {}
    public static ItemStack apply(ItemStack stack, int rank, Player player) {
        return apply(stack, rank, player, player.getRandom().nextLong());
    }
    public static ItemStack apply(ItemStack stack, int rank, Player player, long seed) {
        return applyRarity(stack, net.minecraft.resources.ResourceLocation.parse("tnkloot:common"), player, seed);
    }
    public static ItemStack applyToken(ItemStack stack, ItemStack token, net.minecraft.server.level.ServerPlayer player) {
        var data = UnidentifiedWeaponItem.data(token);
        return applyRarity(stack, net.minecraft.resources.ResourceLocation.parse(data.contains("tnk_rarity") ? data.getString("tnk_rarity") : "tnkloot:common"), player, data.getLong("roll_seed"));
    }
    private static ItemStack applyRarity(ItemStack stack, net.minecraft.resources.ResourceLocation wanted, Player player, long seed) {
        LootRarity rarity = RarityRegistry.getSortedRarities().stream()
            .filter(r -> wanted.equals(RarityRegistry.INSTANCE.getKey(r))).findFirst()
            .orElseThrow(() -> new IllegalStateException("Unknown Apotheosis rarity " + wanted));
        LootCategory category = LootCategory.forItem(stack);
        if (category.isNone()) throw new IllegalStateException("No Apotheosis loot category for " + stack.getItem());
        GenContext context = GenContext.forPlayer(RandomSource.create(seed ^ 0x544E4B4C4F4F54L), player);
        stack.set(Apoth.Components.AFFIXES, ItemAffixes.EMPTY);
        AffixHelper.setRarity(stack, rarity);
        for (LootRule rule : rarity.getRules(category)) {
            // Some item categories have no effect/ability affixes. Skip those optional rules.
            if (rule instanceof LootRule.AffixLootRule affix
                && LootController.getWeightedAffixes(stack, rarity, affix.type(), context).stream()
                    .noneMatch(a -> a.getWeight().asInt() > 0)) continue;
            rule.execute(stack, rarity, context);
        }
        ItemAffixes affixes = stack.getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY);
        if (affixes.size() == 0)
            throw new IllegalStateException("No eligible affixes for " + stack.getItem() + " at rarity " + wanted);
        var names = new ArrayList<>(affixes.keySet());
        // Keep the display name tied to actual affixes and the configured rarity.
        String key = names.size() > 1 ? "misc.apotheosis.affix_name.three" : "misc.apotheosis.affix_name.two";
        AffixHelper.setName(stack, Component.translatable(key, names.getFirst().get().getName(true), "",
            names.size() > 1 ? names.getLast().get().getName(false) : "")
            .withStyle(Style.EMPTY.withColor(rarity.color()).withItalic(false)));
        stack.remove(Apoth.Components.TOUCHED_BY_MALICE);
        return stack;
    }
}
