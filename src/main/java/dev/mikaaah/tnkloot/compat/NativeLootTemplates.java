package dev.mikaaah.tnkloot.compat;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.mikaaah.tnkloot.TnkLoot;
import java.util.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/** Read fixed native reward components from the installed mods, including locked spell slots. */
public final class NativeLootTemplates {
    private static volatile Map<String, ItemStack> templates = Map.of();
    private NativeLootTemplates() {}
    public static void load(MinecraftServer server) {
        Map<String, ItemStack> found = new HashMap<>();
        for (var resource : server.getResourceManager().listResources("loot_modifiers", id -> id.getPath().endsWith(".json")).entrySet()) {
            try (var reader = resource.getValue().openAsReader()) {
                var root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("item") || !root.get("item").isJsonObject()) continue;
                var item = root.getAsJsonObject("item");
                if (!item.has("components") || item.getAsJsonObject("components").isEmpty()) continue;
                String entity = "";
                if (root.has("conditions")) for (var value : root.getAsJsonArray("conditions")) {
                    var condition = value.getAsJsonObject();
                    if (condition.has("predicate") && condition.getAsJsonObject("predicate").has("type") && condition.getAsJsonObject("predicate").get("type").isJsonPrimitive()) entity = condition.getAsJsonObject("predicate").get("type").getAsString();
                }
                var result = ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), item).result();
                if (result.isPresent()) {
                    var stack = result.get();
                    String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    found.putIfAbsent(entity + "|" + id, stack.copyWithCount(1));
                }
            } catch (Exception ex) { TnkLoot.LOG.debug("Native reward template skipped: {}", resource.getKey(), ex); }
        }
        templates = Map.copyOf(found);
        TnkLoot.LOG.info("Loaded {} fixed native reward templates", templates.size());
    }
    public static ItemStack apply(String entity, ItemStack base) {
        if (base.isEmpty()) return base;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(base.getItem()).toString();
        var template = templates.getOrDefault(entity + "|" + id, templates.get("|" + id));
        return template == null ? base : template.copyWithCount(1);
    }
}
