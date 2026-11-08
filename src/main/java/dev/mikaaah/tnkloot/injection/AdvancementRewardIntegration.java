package dev.mikaaah.tnkloot.injection;

import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Opt-in real-runtime regression for the parameter and final-output mixins. */
public final class AdvancementRewardIntegration {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("tnkloot.advancementRewardSmokeTest")) return;
        var server = event.getServer();
        try {
            var level = server.overworld();
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "TNKRewardTest"));
            require(FinalLoot.enabled(), "Test requires active loot replacement");
            var builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withParameter(LootContextParams.ORIGIN, Vec3.ZERO);
            var rewardParams = builder.create(LootContextParamSets.ADVANCEMENT_REWARD);
            var chestParams = builder.create(LootContextParamSets.CHEST);
            require(((LootParamsSource) rewardParams).tnkloot$isAdvancementReward(), "Reward origin missing");
            require(!((LootParamsSource) chestParams).tnkloot$isAdvancementReward(), "Reused builder leaked reward origin");
            var banner = new ItemStack(Items.WHITE_BANNER, 3);
            banner.set(DataComponents.CUSTOM_NAME, Component.literal("Earned Banner"));
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("reward_owner", "advancement");
            banner.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            var trophy = new ItemStack(Items.PLAYER_HEAD);
            trophy.set(DataComponents.CUSTOM_NAME, Component.literal("Earned Trophy"));
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            // Including real equipment proves that this protects the source, not just two IDs.
            for (String table : List.of("lootr:advancement/trophy", "minecraft:chests/simple_dungeon")) {
                var input = new ObjectArrayList<ItemStack>(List.of(banner.copy(), trophy.copy(), sword.copy()));
                var before = input.stream().map(ItemStack::copy).toList();
                var context = new LootContext.Builder(rewardParams).create(Optional.empty());
                context.setQueriedLootTableId(ResourceLocation.parse(table));
                FinalLoot.table(input, context);
                require(input.size() == before.size(), "TNK changed reward count for " + table);
                for (int i = 0; i < before.size(); i++)
                    require(ItemStack.matches(before.get(i), input.get(i)), "TNK changed reward/components for " + table);
                var output = CommonHooks.modifyLoot(ResourceLocation.parse(table), input, context);
                // Other mods can append their own rewards to a reused chest table.
                // Verify the awarded stacks rather than forbidding their modifiers.
                for (int i = 0; i < 2; i++) {
                    var expected = before.get(i);
                    require(output.stream().anyMatch(stack -> ItemStack.matches(expected, stack)),
                            "Reward count/components changed for " + table);
                }
                // Apotheosis may legitimately add affixes to the sword in its own
                // modifier. TNK must leave it as equipment rather than seal it.
                require(output.stream().anyMatch(stack -> stack.is(Items.DIAMOND_SWORD) && stack.getCount() == 1),
                        "Advancement equipment converted for " + table);
                require(output.stream().noneMatch(stack -> stack.getItem() instanceof UnidentifiedWeaponItem),
                        "TNK sealed advancement or bonus equipment for " + table);
            }
            for (var stack : List.of(banner, trophy, new ItemStack(Items.DIAMOND))) {
                require(FinalLoot.category(stack) == null, "Non-equipment classified as gear");
                require(FinalLoot.replacement(stack, level, player, "test").isEmpty(), "Non-equipment replaced");
            }
            require(FinalLoot.category(sword) == TnkConfig.Category.WEAPON, "Weapon classification regressed");
            var normal = FinalLoot.replacement(sword, level, player, "test");
            require(normal.getItem() instanceof UnidentifiedWeaponItem, "Ordinary equipment conversion stopped");
            require(!UnidentifiedWeaponItem.baseResult(level, normal).isEmpty(), "Ordinary replacement has invalid pool");
            TnkLoot.LOG.info("TNKLOOT_ADVANCEMENT_REWARDS_OK: reward and reused-chest tables preserve counts/components, builder origins isolated, decorative rewards protected, normal equipment still identifiable");
        } catch (Throwable error) {
            TnkLoot.LOG.error("TNKLOOT_ADVANCEMENT_REWARDS_FAILED", error);
        } finally {
            server.halt(false);
        }
    }
}
