package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.config.TnkConfig.Category;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import java.util.List;

/** Category-specific token with immutable rank and roll seed stored in 1.21.1 data components. */
public final class UnidentifiedWeaponItem extends Item {
    private final Category category;
    public UnidentifiedWeaponItem(Item.Properties properties, Category category) { super(properties); this.category = category; }
    public Category category() { return category; }
    public static ItemStack create(int rank) { return create(Category.WEAPON, rank); }
    public static ItemStack create(Category category, int rank) {
        return create(category, rank, "", "manual");
    }
    public static ItemStack create(Category category, int rank, String sourceItem, String sourceType) {
        ItemStack stack = new ItemStack(TnkLoot.token(category).get());
        CompoundTag tag = new CompoundTag();
        int tier = TnkConfig.get().tierForRank(rank).rank();
        tag.putInt("tier", tier);
        tag.putString("tier_name", TnkConfig.get().tierForRank(tier).name());
        tag.putLong("roll_seed", java.util.concurrent.ThreadLocalRandom.current().nextLong());
        if (!sourceItem.isBlank()) tag.putString("source_item", sourceItem);
        tag.putString("source_type", sourceType);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
    public static CompoundTag data(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
    public static int tier(ItemStack stack) { return Math.max(0, data(stack).getInt("tier")); }

    /** Tokens can only be identified through the confirmed station transaction. */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    /** Preview the base item deterministically; never mutate the token or player. */
    public static ItemStack baseResult(Level level, ItemStack token) {
        if (!(token.getItem() instanceof UnidentifiedWeaponItem unidentified)) return ItemStack.EMPTY;
        return WeaponSelector.roll(level, tier(token), unidentified.category(),
            RandomSource.create(data(token).getLong("roll_seed")));
    }

    public static int cost(ItemStack token, ItemStack result) {
        var costs = TnkConfig.get().costs();
        return costs.enabled() ? costs.amount(tier(token), BuiltInRegistries.ITEM.getKey(result.getItem()).toString()) : 0;
    }

    public static boolean has(ServerPlayer player, Item item, int amount) {
        int found = 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(item)) found += stack.getCount();
        for (ItemStack stack : player.getInventory().offhand) if (stack.is(item)) found += stack.getCount();
        return found >= amount;
    }
    public static void remove(ServerPlayer player, Item item, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            int taken = Math.min(amount, stack.is(item) ? stack.getCount() : 0);
            if (taken > 0) { stack.shrink(taken); amount -= taken; }
            if (amount == 0) return;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            int taken = Math.min(amount, stack.is(item) ? stack.getCount() : 0);
            if (taken > 0) { stack.shrink(taken); amount -= taken; }
            if (amount == 0) return;
        }
    }

    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int rank = tier(stack);
        CompoundTag payload = data(stack);
        String name = payload.contains("tier_name") ? payload.getString("tier_name") : TnkConfig.get().tierForRank(rank).name();
        tooltip.add(Component.translatable("tooltip.tnkloot.tier", rank, name).withStyle(ChatFormatting.GOLD));
        if (payload.contains("tnk_rarity")) tooltip.add(Component.literal("Rarity: " + payload.getString("tnk_rarity").replace("tnkloot:", "")).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tnkloot.identify_cost").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tnkloot.identify").withStyle(ChatFormatting.GRAY));
    }
}
