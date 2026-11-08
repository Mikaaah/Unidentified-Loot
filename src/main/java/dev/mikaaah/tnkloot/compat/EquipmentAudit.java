package dev.mikaaah.tnkloot.compat;

import com.google.gson.*;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.DimensionRules;
import dev.mikaaah.tnkloot.loot.LevelTierRoll;
import dev.mikaaah.tnkloot.rarity.RarityPolicy;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Read-only report of actual registered items and attributes, never guessed registry IDs. */
public final class EquipmentAudit {
    private EquipmentAudit() {}

    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tnkloot").then(Commands.literal("audit")
            .requires(source -> source.hasPermission(2)).executes(context -> {
                try {
                    Path path = write(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal("Loot audit saved: " + path), false);
                    return 1;
                } catch (Exception ex) {
                    TnkLoot.LOG.error("Could not export loot audit", ex);
                    context.getSource().sendFailure(Component.literal("Could not write loot audit; see server log"));
                    return 0;
                }
            })));
    }
    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("tnkloot.exportAudit")) return;
        try { TnkLoot.LOG.info("TNKLOOT_AUDIT_OK: {}", write(event.getServer())); }
        catch (Exception ex) { TnkLoot.LOG.error("TNKLOOT_AUDIT_FAILED", ex); }
    }

    public static Path write(MinecraftServer server) throws java.io.IOException {
        JsonObject report = new JsonObject();
        report.addProperty("minecraft", "1.21.1");
        report.addProperty("edition", "TNK Progression");
        JsonArray levels = new JsonArray();
        for (int level = 1; level <= 100; level++) {
            JsonObject row = new JsonObject(); row.addProperty("level", level);
            JsonObject distribution = new JsonObject();
            for (int tier = 0; tier <= 7; tier++) {
                int count = 0;
                for (int percentile = 0; percentile < 100; percentile++) if (LevelTierRoll.roll(level, percentile) == tier) count++;
                if (count > 0) distribution.addProperty(Integer.toString(tier), count);
            }
            row.add("tier_percentages", distribution); levels.add(row);
        }
        report.add("levels", levels);
        JsonArray items = new JsonArray();
        StringBuilder csv = new StringBuilder("item,group,category,min_tier,max_tier,level_range,dimension,boss_pools,registered,enabled,weight,attributes\n");
        Set<String> configured = new HashSet<>();
        for (var definition : TnkConfig.get().weapons()) {
            configured.add(definition.id());
            JsonObject row = item(definition.id(), definition.category(), definition.minTier(), definition.maxTier());
            row.addProperty("enabled", definition.enabled()); row.addProperty("weight", definition.weight());
            items.add(row); csv(csv, row);
        }
        for (var pool : BossRewards.all()) for (var reward : pool.items()) if (configured.add(reward.item())) {
            JsonObject row = item(reward.item(), reward.category(), pool.tier(), pool.tier());
            row.addProperty("enabled", pool.enabled()); row.addProperty("weight", reward.weight());
            items.add(row); csv(csv, row);
        }
        report.add("items", items);
        JsonArray attributes = new JsonArray();
        for (var attribute : BuiltInRegistries.ATTRIBUTE) {
            JsonObject row = new JsonObject();
            row.addProperty("id", BuiltInRegistries.ATTRIBUTE.getKey(attribute).toString());
            row.addProperty("default", attribute.getDefaultValue());
            if (attribute instanceof RangedAttribute range) {
                row.addProperty("min", range.getMinValue()); row.addProperty("max", range.getMaxValue());
            }
            attributes.add(row);
        }
        report.add("registered_attributes", attributes);
        JsonArray rarities = new JsonArray();
        for (var quality : RarityPolicy.get().qualities()) {
            JsonObject row = new JsonObject(); row.addProperty("id", quality.id());
            row.addProperty("normal_weight", quality.weight()); row.addProperty("boss_weight", quality.bossWeight());
            row.addProperty("cost_multiplier", quality.cost()); row.addProperty("roll_floor", quality.floor());
            JsonArray bands = new JsonArray();
            for (int tier = 0; tier < RarityPolicy.get().bands().size(); tier++) {
                var band = RarityPolicy.get().bands().get(tier);
                JsonObject value = new JsonObject(); value.addProperty("tier", tier);
                value.addProperty("min_affix_level", band.min() + (band.max() - band.min()) * quality.floor());
                value.addProperty("max_affix_level", band.max()); bands.add(value);
            }
            row.add("tier_affix_bands", bands); rarities.add(row);
        }
        report.add("rarities", rarities);
        JsonArray affixes = new JsonArray();
        for (var entry : server.getResourceManager().listResources("affixes", id -> id.getPath().endsWith(".json")).entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                JsonObject row = new JsonObject(); row.addProperty("resource", entry.getKey().toString());
                row.add("definition", JsonParser.parseReader(reader)); affixes.add(row);
            }
        }
        report.add("affixes", affixes);
        JsonArray bosses = new JsonArray();
        for (var pool : BossRewards.all()) {
            JsonObject row = new JsonObject(); row.addProperty("id", pool.id()); row.addProperty("entity", pool.entity());
            row.addProperty("entity_registered", BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(pool.entity())));
            row.addProperty("tier", pool.tier()); row.addProperty("enabled", BossRewards.active().contains(pool));
            JsonArray rewards = new JsonArray(); for (var reward : pool.items()) rewards.add(reward.item());
            row.add("items", rewards); bosses.add(row);
        }
        report.add("bosses", bosses);
        Path directory = TnkConfig.path().resolveSibling("reports"); Files.createDirectories(directory);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        atomic(directory.resolve("equipment-audit.json"), gson.toJson(report));
        atomic(directory.resolve("equipment-groups.csv"), csv.toString());
        return directory.toAbsolutePath();
    }

    private static JsonObject item(String id, TnkConfig.Category category, int minimum, int maximum) {
        JsonObject row = new JsonObject(); var key = ResourceLocation.parse(id);
        boolean present = BuiltInRegistries.ITEM.containsKey(key) && BuiltInRegistries.ITEM.get(key) != Items.AIR;
        var stack = present ? new ItemStack(BuiltInRegistries.ITEM.get(key)) : ItemStack.EMPTY;
        row.addProperty("item", id); row.addProperty("category", category.name().toLowerCase(Locale.ROOT));
        row.addProperty("group", present ? EquipmentGroups.subgroup(stack.getItem(), category) : "Unavailable");
        row.addProperty("registered", present); row.addProperty("min_tier", minimum); row.addProperty("max_tier", maximum);
        row.addProperty("level_range", EquipmentGroups.levels(minimum, maximum));
        row.addProperty("dimension", DimensionRules.infer(id));
        row.addProperty("boss_pools", String.join(";", BossRewards.all().stream().filter(p -> p.items().stream().anyMatch(w -> w.item().equals(id))).map(BossRewards.Pool::id).toList()));
        JsonArray attributes = new JsonArray();
        if (present) for (var modifier : stack.getAttributeModifiers().modifiers()) {
            JsonObject value = new JsonObject();
            value.addProperty("attribute", BuiltInRegistries.ATTRIBUTE.getKey(modifier.attribute().value()).toString());
            value.addProperty("amount", modifier.modifier().amount()); value.addProperty("operation", modifier.modifier().operation().getSerializedName());
            value.addProperty("slot", modifier.slot().getSerializedName()); attributes.add(value);
        }
        row.add("attributes", attributes); return row;
    }
    private static void csv(StringBuilder target, JsonObject row) {
        for (String key : List.of("item","group","category","min_tier","max_tier","level_range","dimension","boss_pools","registered","enabled","weight","attributes")) {
            if (!key.equals("item")) target.append(',');
            JsonElement value = row.get(key);
            String text = value.isJsonPrimitive() ? value.getAsString() : value.toString();
            target.append('"').append(text.replace("\"", "\"\"")).append('"');
        }
        target.append('\n');
    }
    private static void atomic(Path path, String text) throws java.io.IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, text, StandardCharsets.UTF_8);
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
