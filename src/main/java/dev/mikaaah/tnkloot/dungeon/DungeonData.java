package dev.mikaaah.tnkloot.dungeon;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.compat.PackItemPolicy;
import dev.mikaaah.tnkloot.loot.TnkLootRolls;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** One immutable, atomically reloaded catalog supplies both payouts and their previews. */
public final class DungeonData {
    public static final String TABLES = "tnkloot/dungeon_rewards", PRESETS = "tnkloot/equipment_presets";
    public record Range(double min, double max) {
        public double roll(Random random) { return min == max ? min : min + random.nextDouble() * (max - min); }
        public String description() { return min == max ? format(min) : format(min) + "–" + format(max); }
        private static String format(double value) { return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
    }
    public record Rarities(Map<String, Integer> weights) {
        public Rarities { weights = Collections.unmodifiableMap(new LinkedHashMap<>(weights)); }
        public String roll(Random random) { return weighted(weights, random); }
        public String description() {
            long total = weights.values().stream().mapToLong(Integer::longValue).sum();
            return String.join(", ", weights.entrySet().stream().filter(e -> e.getValue() > 0)
                .map(e -> e.getKey() + " " + percentage((double)e.getValue() / total)).toList());
        }
    }
    public record Settings(int tier, int sourceLevel, Rarities rarities, boolean identified) {}
    public record Preset(ResourceLocation id, JsonObject json) {
        public Preset { json = json.deepCopy(); }
        @Override public JsonObject json() { return json.deepCopy(); }
    }
    public record Entry(ResourceLocation id, ItemStack template, int weight, int minCount, int maxCount,
                        Settings settings, Preset preset, Optional<String> category) {
        public Entry { template = template.copy(); }
        @Override public ItemStack template() { return template.copy(); }
        public boolean available() { String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(template.getItem()).toString();return PackItemPolicy.allowed(id) && dev.mikaaah.tnkloot.compat.PackEquipmentRules.lootAllowed(id); }
    }
    public record Rare(Optional<Double> chance, boolean perPlayer, Optional<List<Entry>> entries, Settings settings) {
        public Rare { entries = entries.map(List::copyOf); }
        public double partyChance(double base, int players) { return players <= 0 ? 0 : perPlayer ? 1 - Math.pow(1 - base, players) : base; }
    }
    public record Table(ResourceLocation id, Settings settings, List<Entry> normal, Rare rare) {
        public Table { normal = List.copyOf(normal); }
        public List<Entry> availableNormal() { return normal.stream().filter(Entry::available).toList(); }
    }
    public record Catalog(Map<ResourceLocation, Preset> presets, Map<ResourceLocation, Table> tables) {
        public Catalog { presets = Map.copyOf(presets); tables = Map.copyOf(tables); }
    }
    private record Prepared(Map<ResourceLocation, JsonObject> presets, Map<ResourceLocation, JsonObject> tables) {}
    private static volatile Catalog current = new Catalog(Map.of(), Map.of());
    private DungeonData() {}
    public static Catalog catalog() { return current; }
    public static Optional<Table> table(ResourceLocation id) { return Optional.ofNullable(current.tables().get(id)); }
    public static Preset preset(ResourceLocation id) {
        var value = current.presets().get(id);
        if (value == null) throw new IllegalArgumentException("Unknown dungeon equipment preset " + id);
        return value;
    }
    public static void reloadListener(AddReloadListenerEvent event) {
        var registries = event.getRegistryAccess();
        event.addListener(new SimplePreparableReloadListener<Prepared>() {
            @Override protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
                return new Prepared(read(manager, PRESETS), read(manager, TABLES));
            }
            @Override protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
                Catalog next = parse(prepared.presets(), prepared.tables(), registries);
                current = next;
                TnkLoot.LOG.info("Loaded {} dungeon reward tables and {} equipment presets", next.tables().size(), next.presets().size());
            }
        });
    }
    private static Map<ResourceLocation, JsonObject> read(ResourceManager manager, String directory) {
        Map<ResourceLocation, JsonObject> result = new LinkedHashMap<>();
        for (var resource : manager.listResources(directory, id -> id.getPath().endsWith(".json")).entrySet()) {
            String path = resource.getKey().getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(resource.getKey().getNamespace(), path.substring(directory.length() + 1, path.length() - 5));
            try (var reader = resource.getValue().openAsReader()) {
                JsonObject root = object(JsonParser.parseReader(reader), id.toString());
                if (modsPresent(root)) result.put(id, root);
            } catch (IOException | RuntimeException error) {
                throw new IllegalArgumentException("Invalid dungeon data " + resource.getKey(), error);
            }
        }
        return result;
    }
    /** Also used by the verification server to exercise a monthly catalog replacement. */
    public static Catalog parse(Map<ResourceLocation, JsonObject> rawPresets, Map<ResourceLocation, JsonObject> rawTables, HolderLookup.Provider registries) {
        Map<ResourceLocation, Preset> presets = new LinkedHashMap<>();
        rawPresets.forEach((id, value) -> {
            validatePreset(value, registries);
            presets.put(id, new Preset(id, value));
        });
        Map<ResourceLocation, Table> tables = new LinkedHashMap<>();
        rawTables.forEach((id, root) -> {
            try {
                Settings settings = settings(root, new Settings(0, 1, rarities(new JsonPrimitive("common")), false));
                List<Entry> normal = entries(root.get("normal"), id, "normal", settings, presets, registries);
                require(!normal.isEmpty(), "Normal reward list is empty");
                JsonObject rare = root.has("rare") ? object(root.get("rare"), "rare") : new JsonObject();
                Optional<Double> chance = rare.has("chance") ? Optional.of(number(rare.get("chance"), 0, 1, "rare chance")) : Optional.empty();
                String scaling = string(rare, "party_scaling", "per_player");
                require(Set.of("per_player", "shared").contains(scaling), "party_scaling must be per_player or shared");
                Settings rareSettings = settings(rare, new Settings(settings.tier(), settings.sourceLevel(), settings.rarities(), true));
                require(rareSettings.identified(), "Rare prizes require identified delivery");
                Optional<List<Entry>> rareEntries = rare.has("entries")
                    ? Optional.of(entries(rare.get("entries"), id, "rare", rareSettings, presets, registries)) : Optional.empty();
                rareEntries.ifPresent(list -> list.forEach(entry -> require(entry.minCount() == 1 && entry.maxCount() == 1 && entry.settings().identified(), "Rare prizes must have count 1 and identified delivery")));
                require(normal.size() + rareEntries.map(List::size).orElse(0) <= 128, "A floor can preview at most 128 reward variants");
                tables.put(id, new Table(id, settings, normal, new Rare(chance, scaling.equals("per_player"), rareEntries, rareSettings)));
            } catch (RuntimeException error) { throw new IllegalArgumentException("Invalid dungeon reward table " + id + ": " + error.getMessage(), error); }
        });
        return new Catalog(presets, tables);
    }
    public static void replace(Catalog catalog) { current = Objects.requireNonNull(catalog); }

    private static List<Entry> entries(JsonElement value, ResourceLocation table, String section, Settings defaults,
                                       Map<ResourceLocation, Preset> presets, HolderLookup.Provider registries) {
        require(value != null && value.isJsonArray(), section + " must be an array");
        require(value.getAsJsonArray().size() <= 128, "At most 128 entries per reward list");
        List<Entry> result = new ArrayList<>(); Set<ResourceLocation> ids = new HashSet<>();
        int index = 0;
        for (var element : value.getAsJsonArray()) {
            JsonObject root = object(element, section + " entry");
            int entryIndex = index++;
            if (!modsPresent(root)) continue;
            String key = string(root, "id", Integer.toString(entryIndex));
            require(key.matches("[a-z0-9_./-]+"), "Entry id must be a simple path");
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(table.getNamespace(), table.getPath() + "/" + section + "/" + key);
            require(ids.add(id), "Duplicate reward id " + id);
            Preset preset = null;
            if (root.has("preset")) {
                ResourceLocation presetId = location(root.get("preset").getAsString());
                preset = presets.get(presetId); require(preset != null, "Missing preset " + presetId);
            }
            JsonObject stack;
            require(root.has("item") != root.has("stack"), "Reward needs exactly one of item or stack");
            if (root.has("stack")) stack = object(root.get("stack"), "stack").deepCopy();
            else { stack = new JsonObject(); stack.addProperty("id", root.get("item").getAsString()); }
            stack.addProperty("count", 1);
            JsonObject components = new JsonObject();
            if (preset != null && preset.json().has("components")) merge(components, object(preset.json().get("components"), "preset components"));
            if (stack.has("components")) merge(components, object(stack.get("components"), "stack components"));
            if (root.has("components")) merge(components, object(root.get("components"), "components"));
            if (!components.isEmpty()) stack.add("components", components);
            ItemStack template = ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), stack).getOrThrow();
            require(!template.isEmpty(), "Reward item is empty");
            if (root.has("name")) template.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(root.get("name").getAsString()));
            DungeonEquipment.requireNativePayload(template);
            int min = 1, max = 1;
            if (root.has("count")) {
                var count = root.get("count");
                if (count.isJsonObject()) { min = integer(count.getAsJsonObject(), "min", 1, 1, 4096); max = integer(count.getAsJsonObject(), "max", min, min, 4096); }
                else min = max = integer(count, 1, 4096, "count");
            }
            int weight = integer(root, "weight", 1, 1, 1_000_000);
            Settings settings = settings(root, defaults);
            Optional<String> category = root.has("category") ? Optional.of(root.get("category").getAsString()) : Optional.empty();
            category.ifPresent(DungeonEquipment::categoryName);
            require(template.getMaxStackSize() > 1 || max == 1, "Nonstackable rewards must use count 1; use more rolls instead");
            var entry = new Entry(id, template, weight, min, max, settings, preset, category);
            if (entry.available()) result.add(entry);
        }
        require(result.size() <= 128, "Too many reward variants");
        return List.copyOf(result);
    }
    private static void merge(JsonObject target, JsonObject source) { source.entrySet().forEach(e -> target.add(e.getKey(), e.getValue().deepCopy())); }
    public static Settings settings(JsonObject root, Settings fallback) {
        String delivery = string(root, "delivery", fallback.identified() ? "identified" : "unidentified");
        require(Set.of("identified", "unidentified").contains(delivery), "delivery must be identified or unidentified");
        require(!(root.has("rarity") && root.has("rarities")), "Use rarity or rarities, not both");
        Rarities rarities = root.has("rarity") ? rarities(root.get("rarity")) : root.has("rarities") ? rarities(root.get("rarities")) : fallback.rarities();
        return new Settings(integer(root, "tier", fallback.tier(), 0, 7), integer(root, "source_level", fallback.sourceLevel(), 1, 100), rarities, delivery.equals("identified"));
    }
    public static Rarities rarities(JsonElement value) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        if (value.isJsonPrimitive()) weights.put(rarity(value.getAsString()), 1);
        else {
            object(value, "rarities").entrySet().forEach(e -> weights.put(rarity(e.getKey()), integer(e.getValue(), 0, 1_000_000, "rarity weight")));
        }
        require(weights.values().stream().mapToLong(Integer::longValue).sum() > 0, "Rarity distribution is empty");
        return new Rarities(weights);
    }
    public static String rarity(String value) {
        String id = value.startsWith("tnkloot:") ? value.substring(8) : value;
        require(List.of(TnkLootRolls.RARITIES).contains(id), "Unknown rarity " + value); return id;
    }
    static void validatePreset(JsonObject root, HolderLookup.Provider registries) {
        if (root.has("components")) net.minecraft.core.component.DataComponentPatch.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), root.get("components")).getOrThrow();
        if (root.has("attributes")) for (var value : root.getAsJsonArray("attributes")) {
            var spec = object(value, "attribute");
            var attribute = location(spec.get("attribute").getAsString());
            require(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.containsKey(attribute), "Unknown attribute " + attribute);
            range(spec.get("amount"), -1_000_000, 1_000_000);
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(string(spec, "operation", "add_value"))).getOrThrow();
            if (spec.has("slot")) net.minecraft.world.entity.EquipmentSlotGroup.CODEC.parse(JsonOps.INSTANCE, spec.get("slot")).getOrThrow();
        }
        if (root.has("enchantments")) for (var e : root.getAsJsonObject("enchantments").entrySet()) {
            var id = location(e.getKey()); integer(e.getValue(), 1, 255, "enchantment level");
            registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, id));
        }
        if (root.has("fixed_affixes")) root.getAsJsonObject("fixed_affixes").entrySet().forEach(e -> {
            var id = location(e.getKey()); range(e.getValue(), 0, 1);
            require(dev.shadowsoffire.apotheosis.affix.AffixRegistry.INSTANCE.holder(id).isBound(), "Unknown fixed affix " + id);
        });
        integer(root, "random_affixes", 0, 0, 12);
        if (root.has("affix_pool")) root.getAsJsonArray("affix_pool").forEach(v -> {
            var id = location(v.getAsString()); require(dev.shadowsoffire.apotheosis.affix.AffixRegistry.INSTANCE.holder(id).isBound(), "Unknown random affix " + id);
        });
        if (root.has("name")) require(!root.get("name").getAsString().isBlank(), "Preset name is empty");
    }
    public static Range range(JsonElement value, double min, double max) {
        if (value.isJsonObject()) {
            double low = number(value.getAsJsonObject().get("min"), min, max, "range minimum");
            return new Range(low, number(value.getAsJsonObject().get("max"), low, max, "range maximum"));
        }
        double fixed = number(value, min, max, "value"); return new Range(fixed, fixed);
    }
    public static <T> T weighted(Map<T, Integer> weights, Random random) {
        long total = weights.values().stream().mapToLong(Integer::longValue).sum();
        require(total > 0, "Reward pool is empty"); long ticket = random.nextLong(total);
        for (var entry : weights.entrySet()) { ticket -= entry.getValue(); if (ticket < 0) return entry.getKey(); }
        throw new AssertionError();
    }
    public static Entry choose(List<Entry> entries, Random random) {
        Map<Entry, Integer> weights = new LinkedHashMap<>(); entries.stream().filter(Entry::available).forEach(e -> weights.put(e, e.weight()));
        return weighted(weights, random);
    }
    public static double probability(Entry entry, List<Entry> entries) { return (double)entry.weight() / entries.stream().mapToLong(Entry::weight).sum(); }
    public static String percentage(double chance) { return String.format(Locale.ROOT, "%.2f%%", chance * 100).replaceAll("0+%$", "%").replace(".%", "%"); }
    public static boolean modsPresent(JsonObject root) {
        if (!root.has("required_mods")) return true;
        for (var value : root.getAsJsonArray("required_mods")) if (!ModList.get().isLoaded(value.getAsString())) return false;
        return true;
    }
    public static ResourceLocation location(String value) { return ResourceLocation.parse(value); }
    public static String string(JsonObject root, String key, String fallback) { return root.has(key) ? root.get(key).getAsString() : fallback; }
    public static JsonObject object(JsonElement value, String name) { require(value != null && value.isJsonObject(), name + " must be an object"); return value.getAsJsonObject(); }
    public static int integer(JsonObject root, String key, int fallback, int min, int max) { return root.has(key) ? integer(root.get(key), min, max, key) : fallback; }
    public static int integer(JsonElement value, int min, int max, String name) {
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), name + " must be an integer");
        int number = value.getAsBigDecimal().intValueExact(); require(number >= min && number <= max, name + " out of range"); return number;
    }
    public static double number(JsonElement value, double min, double max, String name) {
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), name + " must be a number");
        double number = value.getAsDouble(); require(Double.isFinite(number) && number >= min && number <= max, name + " out of range"); return number;
    }
    public static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
