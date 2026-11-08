/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.CustomData
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.storage.loot.LootContext
 *  net.minecraft.world.level.storage.loot.parameters.LootContextParams
 *  net.neoforged.fml.ModList
 *  net.neoforged.neoforge.event.entity.living.LivingDropsEvent
 */
package dev.mikaaah.tnkloot.compat;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.compat.CompatPresets;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.loot.WeightedPool;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class BossRewards {
    private static volatile List<Pool> pools = BossRewards.parse(CompatPresets.bundled("default-boss-rewards.json"));
    private static volatile java.util.Set<String> exclusiveItems = exclusiveIds(pools);
    private static boolean enabled = true;
    private static boolean playerOnly = true;

    private BossRewards() {
    }

    public static Path path() {
        return TnkConfig.path().resolveSibling("boss-rewards.json");
    }

    public static List<Pool> all() {
        return pools;
    }

    public static List<Pool> active() {
        return pools.stream().filter(p -> enabled && p.enabled() && ModList.get().isLoaded(p.mod()) && BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(p.entity()))).toList();
    }

    public static Optional<Pool> find(String id) {
        return BossRewards.active().stream().filter(p -> p.id().equals(id)).findFirst();
    }

    public static boolean exclusive(String item) {
        return exclusiveItems.contains(item);
    }

    private static java.util.Set<String> exclusiveIds(List<Pool> definitions) {
        var ids = new java.util.HashSet<String>(); ids.add("mowziesmobs:earthrend_gauntlet");
        for (var pool : definitions) for (var reward : pool.items()) ids.add(reward.item());
        return java.util.Set.copyOf(ids);
    }

    public static boolean reload() {
        try {
            if (!Files.exists(BossRewards.path(), new LinkOption[0])) {
                Files.createDirectories(BossRewards.path().getParent(), new FileAttribute[0]);
                Files.writeString(BossRewards.path(), (CharSequence)new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)CompatPresets.bundled("default-boss-rewards.json")), new OpenOption[0]);
            }
            JsonObject root = JsonParser.parseString((String)Files.readString(BossRewards.path())).getAsJsonObject();
            List<Pool> candidate = BossRewards.parse(root);
            pools = candidate;
            exclusiveItems = exclusiveIds(candidate);
            enabled = root.get("enabled").getAsBoolean();
            playerOnly = root.get("require_player_kill").getAsBoolean();
            return true;
        }
        catch (Exception ex) {
            TnkLoot.LOG.error("Invalid boss rewards; keeping last working definitions", (Throwable)ex);
            return false;
        }
    }

    public static List<Pool> parse(JsonObject root) { return dev.mikaaah.tnkloot.boss.BossStore.parse(root); }
    private static boolean registered(Reward w) {
        return PackItemPolicy.allowed(w.item()) && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse((String)w.item()));
    }

    public static List<ItemStack> options(Pool pool, TnkConfig.Category category) {
        return pool.items().stream().filter(w -> BossRewards.registered(w)).map(w -> new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(ResourceLocation.parse((String)w.item())))).toList();
    }

    public static ItemStack token(Pool pool, TnkConfig.Category category, Level level, ItemStack ignored) {
        ItemStack stack = new ItemStack(dev.mikaaah.tnkloot.boss.BossItems.DROP.get());
        CompoundTag tag = UnidentifiedWeaponItem.data(UnidentifiedWeaponItem.create(TnkConfig.Category.WEAPON,pool.tier(),"","boss_reward"));
        tag.putString("boss_pool",pool.id()); tag.putString("boss_name",pool.name());tag.putBoolean("boss_universal",true);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
        dev.mikaaah.tnkloot.dimension.DimensionLoot.bindOrigin(stack,level);
        return dev.mikaaah.tnkloot.rarity.RaritySupport.ensure(stack);
    }
    public static ItemStack baseResult(Level level, ItemStack token) {
        Item item;
        CompoundTag tag = UnidentifiedWeaponItem.data(token);
        Optional<Pool> pool = BossRewards.find(tag.getString("boss_pool"));
        if (pool.isEmpty() || !((item = token.getItem()) instanceof UnidentifiedWeaponItem)) {
            return ItemStack.EMPTY;
        }
        UnidentifiedWeaponItem item2 = (UnidentifiedWeaponItem)item;
        if (tag.contains("boss_result", 10)) {
            ItemStack result = ItemStack.parseOptional((HolderLookup.Provider)level.registryAccess(), (CompoundTag)tag.getCompound("boss_result"));
            String id = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
            return PackItemPolicy.allowed(id) && pool.get().items().stream().anyMatch(w -> w.item().equals(id) && (tag.getBoolean("boss_universal") || w.category() == item2.category())) ? result.copyWithCount(1) : ItemStack.EMPTY;
        }
        List<WeightedPool.Entry<Item>> entries = pool.get().items().stream().filter(w -> (tag.getBoolean("boss_universal") || w.category() == item2.category()) && BossRewards.registered(w)).map(w -> new WeightedPool.Entry<Item>((Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse((String)w.item())), w.weight())).toList();
        return new WeightedPool<Item>(entries).roll(() -> ((RandomSource)RandomSource.create((long)tag.getLong("roll_seed"))).nextLong()).map(ItemStack::new).map(base -> NativeLootTemplates.apply(pool.get().entity(), base)).orElse(ItemStack.EMPTY);
    }

    public static Optional<Pool> source(LootContext context) {
        Entity entity = (Entity)context.getParamOrNull(LootContextParams.THIS_ENTITY);
        String entityId = entity == null ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        ResourceLocation table = context.getQueriedLootTableId();
        Optional<Pool> exact = BossRewards.source(entityId);
        if (exact.isPresent()) {
            return exact;
        }
        return BossRewards.active().stream().sorted(Comparator.comparingInt((Pool p) -> p.entity().length()).reversed()).filter(p -> {
            String base = "entities/" + p.entity().substring(p.entity().indexOf(58) + 1);
            return table != null && table.getNamespace().equals(p.mod()) && (table.getPath().equals(base) || table.getPath().startsWith(base + "_"));
        }).findFirst();
    }

    public static Optional<Pool> source(String entity) {
        return BossRewards.active().stream().filter(p -> p.entity().equals(entity)).findFirst();
    }

    // Delay sealing until LivingDropsEvent so multiple equipment drops become one reward.
    public static ItemStack wrap(Pool pool, ItemStack original, Level level) { return ItemStack.EMPTY; }
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !TnkConfig.get().enableIdentification() || !TnkConfig.get().replaceWeaponLoot()) return;
        Optional<Pool> found=source(BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString());
        if(found.isEmpty()) return; Pool pool=found.get();
        if(playerOnly && !event.isRecentlyHit() && !(event.getSource().getEntity() instanceof ServerPlayer)) return;
        if(event.getDrops().stream().anyMatch(d->d.getItem().getItem() instanceof UnidentifiedWeaponItem&&UnidentifiedWeaponItem.data(d.getItem()).getString("boss_pool").equals(pool.id()))) return;
        if(pool.items().stream().noneMatch(BossRewards::registered)) return;
        boolean equipment=event.getDrops().stream().anyMatch(d->!UnidentifiedWeaponItem.data(d.getItem()).contains("tnk_rarity")&&!dev.mikaaah.tnkloot.injection.LootProtection.keepNative(d.getItem())&&pool.items().stream().anyMatch(w->registered(w)&&w.item().equals(BuiltInRegistries.ITEM.getKey(d.getItem().getItem()).toString())));
        if(!equipment && (!pool.additional() || level.getRandom().nextDouble()>=pool.chance()))return;
        event.getDrops().removeIf(d->!UnidentifiedWeaponItem.data(d.getItem()).contains("tnk_rarity")&&!dev.mikaaah.tnkloot.injection.LootProtection.keepNative(d.getItem())&&pool.items().stream().anyMatch(w->registered(w)&&w.item().equals(BuiltInRegistries.ITEM.getKey(d.getItem().getItem()).toString())));
        var e=event.getEntity();
        event.getDrops().add(new ItemEntity(level,e.getX(),e.getY()+0.5,e.getZ(),token(pool,TnkConfig.Category.WEAPON,level,ItemStack.EMPTY)));
    }

    public record Reward(String item, TnkConfig.Category category, int weight) {
    }

    public record Pool(String id, String mod, String entity, String name, int tier, boolean enabled, double chance, boolean additional, List<Reward> items) {
    }
}
