package dev.mikaaah.tnkloot.dungeon;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.mikaaah.tnkloot.compat.PackItemPolicy;
import dev.mikaaah.tnkloot.compat.PresetCleanup;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.rarity.*;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.*;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.tiers.*;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Dungeon gear is generated once. Identification reveals the complete saved stack. */
public final class DungeonEquipment {
    public static final String AUTHORED = "tnk_dungeon_reward", RESULT = "tnk_dungeon_result", HINT = "tnk_dungeon_hint", PLAN = "tnk_dungeon_plan", PREVIEW = "tnk_dungeon_preview";
    private DungeonEquipment() {}
    public static boolean authored(ItemStack stack) { return UnidentifiedWeaponItem.data(stack).getBoolean(AUTHORED); }
    public static boolean sealed(ItemStack stack) { return stack.getItem() instanceof UnidentifiedWeaponItem && UnidentifiedWeaponItem.data(stack).contains(RESULT, 10); }
    public static boolean preview(ItemStack stack) { return UnidentifiedWeaponItem.data(stack).getBoolean(PREVIEW); }
    public static TnkConfig.Category categoryName(String name) {
        try { return TnkConfig.Category.valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("Unknown equipment category " + name); }
    }
    public static TnkConfig.Category category(ItemStack stack, Optional<String> explicit) { return explicit.map(DungeonEquipment::categoryName).orElseGet(() -> FinalLoot.category(stack)); }
    public static void requireNativePayload(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!PresetCleanup.requiresNativePayload(id)) return;
        try {
            Class<?> data = Class.forName("io.redspace.ironsjewelry.core.data.JewelryData");
            Object payload = data.getMethod("get", ItemStack.class).invoke(null, stack);
            DungeonData.require((boolean)data.getMethod("isValid").invoke(payload), "Jewellery reward " + id + " needs valid native pattern/material components");
        } catch (ReflectiveOperationException error) { throw new IllegalArgumentException("Cannot validate native jewellery " + id, error); }
    }
    public static ItemStack reward(DungeonData.Entry entry, ServerLevel level, Random random) {
        ItemStack input = entry.template();
        var category = category(input, entry.category());
        int count = entry.minCount() + random.nextInt(entry.maxCount() - entry.minCount() + 1);
        long seed = random.nextLong();
        JsonObject preset = entry.preset() == null ? new JsonObject() : entry.preset().json();
        if (category == null) {
            DungeonData.require(!preset.has("fixed_affixes") && DungeonData.integer(preset, "random_affixes", 0, 0, 12) == 0, "Affix preset requires an equipment category");
            nativePreset(input, preset, entry.preset() == null ? entry.id() : entry.preset().id(), level, new Random(seed), false, new ArrayList<>());
            input.setCount(count); return input;
        }
        DungeonData.require(count == 1, "Equipment rewards must have count 1");
        String rarity = entry.settings().rarities().roll(new Random(seed ^ 0x524152495459L));
        ItemStack result = prepare(input, entry.settings(), rarity, entry.preset(), entry.id(), level, seed);
        return entry.settings().identified() ? result : wrap(result, preview(entry, level, List.of()), category, entry.settings(), rarity, entry.id(), level, seed);
    }
    public static ItemStack prepare(ItemStack template, DungeonData.Settings settings, String rarity, DungeonData.Preset preset,
                                    ResourceLocation source, ServerLevel level, long seed) {
        ItemStack result = template.copyWithCount(1);
        requireNativePayload(result);
        JsonObject raw = preset == null ? new JsonObject() : preset.json();
        var quality = RarityPolicy.get().quality(rarity);
        var nativeAffixes = result.getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY);
        JsonObject plan = new JsonObject();
        plan.addProperty("standard", preset == null && nativeAffixes.size() == 0);
        plan.addProperty("named", raw.has("name") || result.has(DataComponents.CUSTOM_NAME) || result.has(Apoth.Components.AFFIX_NAME));
        plan.addProperty("count", DungeonData.integer(raw, "random_affixes", 0, 0, 12));
        plan.add("pool", raw.has("affix_pool") ? raw.getAsJsonArray("affix_pool").deepCopy() : new JsonArray());
        JsonObject fixed = new JsonObject();
        for (var holder : nativeAffixes.keySet()) fixed.addProperty(holder.getId().toString(), nativeAffixes.getLevel(holder));
        if (raw.has("fixed_affixes")) raw.getAsJsonObject("fixed_affixes").entrySet().forEach(e -> fixed.addProperty(e.getKey(), DungeonData.range(e.getValue(), 0, 1).roll(new Random(seed ^ e.getKey().hashCode()))));
        plan.add("fixed", fixed);
        mark(result, source, settings, seed);
        var tag = UnidentifiedWeaponItem.data(result); tag.putString(PLAN, plan.toString()); result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        RaritySupport.stamp(result, quality, settings.tier());
        var rarityValue = RarityRegistry.INSTANCE.getValue(ResourceLocation.parse(quality.apotheosis()));
        DungeonData.require(rarityValue != null, "Missing equipment rarity " + quality.apotheosis());
        var enchantments = result.get(DataComponents.ENCHANTMENTS);
        var storedEnchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
        var name = result.get(DataComponents.CUSTOM_NAME);
        var lore = result.get(DataComponents.LORE);
        var context = GenContext.standalone(RandomSource.create(seed), worldTier(settings.tier()), 0, level, net.minecraft.core.BlockPos.ZERO);
        rollAffixes(result, rarityValue, context, seed, false);
        if (enchantments != null && !enchantments.isEmpty()) result.set(DataComponents.ENCHANTMENTS, enchantments);
        if (storedEnchantments != null && !storedEnchantments.isEmpty()) result.set(DataComponents.STORED_ENCHANTMENTS, storedEnchantments);
        if (name != null) result.set(DataComponents.CUSTOM_NAME, name);
        if (lore != null) result.set(DataComponents.LORE, lore);
        nativePreset(result, raw, preset == null ? source : preset.id(), level, new Random(seed ^ 0x505245534554L), false, new ArrayList<>());
        return result;
    }
    private static WorldTier worldTier(int tier) { return tier >= 7 ? WorldTier.PINNACLE : tier >= 5 ? WorldTier.SUMMIT : tier >= 3 ? WorldTier.ASCENT : tier >= 1 ? WorldTier.FRONTIER : WorldTier.HAVEN; }
    private static void mark(ItemStack stack, ResourceLocation source, DungeonData.Settings settings, long seed) {
        var tag = UnidentifiedWeaponItem.data(stack);
        tag.putBoolean(AUTHORED, true); tag.putBoolean("tnk_progression_sealed", true);
        tag.putString("tnk_dungeon_source", source.toString()); tag.putInt("tnk_source_level", settings.sourceLevel());
        tag.putInt("tnk_tier", settings.tier()); tag.putLong("roll_seed", seed);
        tag.remove("tnk_external_generated"); tag.remove("tnk_random_pool");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    public static ItemStack wrap(ItemStack result, ItemStack hint, TnkConfig.Category category, DungeonData.Settings settings,
                                 String rarity, ResourceLocation source, ServerLevel level, long seed) {
        ItemStack token = UnidentifiedWeaponItem.create(category, settings.tier(), BuiltInRegistries.ITEM.getKey(result.getItem()).toString(), "dungeon_reward");
        var tag = UnidentifiedWeaponItem.data(token);
        tag.putLong("roll_seed", seed); tag.putInt("tnk_source_level", settings.sourceLevel()); tag.putBoolean("tnk_progression_sealed", true);
        tag.putBoolean(AUTHORED, true); tag.putString("tnk_dungeon_source", source.toString());
        tag.putString("origin_dimension", level.dimension().location().toString());
        tag.put(RESULT, result.save(level.registryAccess())); tag.put(HINT, hint.save(level.registryAccess()));
        token.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); RaritySupport.force(token, rarity);
        return token;
    }
    public static ItemStack result(net.minecraft.world.level.Level level, ItemStack token) {
        var payload = UnidentifiedWeaponItem.data(token).getCompound(RESULT);
        ItemStack stack = ItemStack.parseOptional(level.registryAccess(), payload);
        return stack.isEmpty() || !PackItemPolicy.allowed(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) ? ItemStack.EMPTY : stack;
    }
    public static ItemStack hint(net.minecraft.world.level.Level level, ItemStack token) {
        ItemStack hint = ItemStack.parseOptional(level.registryAccess(), UnidentifiedWeaponItem.data(token).getCompound(HINT));
        return hint.isEmpty() ? result(level, token) : hint;
    }
    public static ItemStack preview(DungeonData.Entry entry, ServerLevel level, List<? extends Component> notes) {
        ItemStack icon = entry.template(); List<Component> lore = new ArrayList<>();
        var existing = icon.get(DataComponents.LORE); if (existing != null) lore.addAll(existing.lines());
        if (entry.preset() != null) nativePreset(icon, entry.preset().json(), entry.preset().id(), level, new Random(0), true, lore);
        if (category(icon, entry.category()) != null) {
            var rarities = entry.settings().rarities().weights().entrySet().stream().filter(e -> e.getValue() > 0).toList();
            if (rarities.size() == 1) {
                var q = RarityPolicy.get().quality(rarities.getFirst().getKey());
                RaritySupport.stamp(icon, q, entry.settings().tier());
                var rarity = RarityRegistry.INSTANCE.getValue(ResourceLocation.parse(q.apotheosis()));
                if (rarity != null) AffixHelper.setRarity(icon, rarity);
                if (entry.preset() != null && entry.preset().json().has("fixed_affixes")) {
                    var fixed = ItemAffixes.EMPTY.toBuilder();
                    entry.preset().json().getAsJsonObject("fixed_affixes").entrySet().forEach(e -> {
                        var range = DungeonData.range(e.getValue(), 0, 1); if (range.min() == range.max()) fixed.put(affix(e.getKey()), (float)range.min());
                    });
                    icon.set(Apoth.Components.AFFIXES, fixed.build());
                }
            }
            lore.add(Component.literal("Tier " + entry.settings().tier() + " · level " + entry.settings().sourceLevel() + (entry.settings().identified() ? " · identified" : " · unidentified")));
            if(rarities.size()>1)lore.add(Component.literal("Rarity odds: " + entry.settings().rarities().description()));
            var raw = entry.preset() == null ? null : entry.preset().json();
            if (raw == null) lore.add(Component.literal("Random affixes · floor tier"));
            else {
                int count = DungeonData.integer(raw, "random_affixes", 0, 0, 12);
                if (count > 0) lore.add(Component.literal(count + " random affix" + (count == 1 ? "" : "es") + " · floor tier"));
                if (raw.has("fixed_affixes")) raw.getAsJsonObject("fixed_affixes").entrySet().forEach(e -> lore.add(affix(e.getKey()).get().getName(false).copy().append(" · guaranteed affix")));
            }
        }
        if (entry.minCount() != 1 || entry.maxCount() != 1) lore.add(Component.literal("Quantity: " + entry.minCount() + (entry.minCount() == entry.maxCount() ? "" : "–" + entry.maxCount())));
        lore.addAll(notes);
        icon.set(DataComponents.LORE, new ItemLore(lore.stream().map(line -> (Component)line.copy().withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withItalic(false))).toList()));
        var tag = UnidentifiedWeaponItem.data(icon); tag.putBoolean(PREVIEW, true); tag.putString("tnk_dungeon_source", entry.id().toString());
        icon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); return icon;
    }
    private static void nativePreset(ItemStack stack, JsonObject preset, ResourceLocation id, ServerLevel level, Random random, boolean preview, List<Component> notes) {
        var modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (modifiers.modifiers().isEmpty()) modifiers = stack.getItem().getDefaultAttributeModifiers();
        if (preset.has("attributes")) {
            int index = 0;
            for (var element : preset.getAsJsonArray("attributes")) {
                var spec = element.getAsJsonObject(); var range = DungeonData.range(spec.get("amount"), -1_000_000, 1_000_000);
                var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(spec.get("attribute").getAsString())).orElseThrow();
                var operation = AttributeModifier.Operation.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(DungeonData.string(spec, "operation", "add_value"))).getOrThrow();
                var slot = spec.has("slot") ? EquipmentSlotGroup.CODEC.parse(JsonOps.INSTANCE, spec.get("slot")).getOrThrow() : slot(stack);
                var modifierId = spec.has("id") ? ResourceLocation.parse(spec.get("id").getAsString()) : ResourceLocation.fromNamespaceAndPath("tnkloot", "dungeon/" + id.getNamespace() + "/" + id.getPath() + "/" + index);
                index++;
                if (preview) {
                    var shown = operation == AttributeModifier.Operation.ADD_VALUE ? range : new DungeonData.Range(range.min() * 100, range.max() * 100);
                    notes.add(Component.literal("Bonus: " + shown.description() + (operation == AttributeModifier.Operation.ADD_VALUE ? " " : "% ")).append(Component.translatable(attribute.value().getDescriptionId())));
                } else modifiers = modifiers.withModifierAdded(attribute, new AttributeModifier(modifierId, range.roll(random), operation), slot);
            }
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
        }
        if (preset.has("enchantments")) EnchantmentHelper.updateEnchantments(stack, mutable -> preset.getAsJsonObject("enchantments").entrySet().forEach(e ->
            mutable.set(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(e.getKey()))), e.getValue().getAsInt())));
        if (preset.has("name")) stack.set(DataComponents.CUSTOM_NAME, Component.literal(preset.get("name").getAsString()));
    }
    private static EquipmentSlotGroup slot(ItemStack stack) {
        if (stack.getItem() instanceof Equipable item) return switch (item.getEquipmentSlot()) {
            case HEAD -> EquipmentSlotGroup.HEAD; case CHEST -> EquipmentSlotGroup.CHEST; case LEGS -> EquipmentSlotGroup.LEGS; case FEET -> EquipmentSlotGroup.FEET;
            case OFFHAND -> EquipmentSlotGroup.OFFHAND; default -> EquipmentSlotGroup.MAINHAND;
        };
        return EquipmentSlotGroup.MAINHAND;
    }
    private static JsonObject plan(ItemStack stack) {
        String json = UnidentifiedWeaponItem.data(stack).getString(PLAN);
        return json.isBlank() ? new JsonObject() : JsonParser.parseString(json).getAsJsonObject();
    }
    private static DynamicHolder<Affix> affix(String id) {
        var holder = AffixRegistry.INSTANCE.holder(ResourceLocation.parse(id));
        DungeonData.require(holder.isBound(), "Unknown dungeon affix " + id); return holder;
    }
    private static ItemAffixes fixed(JsonObject plan) {
        var builder = ItemAffixes.EMPTY.toBuilder();
        if (plan.has("fixed")) plan.getAsJsonObject("fixed").entrySet().forEach(e -> builder.put(affix(e.getKey()), e.getValue().getAsFloat()));
        return builder.build();
    }
    private static void rollAffixes(ItemStack stack, LootRarity rarity, GenContext context, long seed, boolean reforging) {
        JsonObject plan = plan(stack); var fixed = fixed(plan);
        stack.set(Apoth.Components.AFFIXES, fixed); AffixHelper.setRarity(stack, rarity);
        var category = LootCategory.forItem(stack);
        if (!reforging) for (var key : fixed.keySet()) DungeonData.require(key.get().canApplyTo(stack, category, rarity), "Fixed affix " + key.getId() + " cannot apply to this reward");
        if (!category.isNone()) {
            if (plan.has("standard") && plan.get("standard").getAsBoolean()) {
                for (LootRule rule : rarity.getRules(category)) {
                    if (reforging && !(rule instanceof LootRule.AffixLootRule)) continue;
                    if (!(rule instanceof LootRule.AffixLootRule r) || !LootController.getWeightedAffixes(stack, rarity, r.type(), context).isEmpty()) rule.execute(stack, rarity, context);
                }
            } else {
                int count = DungeonData.integer(plan, "count", 0, 0, 12); Set<String> pool = new LinkedHashSet<>();
                if (plan.has("pool")) plan.getAsJsonArray("pool").forEach(v -> pool.add(v.getAsString()));
                for (int i = 0; i < count; i++) {
                    var current = stack.getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY);
                    Collection<DynamicHolder<Affix>> source = pool.isEmpty() ? new LinkedHashSet<>(AffixRegistry.INSTANCE.getTypeMap().values()) : pool.stream().map(DungeonEquipment::affix).toList();
                    Map<DynamicHolder<Affix>, Integer> choices = new LinkedHashMap<>();
                    source.stream().filter(DynamicHolder::isBound).filter(h -> pool.isEmpty() ? h.getId().getNamespace().equals("tnkloot") : true)
                        .filter(h -> !current.keySet().contains(h) && h.get().canApplyTo(stack, category, rarity) && h.get().isCompatibleWith(current))
                        .forEach(h -> { int weight = h.get().weights().getWeight(context.tier(), context.luck()); if (!pool.isEmpty()) weight = Math.max(1, weight); if (weight > 0) choices.put(h, weight); });
                    DungeonData.require(!choices.isEmpty(), "Dungeon affix pool cannot supply " + count + " compatible random affixes");
                    var chosen = DungeonData.weighted(choices, new Random(context.rand().nextLong()));
                    stack.set(Apoth.Components.AFFIXES, current.toBuilder().put(chosen, 0).build());
                }
            }
        } else DungeonData.require(fixed.size() == 0 && DungeonData.integer(plan, "count", 0, 0, 12) == 0, "Item has no Apotheosis equipment category");
        var generated = stack.getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY); var builder = generated.toBuilder();
        var tag = UnidentifiedWeaponItem.data(stack); var q = RarityPolicy.get().quality(tag.getString("tnk_rarity")); Random random = new Random(seed ^ 0x41464649584cL);
        for (var holder : generated.keySet()) if (!fixed.keySet().contains(holder)) builder.put(holder, RarityPolicy.get().level(tag.getInt("tnk_tier"), q, random));
        stack.set(Apoth.Components.AFFIXES, builder.build());
        if (!plan.has("named") || !plan.get("named").getAsBoolean()) {
            var names = new ArrayList<>(builder.keySet());
            if (names.isEmpty()) stack.remove(Apoth.Components.AFFIX_NAME);
            else AffixHelper.setName(stack, Component.translatable(names.size() > 1 ? "misc.apotheosis.affix_name.three" : "misc.apotheosis.affix_name.two",
                names.getFirst().get().getName(true), "", names.size() > 1 ? names.getLast().get().getName(false) : "").withStyle(style -> style.withColor(rarity.color()).withItalic(false)));
        }
    }
    /** Reforging changes only the random portion; the plan is saved on the item, not looked up again. */
    public static ItemStack reforge(ItemStack original, LootRarity rarity, GenContext context) {
        ItemStack stack = original.copy(); var tag = UnidentifiedWeaponItem.data(stack);
        // Fixed affix values depend on rarity as well as their saved normalized level.
        // Keep the original rarity even when another caller requests a different one.
        var saved = AffixHelper.getRarity(original);
        DungeonData.require(saved.isBound(), "Dungeon reward lost its saved rarity");
        rarity = saved.get();
        RaritySupport.stamp(stack, dev.mikaaah.tnkloot.apotheosis.UnifiedApotheosis.quality(rarity), tag.getInt("tnk_tier"));
        rollAffixes(stack, rarity, context, context.rand().nextLong(), true); return stack;
    }
    public static ItemAffixes bounded(ItemStack stack, ItemAffixes incoming) {
        JsonObject plan = plan(stack); ItemAffixes fixed = fixed(plan); var builder = incoming.toBuilder();
        Set<String> pool = new HashSet<>(); if (plan.has("pool")) plan.getAsJsonArray("pool").forEach(v -> pool.add(v.getAsString()));
        float maximum = (float)RarityPolicy.get().bands().get(Math.clamp(UnidentifiedWeaponItem.data(stack).getInt("tnk_tier"), 0, 7)).max();
        for (var key : incoming.keySet()) {
            if (fixed.keySet().contains(key)) builder.put(key, fixed.getLevel(key));
            else if ((!pool.isEmpty() && !pool.contains(key.getId().toString())) || (pool.isEmpty() && !key.getId().getNamespace().equals("tnkloot"))) builder.remove(key);
            else builder.put(key, Math.min(maximum, incoming.getLevel(key)));
        }
        for (var key : fixed.keySet()) builder.put(key, fixed.getLevel(key));
        return builder.build();
    }
    public static boolean allowedAffix(ItemStack stack, DynamicHolder<Affix> holder) {
        if (holder.getId().getNamespace().equals("tnkloot")) return true;
        JsonObject plan = plan(stack);
        if (plan.has("fixed") && plan.getAsJsonObject("fixed").has(holder.getId().toString())) return true;
        return plan.has("pool") && plan.getAsJsonArray("pool").asList().stream().anyMatch(e -> e.getAsString().equals(holder.getId().toString()));
    }
}
