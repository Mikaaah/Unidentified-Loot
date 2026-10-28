package dev.mikaaah.tnkloot;

import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.menu.*;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.UUID;

/** Opt-in CI integration checks against a running, fully reloaded Apotheosis server. */
public final class IdentifierSmokeTest {
    private IdentifierSmokeTest() {}
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("tnkloot.smokeTest")) return;
        var server = event.getServer();
        try {
            var player = new net.neoforged.neoforge.common.util.FakePlayer(server.overworld(),
                new GameProfile(UUID.randomUUID(), "TNKSmoke")) {
                    @Override public boolean hasDisconnected() { return false; }
                };
            int checks = 0;
            for (var tier : TnkConfig.get().tiers()) {
                for (var category : TnkConfig.Category.values()) {
                    if (TnkConfig.get().weapons().stream().noneMatch(w -> w.category() == category)) continue;
                    for (int seed = 0; seed < 4; seed++) {
                        ItemStack base = WeaponSelector.roll(player.level(), tier.rank(), category, net.minecraft.util.RandomSource.create(seed));
                        require(!base.isEmpty(), "Empty base pool " + tier.id() + "/" + category);
                        ItemStack result = ApotheosisCompat.apply(base, tier.rank(), player, seed);
                        int affixes = result.getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY).size();
                        require(affixes >= 1, "Missing affixes " + tier.id() + "/" + category);
                        checks++;
                    }
                }
            }
            // A paid seven-item batch must return seven final items when closed mid-animation.
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(Items.EMERALD, 7));
            TnkMenu menu = new TnkMenu(TnkMenus.IDENTIFY.get(), 1, player.getInventory(), false);
            for (int i = 0; i < 7; i++) menu.getSlot(i).set(UnidentifiedWeaponItem.create(0));
            require(menu.clickMenuButton(player, 0) && menu.busy(), "Batch did not start");
            require(player.getInventory().countItem(Items.EMERALD) == 0, "Batch cost not charged exactly once");
            ItemStack locked = menu.getSlot(0).getItem().copy();
            menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.THROW, player);
            require(ItemStack.matches(locked, menu.getSlot(0).getItem()), "Can extract a rolling slot");
            require(!menu.clickMenuButton(player, 0), "Could start a second paid batch while rolling");
            menu.removed(player);
            int found = 0;
            for (var stack : player.getInventory().items) if (!stack.isEmpty()) {
                require(!(stack.getItem() instanceof UnidentifiedWeaponItem), "Paid token returned instead of result");
                require(stack.getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY).size() > 0, "Preview item escaped");
                found += stack.getCount();
            }
            require(found == 7, "Lost or duplicated results on close: " + found);
            menu.removed(player);
            require(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum() == 7, "Duplicate close duplicated items");

            // Insufficient funds must preserve the complete batch.
            player.getInventory().clearContent();
            TnkMenu unpaid = new TnkMenu(TnkMenus.IDENTIFY.get(), 2, player.getInventory(), false);
            unpaid.getSlot(0).set(UnidentifiedWeaponItem.create(0));
            unpaid.clickMenuButton(player, 0);
            require(!unpaid.busy() && unpaid.getSlot(0).getItem().getItem() instanceof UnidentifiedWeaponItem, "Unpaid batch consumed token");
            unpaid.removed(player);
            player.getInventory().clearContent();

            // Exercise normal rolling completion using server game-time increments.
            player.getInventory().setItem(0, new ItemStack(Items.EMERALD, 7));
            TnkMenu completed = new TnkMenu(TnkMenus.IDENTIFY.get(), 3, player.getInventory(), false);
            for (int i = 0; i < 7; i++) completed.getSlot(i).set(UnidentifiedWeaponItem.create(0));
            completed.clickMenuButton(player, 0);
            long clock = server.overworld().getGameTime();
            for (int tick = 0; tick < 400 && completed.busy(); tick++) {
                server.getWorldData().overworldData().setGameTime(clock + tick + 1);
                completed.broadcastChanges();
            }
            require(!completed.busy(), "Roll animation never finished");
            for (int i = 0; i < 7; i++) require(completed.getSlot(i).getItem()
                .getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY).size() > 0, "Animation stopped on a preview");
            completed.removed(player);
            require(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum() == 7, "Completed roll lost results");
            server.getWorldData().overworldData().setGameTime(clock);
            BlockPos pos = server.overworld().getSharedSpawnPos().above(10);
            var block = TnkLoot.IDENTIFIER_BLOCK.get();
            var state = block.defaultBlockState();
            server.overworld().setBlockAndUpdate(pos, state);
            block.setPlacedBy(server.overworld(), pos, state, player, new ItemStack(TnkLoot.IDENTIFIER_ITEM.get()));
            var npcs = server.overworld().getEntitiesOfClass(Villager.class, new AABB(pos).inflate(2), IdentifierInteractions::isIdentifier);
            require(npcs.size() == 1, "Expected one identifier in active spawn chunk, found " + npcs.size());
            require(npcs.getFirst().isInvulnerable(), "Identifier not protected");
            require(Math.abs(npcs.getFirst().getYRot() - state.getValue(IdentifierBlock.FACING).toYRot()) < 0.01, "Identifier facing mismatch");
            server.overworld().removeBlock(pos, false);
            require(npcs.getFirst().isRemoved(), "Identifier not removed with counter");
            TnkLoot.LOG.info("TNKLOOT_SMOKE_OK: {} live affix rolls, seven-slot payment/close, and NPC lifecycle passed", checks);
            server.halt(false);
        } catch (Throwable error) {
            TnkLoot.LOG.error("TNKLOOT_SMOKE_FAILED", error);
            server.halt(false);
        }
    }
    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
}
