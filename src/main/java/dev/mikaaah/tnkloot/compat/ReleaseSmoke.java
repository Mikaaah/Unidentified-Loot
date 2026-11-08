package dev.mikaaah.tnkloot.compat;

import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.mikaaah.tnkloot.injection.LootParamsSource;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in tests against the actually installed TNK dependency JARs. */
public final class ReleaseSmoke {
    private ReleaseSmoke() {}
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("tnkloot.releaseSmokeTest")) return;
        var server = event.getServer(); var level = server.overworld();
        try {
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "TNKRelease"));
            var profile = (CompoundTag) Class.forName("dev.mikaaah.tnkprogression.ProgressionAPI").getMethod("profile", ServerPlayer.class).invoke(null, player);
            profile.putInt("level", 100); profile.putInt("ceiling", 100);
            int identifications = 0, rewards = 0, equipment = 0;
            var policyFile = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("kubejs/pack_catalog.json");
            byte[] priorPolicy = java.nio.file.Files.exists(policyFile) ? java.nio.file.Files.readAllBytes(policyFile) : null;
            var priorExclusions = PackItemPolicy.disabled();
            try {
                java.nio.file.Files.createDirectories(policyFile.getParent());
                java.nio.file.Files.writeString(policyFile, "{\"fullyDisabledItems\":[\"minecraft:diamond_sword\"],\"infiniteFoods\":[]}");
                require(PackItemPolicy.reload() && !PackItemPolicy.allowed("minecraft:diamond_sword"), "Pack policy override was ignored");
                TnkLoot.LOG.info("TNKLOOT_POLICY_CHECK: intentionally rejecting a malformed test override");
                java.nio.file.Files.writeString(policyFile, "invalid policy");
                require(!PackItemPolicy.reload() && !PackItemPolicy.allowed("minecraft:diamond_sword"), "Invalid policy discarded active exclusions");
            } finally {
                if (priorPolicy == null) java.nio.file.Files.deleteIfExists(policyFile); else java.nio.file.Files.write(policyFile, priorPolicy);
                PackItemPolicy.reload();
                require(PackItemPolicy.disabled().equals(priorExclusions), "Policy override was not restored");
            }
            for (String blocked : PackItemPolicy.disabled()) {
                require(TnkConfig.get().weapons().stream().noneMatch(w -> w.id().equals(blocked) && WeaponSelector.available(w)), "Disabled pack item imported: " + blocked);
                for (var pool : BossRewards.active()) require(BossRewards.options(pool, TnkConfig.Category.WEAPON).stream().noneMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(blocked)), "Disabled pack item in boss options: " + blocked);
            }
            TnkLoot.LOG.info("TNKLOOT_PACK_POLICY_OK: overrides/reload retained; disabled equipment excluded from normal and boss pools");
            for (int source = 1; source <= 100; source++) {
                var token = TnkLootRolls.roll(player, source, "release_check");
                require(!token.isEmpty(), "Empty equipment pool at level " + source);
                var snapshot = token.copy(); UnidentifiedWeaponItem.bindRollSeed(token);
                require(ItemStack.matches(snapshot, token), "Opening rerolled token");
                var result = ApotheosisCompat.apply(UnidentifiedWeaponItem.baseResult(level, token), UnidentifiedWeaponItem.tier(token), player, UnidentifiedWeaponItem.data(token).getLong("roll_seed"));
                require(!result.isEmpty(), "Identification failed at source level " + source); identifications++;
            }
            for (var definition : TnkConfig.get().weapons()) {
                if (!WeaponSelector.available(definition) || BossRewards.exclusive(definition.id())) continue;
                for (String rarity : List.of("common", "mythic")) {
                    int tier = rarity.equals("common") ? definition.minTier() : Math.min(7, definition.maxTier());
                    var token = UnidentifiedWeaponItem.create(definition.category(), tier, definition.id(), "release_check");
                    RaritySupport.force(token, rarity);
                    var base = RaritySupport.base(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(definition.id()))), token);
                    var result = ApotheosisCompat.apply(base, tier, player, UnidentifiedWeaponItem.data(token).getLong("roll_seed"));
                    require(!result.isEmpty(), "Equipment identification failed: " + definition.id());
                    for (var modifier : result.getAttributeModifiers().modifiers()) require(Double.isFinite(modifier.modifier().amount()), "Non-finite attribute on " + definition.id());
                    equipment++;
                }
            }
            for (var pool : BossRewards.active()) for (var reward : pool.items()) {
                var id = ResourceLocation.parse(reward.item());
                if (!BuiltInRegistries.ITEM.containsKey(id) && !net.neoforged.fml.ModList.get().isLoaded(id.getNamespace())) continue;
                require(BuiltInRegistries.ITEM.containsKey(id), "Invalid boss reward " + pool.id() + ": " + id);
                var token = BossRewards.token(pool, TnkConfig.Category.WEAPON, level, ItemStack.EMPTY);
                var selected = NativeLootTemplates.apply(pool.entity(), new ItemStack(BuiltInRegistries.ITEM.get(id)));
                var tag = UnidentifiedWeaponItem.data(token); tag.put("boss_result", selected.save(level.registryAccess()));
                token.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); RaritySupport.force(token, "mythic");
                var base = UnidentifiedWeaponItem.baseResult(level, token);
                require(base.is(selected.getItem()), "Universal boss crate rejected " + reward.category() + " reward " + id);
                var result = ApotheosisCompat.apply(base, pool.tier(), player, UnidentifiedWeaponItem.data(token).getLong("roll_seed"));
                require(!result.isEmpty(), "Boss result failed: " + id);
                for (var component : selected.getComponentsPatch().entrySet()) {
                    var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.getKey());
                    if (key != null && key.getNamespace().equals("irons_spellbooks") && component.getValue().isPresent()) require(Objects.equals(result.get(component.getKey()), component.getValue().get()), "Native spell components lost: " + id);
                }
                rewards++;
            }
            var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.ADVANCEMENT_REWARD);
            require(((LootParamsSource) params).tnkloot$isAdvancementReward(), "Advancement-origin mixin is inactive");
            int protectedRewards = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                if (!id.contains("trophy") && !id.endsWith("_banner") && item != Items.DIAMOND_SWORD && item != Items.PLAYER_HEAD) continue;
                var stack = new ItemStack(item, Math.min(3, item.getDefaultMaxStackSize()));
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Earned reward"));
                var loot = new ObjectArrayList<ItemStack>(List.of(stack.copy()));
                var context = new LootContext.Builder(params).create(Optional.empty());
                context.setQueriedLootTableId(ResourceLocation.parse("minecraft:chests/simple_dungeon"));
                FinalLoot.table(loot, context);
                require(loot.size() == 1 && ItemStack.matches(stack, loot.getFirst()), "Advancement reward changed: " + id); protectedRewards++;
            }
            if (dev.mikaaah.tnkloot.features.FeatureConfig.get().collection()) {
                var sampleToken = UnidentifiedWeaponItem.create(2); RaritySupport.force(sampleToken, "rare");
                var gear = RaritySupport.base(new ItemStack(Items.DIAMOND_SWORD), sampleToken);
                player.getInventory().setItem(0, gear);
                dev.mikaaah.tnkloot.features.PlayerLedger.observe(player); dev.mikaaah.tnkloot.features.PlayerLedger.observe(player);
                var found = dev.mikaaah.tnkloot.features.PlayerLedger.read(player).getCompound("discoveries").getCompound("minecraft:diamond_sword");
                require(found.contains("first") && found.getInt("count")==0, "Automated discovery inflated identification count");
                dev.mikaaah.tnkloot.features.PlayerLedger.discovered(player, gear); dev.mikaaah.tnkloot.features.PlayerLedger.observe(player);
                require(dev.mikaaah.tnkloot.features.PlayerLedger.read(player).getCompound("discoveries").getCompound("minecraft:diamond_sword").getInt("count")==1, "Repeated inventory scans inflated identification count");
            }
            EquipmentAudit.write(server);
            TnkLoot.LOG.info("TNKLOOT_RELEASE_OK: {} source-level identifications, {} equipment/rarity checks, {} boss rewards, {} actual advancement/banner/trophy rewards preserved", identifications, equipment, rewards, protectedRewards);
        } catch (Throwable failure) { TnkLoot.LOG.error("TNKLOOT_RELEASE_FAILED", failure); }
        finally { server.halt(false); }
    }
}
