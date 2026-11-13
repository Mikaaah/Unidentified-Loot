package dev.mikaaah.tnkloot.verification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.compat.CompatPresets;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.mikaaah.tnkloot.loot.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Runs only in the explicitly selected, isolated compatibility verification server. */
@EventBusSubscriber(modid = TnkLoot.MOD_ID)
public final class PackCompatSmoke {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("tnkloot.compatSmokeTest")) return;
        var server = event.getServer();
        var level = server.overworld();
        JsonObject results = new JsonObject();
        try {
            var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
            var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
            var context = new LootContext.Builder(params).create(Optional.empty());
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "TNKCompat"));
            Set<Long> seeds = new HashSet<>();
            int rolls = 0;
            for (int tier = 0; tier < 8; tier++) for (String rarity : TnkLootRolls.RARITIES) for (var category : TnkConfig.Category.values()) {
                String function = "{\"function\":\"tnkloot:seal_reward\",\"tier\":" + tier + ",\"source_level\":94,\"rarity\":\"" + rarity + "\"}";
                String json = "{\"type\":\"minecraft:chest\",\"pools\":[{\"rolls\":2,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"tnkloot:" + category.token() + "\",\"functions\":[" + function + "]}]},{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:diamond\"}]}]}";
                var table = LootTable.DIRECT_CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
                var loot = table.getRandomItems(params);
                require(loot.size() == 3, "Explicit chest rewards changed count");
                require(loot.stream().filter(s -> s.is(Items.DIAMOND)).count() == 1, "Material reward was changed");
                var snapshot = loot.stream().map(ItemStack::copy).toList();
                FinalLoot.table(loot, context);
                for (int i = 0; i < loot.size(); i++) require(ItemStack.matches(snapshot.get(i), loot.get(i)), "Repeated final processing changed a sealed reward");
                for (var token : loot) if (token.getItem() instanceof UnidentifiedWeaponItem item) {
                    var before = token.copy();
                    TnkLootRolls.sealStarter(token);
                    TnkLootRolls.cap(token, player, 1);
                    require(ItemStack.matches(before, token), "A later player or automation rerolled the floor reward");
                    var tag = UnidentifiedWeaponItem.data(token);
                    require(item.category() == category && UnidentifiedWeaponItem.tier(token) == tier, "Reward tier/category mismatch");
                    require(tag.getString("tnk_rarity").equals(rarity), "Reward rarity mismatch");
                    require(tag.getInt("tnk_source_level") == 94 && tag.getBoolean("tnk_progression_sealed"), "Reward was not sealed");
                    require(seeds.add(tag.getLong("roll_seed")), "Independent rolls reused a seed");
                    var restored = ItemStack.parseOptional(server.registryAccess(), (net.minecraft.nbt.CompoundTag) token.save(server.registryAccess()));
                    require(ItemStack.matches(token, restored), "Saved reward lost its sealed data");
                    rolls++;
                }
            }
            for (String invalid : List.of("{\"tier\":8,\"source_level\":1,\"rarity\":\"common\"}", "{\"tier\":1,\"source_level\":0,\"rarity\":\"common\"}", "{\"tier\":1,\"source_level\":1,\"rarity\":\"ancient\"}")) {
                require(SealRewardFunction.CODEC.codec().parse(ops, JsonParser.parseString(invalid)).error().isPresent(), "Invalid reward strength was accepted");
            }
            results.addProperty("sealed_tokens", rolls);
            results.addProperty("unique_seeds", seeds.size());
            var explicit = SealRewardFunction.CODEC.codec().parse(ops, JsonParser.parseString("{\"tier\":3,\"source_level\":60,\"rarity\":\"tnkloot:epic\"}")).getOrThrow();
            for (var unchanged : List.of(new ItemStack(Items.EMERALD, 7), new ItemStack(TnkLoot.bossToken(TnkConfig.Category.WEAPON).get()))) {
                var before = unchanged.copy();
                require(ItemStack.matches(before, explicit.apply(unchanged, context)), "Non-equipment or boss reward was rewritten");
            }
            var named = new ItemStack(TnkLoot.UNIDENTIFIED_WEAPON.get());
            var lore = new net.minecraft.world.item.component.ItemLore(List.of(net.minecraft.network.chat.Component.literal("Floor prize")));
            named.set(net.minecraft.core.component.DataComponents.LORE, lore);
            explicit.apply(named, context);
            require(lore.equals(named.get(net.minecraft.core.component.DataComponents.LORE)), "Reward customization was lost");

            // Exercise the real shipped config migration, including already-imported stale entries.
            var root = JsonParser.parseString(Files.readString(TnkConfig.path())).getAsJsonObject();
            for (String id : List.of("efn:nf_shortsword", "irons_jewelry:ring", "irons_jewelry:necklace")) {
                var entry = new JsonObject(); entry.addProperty("item", id); entry.addProperty("category", "accessory");
                entry.addProperty("min_tier", 0); entry.addProperty("max_tier", 7); entry.addProperty("weight", 1);
                root.getAsJsonArray("weapons").add(entry);
            }
            Files.writeString(TnkConfig.path(), root.toString());
            require(CompatPresets.install(), "Compat cleanup failed");
            require(TnkConfig.get().weapons().stream().noneMatch(w -> w.id().startsWith("efn:") || w.id().startsWith("irons_jewelry:")), "Removed random rewards were reimported");
            require(Files.exists(TnkConfig.path().resolveSibling("tiers.json.before-1.1.3.bak")), "Migration backup missing");

            if (ModList.get().isLoaded("irons_jewelry")) {
                var data = Class.forName("io.redspace.ironsjewelry.core.data.JewelryData");
                for (String id : List.of("irons_jewelry:ring", "irons_jewelry:necklace")) {
                    var definition = new TnkConfig.Weapon(id, TnkConfig.Category.ACCESSORY, 0, 7, 1);
                    require(!WeaponSelector.available(definition), "A manually added blank jewel remained eligible");
                    for (int tier = 0; tier < 8; tier++) require(DimensionLoot.pool(tier, TnkConfig.Category.ACCESSORY, "minecraft:overworld").stream().noneMatch(w -> w.id().equals(id)), "Blank jewelry remained in an identification pool");
                }
                var table = server.reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, ResourceLocation.parse("irons_jewelry:generate_jewelry_test")));
                int nativeJewels = 0;
                for (int i = 0; i < 30; i++) for (var stack : table.getRandomItems(params)) {
                    if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("irons_jewelry")) continue;
                    require((boolean) data.getMethod("isValid").invoke(data.getMethod("get", ItemStack.class).invoke(null, stack)), "Iron's native jewellery generator returned invalid data");
                    var before = stack.copy();
                    var loot = new ObjectArrayList<ItemStack>(List.of(stack));
                    FinalLoot.table(loot, context);
                    require(loot.stream().anyMatch(s -> ItemStack.matches(before, s)), "Native jewel was removed or rewritten");
                    nativeJewels++;
                }
                require(nativeJewels > 0, "Native jewellery test produced no jewellery");
                results.addProperty("native_jewels_preserved", nativeJewels);
            }

            if (ModList.get().isLoaded("spiral_abyss") && !dev.mikaaah.tnkloot.dungeon.DungeonData.catalog().tables().isEmpty()) {
                results.add("dungeon_rewards", DungeonRewardSmoke.verify(server));
            } else if (ModList.get().isLoaded("spiral_abyss")) {
                var catalog = Class.forName("com.pla.spiral_abyss.encounter.data.FloorCatalog").getMethod("snapshot").invoke(null);
                var floors = (Map<?, ?>) catalog.getClass().getMethod("floors").invoke(catalog);
                var rewardsType = Class.forName("com.pla.spiral_abyss.encounter.data.FloorDefinition$Rewards");
                var roll = Class.forName("com.pla.spiral_abyss.encounter.FloorRewards").getMethod("roll", net.minecraft.server.level.ServerLevel.class, Vec3.class, rewardsType, int.class, int.class);
                int[] tiers = {1, 2, 3, 4, 6, 7}; int[] levels = {20, 40, 60, 80, 94, 100};
                int spiralTokens = 0;
                for (int floor = 1; floor <= 6; floor++) {
                    var definition = floors.get(floor);
                    require(definition != null, "Missing Spiral floor " + floor);
                    var rewards = definition.getClass().getMethod("rewards").invoke(definition);
                    for (int chest = 1; chest <= 3; chest++) {
                        var bundle = roll.invoke(null, level, Vec3.ZERO, rewards, chest, 1);
                        var items = (List<?>) bundle.getClass().getMethod("items").invoke(bundle);
                        int count = 0;
                        for (var value : items) if (value instanceof ItemStack token && token.getItem() instanceof UnidentifiedWeaponItem) {
                            var tag = UnidentifiedWeaponItem.data(token);
                            require(UnidentifiedWeaponItem.tier(token) == tiers[floor - 1] && tag.getInt("tnk_source_level") == levels[floor - 1] && tag.getBoolean("tnk_progression_sealed"), "Spiral floor strength was overwritten");
                            require(seeds.add(tag.getLong("roll_seed")), "Spiral rewards reused a seed");
                            count++; spiralTokens++;
                        }
                        require(count == chest, "Spiral copper/iron/gold roll count changed: " + floor + "/" + chest + " -> " + count);
                    }
                }
                results.addProperty("spiral_floor_chest_cases", 18);
                results.addProperty("spiral_tokens", spiralTokens);
            }
            if(ModList.get().isLoaded("archaion") && ModList.get().isLoaded("enderscape"))results.add("approved_equipment",ApprovedEquipmentSmoke.verify(server));
            Files.createDirectories(java.nio.file.Path.of("verification"));
            Files.writeString(java.nio.file.Path.of("verification/compat-results.json"), results.toString());
            TnkLoot.LOG.info("TNKLOOT_PACK_COMPAT_OK: {}", results);
        } catch (Throwable error) {
            TnkLoot.LOG.error("TNKLOOT_PACK_COMPAT_FAILED", error);
        } finally {
            server.halt(false);
        }
    }
}
