package dev.mikaaah.tnkloot.loot;
import com.google.gson.*;
import dev.mikaaah.tnkloot.compat.*;
import dev.mikaaah.tnkloot.dimension.DimensionRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PackEquipmentPolicyTest {
    @Test void craftingOnlyChoicesDoNotBecomeRandomLoot() {
        for(String id:new String[]{"vinery:straw_hat","brewery:brewfest_hat","herbalbrews:witch_hat","hybrid_aquatic:diving_helmet","netherexp:shotgun_fist","wildernature:blunderbuss","supplementaries:quiver","deeperdarker:sonorous_staff","deeperdarker:soul_elytra","the_beyond:ring_remembrance","ae2:charged_staff","immersiveengineering:railgun"})assertFalse(PackEquipmentRules.lootAllowed(id),id);
        for(String id:new String[]{"minecraft:copper_helmet","deeperdarker:warden_helmet","ae2:fluix_sword","immersiveengineering:armor_steel_chestplate","enderscape:drift_leggings","archaion:echo_mace"})assertTrue(PackEquipmentRules.lootAllowed(id),id);
    }
    @Test void approvedMaterialBandsMatchThePackChoices() {
        var bundled=CompatPresets.bundled("default-tiers.json");PackEquipmentRules.migrate(bundled);assertDoesNotThrow(()->dev.mikaaah.tnkloot.config.TnkConfig.parse(bundled.toString()));
        assertArrayEquals(new int[]{1,2},PackEquipmentRules.band("minecraft:copper_chestplate"));
        assertArrayEquals(new int[]{2,3},PackEquipmentRules.band("ae2:fluix_sword"));
        assertArrayEquals(new int[]{3,4},PackEquipmentRules.band("immersiveengineering:armor_steel_chestplate"));
        assertArrayEquals(new int[]{6,7},PackEquipmentRules.band("deeperdarker:warden_chestplate"));
    }
    @Test void balanceMigrationPreservesOverridesAndOnlyRunsOnce() {
        var root=JsonParser.parseString("{\"weapons\":[{\"item\":\"minecraft:copper_helmet\",\"category\":\"helmet\",\"min_tier\":0,\"max_tier\":7,\"weight\":9,\"enabled\":false}]}").getAsJsonObject();
        assertTrue(PackEquipmentRules.migrate(root));var item=root.getAsJsonArray("weapons").get(0).getAsJsonObject();
        assertEquals(1,item.get("min_tier").getAsInt());assertEquals(2,item.get("max_tier").getAsInt());assertEquals(9,item.get("weight").getAsInt());assertFalse(item.get("enabled").getAsBoolean());
        item.addProperty("min_tier",2);assertFalse(PackEquipmentRules.migrate(root));assertEquals(2,item.get("min_tier").getAsInt());
    }
    @Test void routingMigrationPreservesCustomChancesAndNamespaces() {
        var root=JsonParser.parseString("{\"enabled\":false,\"routes\":[{\"dimension\":\"minecraft:the_end\",\"include_global_equipment\":false,\"bonus_chest_chance\":0.37,\"bonus_hostile_mob_chance\":0.01,\"item_namespaces\":[\"custom\"],\"item_prefixes\":[]}]}").getAsJsonObject();
        assertTrue(DimensionRules.migrate(root));var rules=DimensionRules.parse(root);var end=rules.routes().get("minecraft:the_end");
        assertFalse(rules.enabled());assertFalse(end.includeGlobal());assertEquals(.37,end.chestChance());assertTrue(end.namespaces().containsAll(java.util.List.of("custom","enderscape","the_beyond")));assertTrue(rules.routes().containsKey("deeperdarker:otherside"));assertFalse(DimensionRules.migrate(root));
    }
}
