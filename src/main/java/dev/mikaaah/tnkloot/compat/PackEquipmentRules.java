package dev.mikaaah.tnkloot.compat;

import com.google.gson.*;
import java.util.*;

/** Explicit TNK3 loot choices. Crafting and owned-item reforging remain independent. */
public final class PackEquipmentRules {
    private static final Set<String> NATIVE_ONLY = Set.of("vinery", "brewery", "herbalbrews", "hybrid_aquatic", "netherexp", "wildernature", "supplementaries");
    private static final Set<String> EXCLUDED = Set.of("deeperdarker:sonorous_staff", "deeperdarker:soul_elytra", "the_beyond:ring_remembrance", "archaion:echos_grace");
    private PackEquipmentRules() {}
    private static final class Definitions {
        static final JsonObject DATA = CompatPresets.bundled("approved-pack-compat.json");
        static final Map<String, JsonObject> ITEMS = items();
        private static Map<String, JsonObject> items() {
            Map<String, JsonObject> items = new LinkedHashMap<>();
            for (var p : DATA.getAsJsonArray("profiles")) for (var e : p.getAsJsonObject().getAsJsonArray("items")) {
                var item = e.getAsJsonObject(); items.put(item.get("item").getAsString(), item);
            }
            return Map.copyOf(items);
        }
    }
    public static boolean nativeOnly(String id) {
        int colon=id.indexOf(':'); String namespace=colon<0?"":id.substring(0,colon);
        if(NATIVE_ONLY.contains(namespace) || EXCLUDED.contains(id))return true;
        // Import the approved ordinary equipment, never blank powered/modular devices.
        return Set.of("immersiveengineering", "ae2", "deeperdarker", "the_beyond", "enderscape").contains(namespace) && !Definitions.ITEMS.containsKey(id);
    }
    public static boolean lootAllowed(String id) { return !nativeOnly(id); }
    public static boolean managed(String id) { return Definitions.ITEMS.containsKey(id); }
    public static int[] band(String id) {
        var item=Definitions.ITEMS.get(id);
        return item==null?null:new int[]{item.get("min_tier").getAsInt(),item.get("max_tier").getAsInt()};
    }
    public static dev.mikaaah.tnkloot.config.TnkConfig.Category category(String id) {
        if(id.equals("archaion:echo_mace"))return dev.mikaaah.tnkloot.config.TnkConfig.Category.WEAPON;
        var item=Definitions.ITEMS.get(id);
        return item==null?null:dev.mikaaah.tnkloot.config.TnkConfig.Category.valueOf(item.get("category").getAsString().toUpperCase(Locale.ROOT));
    }
    public static void appendProfiles(JsonArray profiles) {
        for(var profile:Definitions.DATA.getAsJsonArray("profiles"))profiles.add(profile.deepCopy());
    }
    /** Apply the newly approved tiers once; retain enabled flags, weights and later edits. */
    public static boolean migrate(JsonObject root) {
        if(root.has("tnk_pack_compat_revision") && root.get("tnk_pack_compat_revision").getAsInt()>=1)return false;
        for(var element:root.getAsJsonArray("weapons")) {
            var old=element.getAsJsonObject();var rule=Definitions.ITEMS.get(old.get("item").getAsString());
            if(rule!=null){old.add("min_tier",rule.get("min_tier").deepCopy());old.add("max_tier",rule.get("max_tier").deepCopy());old.add("category",rule.get("category").deepCopy());}
        }
        root.addProperty("tnk_pack_compat_revision",1);return true;
    }
}
