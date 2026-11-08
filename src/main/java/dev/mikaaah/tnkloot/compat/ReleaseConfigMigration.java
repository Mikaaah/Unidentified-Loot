package dev.mikaaah.tnkloot.compat;

import com.google.gson.*;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** One-time cleanup of the shipped Beta 1.0 preset; user-added entries are retained. */
public final class ReleaseConfigMigration {
    private ReleaseConfigMigration() {}
    public static void started(ServerStartedEvent event) {
        migrate();
        NativeLootTemplates.load(event.getServer());
        EquipmentAudit.started(event);
        ReleaseSmoke.run(event);
    }
    public static synchronized boolean migrate() {
        try {
            var baseline = CompatPresets.bundled("tnk-release-baseline.json");
            Set<String> stockItems = new HashSet<>(); baseline.getAsJsonArray("items").forEach(v -> stockItems.add(v.getAsString()));
            Path tiersPath = TnkConfig.path(); var tiers = JsonParser.parseString(Files.readString(tiersPath)).getAsJsonObject();
            if (tiers.has("tnk_release_revision") && tiers.get("tnk_release_revision").getAsInt() >= 1) return true;
            int removed = 0;
            var entries = tiers.getAsJsonArray("weapons");
            for (int i = entries.size() - 1; i >= 0; i--) {
                String id = entries.get(i).getAsJsonObject().get("item").getAsString();
                if (stockItems.contains(id) && (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)) || BossRewards.exclusive(id))) {
                    entries.remove(i); removed++;
                }
            }
            for (var value : tiers.getAsJsonArray("tiers")) {
                var tier = value.getAsJsonObject();
                if (tier.get("apotheosis_rarity").getAsString().equals("tnkloot:ancient")) tier.addProperty("apotheosis_rarity", "tnkloot:mythic");
            }
            tiers.addProperty("tnk_release_revision", 1);
            tiers.addProperty("progression_mode", "MANUAL");
            TnkConfig.parse(tiers.toString());
            var bosses = JsonParser.parseString(Files.readString(BossRewards.path())).getAsJsonObject();
            var pools = bosses.getAsJsonArray("pools");
            for (int i = pools.size() - 1; i >= 0; i--) {
                var pool = pools.get(i).getAsJsonObject();
                for (var value : pool.getAsJsonArray("items")) {
                    var reward = value.getAsJsonObject();
                    if (reward.get("item").getAsString().equals("legendary_monsters:ender_hammer")) reward.addProperty("item", "legendary_monsters:monstrous_anchor");
                    if (reward.get("item").getAsString().equals("legendary_monsters:sickle_of_harvest")) { reward.addProperty("item", "legendary_monsters:totem_of_moss"); reward.addProperty("category", "accessory"); }
                }
                if (pool.get("id").getAsString().equals("legendary_monsters/cloud_golem") && pool.getAsJsonArray("items").asList().stream().noneMatch(v -> v.getAsJsonObject().get("item").getAsString().equals("legendary_spellbooks:stormbound_grimoire"))) {
                    var reward = new JsonObject(); reward.addProperty("item", "legendary_spellbooks:stormbound_grimoire"); reward.addProperty("category", "accessory"); reward.addProperty("weight", 1); pool.getAsJsonArray("items").add(reward);
                }
                // Sculptor's trial is an interaction reward, not a registered boss entity.
                if (pool.get("id").getAsString().equals("mowziesmobs/sculptor_test")) pools.remove(i);
                else if (Set.of("aether/slider", "aether/valkyrie_queen", "aether/sun_spirit", "twilightforest/knight_phantom").contains(pool.get("id").getAsString())) pool.addProperty("additional_if_no_equipment", false);
            }
            BossRewards.parse(bosses); save(BossRewards.path(), bosses); save(tiersPath, tiers);
            TnkConfig.reload(); BossRewards.reload();
            TnkLoot.LOG.info("TNK release preset migrated: {} unavailable or boss-only stock equipment rules removed; custom rules retained", removed);
            return true;
        } catch (Exception ex) {
            TnkLoot.LOG.error("TNK release preset migration failed; see existing config backups", ex); return false;
        }
    }
    private static void save(Path path, JsonObject root) throws java.io.IOException {
        Path backup = path.resolveSibling(path.getFileName() + ".before-tnk-release-1.bak");
        if (!Files.exists(backup)) Files.copy(path, backup);
        Path temporary = path.resolveSibling(path.getFileName() + ".release.tmp");
        Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
