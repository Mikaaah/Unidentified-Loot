package dev.mikaaah.tnkloot;

import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.ApotheosisCompat;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.loot.WeaponSelector;
import dev.mikaaah.tnkloot.menu.TnkMenu;
import dev.mikaaah.tnkloot.menu.TnkMenus;
import dev.mikaaah.tnkloot.rarity.RarityIntegration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class IdentifierSmokeTest {
   private IdentifierSmokeTest() {
   }

   public static void run(ServerStartedEvent var0) {
      RarityIntegration.run(var0);
   }

   private static void compatibilityChecks(ServerPlayer player) {
      int checked = 0;

      for (TnkConfig.Weapon definition : TnkConfig.get().weapons()) {
         ResourceLocation id = ResourceLocation.parse(definition.id());
         if (!id.getNamespace().equals("minecraft") && WeaponSelector.available(definition)) {
            for (int tier : new int[]{definition.minTier(), Math.min(7, definition.maxTier())}) {
               ItemStack stack = new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(id));
               require(!ApotheosisCompat.apply(stack, tier, player, 314159L + (long)checked).isEmpty(), "Empty compatibility reward " + id);
               checked++;
            }
         }
      }

      for (BossRewards.Pool pool : BossRewards.active()) {
         require(BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(pool.entity())), "Unregistered boss entity " + pool.entity());

         for (BossRewards.Reward reward : pool.items()) {
            ResourceLocation id = ResourceLocation.parse(reward.item());
            require(BuiltInRegistries.ITEM.containsKey(id), "Unregistered boss reward " + id);
            require(
               !ApotheosisCompat.apply(new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(id)), pool.tier(), player, 123L).isEmpty(), "Empty boss reward " + id
            );
            checked++;
         }
      }

      TnkLoot.LOG.info("TNKLOOT_COMPAT_OK: {} imported equipment and boss reward checks", checked);
   }

   private static void bossChecks(ServerPlayer player) throws Exception {
      Path path = BossRewards.path();
      String previous = Files.readString(path);

      try {
         Files.writeString(
            path,
            "{\"enabled\":true,\"require_player_kill\":true,\"pools\":[{\"id\":\"tnkloot/test\",\"mod\":\"tnkloot\",\"entity\":\"minecraft:zombie\",\"name\":\"Test Boss\",\"tier\":7,\"enabled\":true,\"chance\":1,\"additional_if_no_equipment\":true,\"items\":[{\"item\":\"minecraft:diamond_sword\",\"category\":\"weapon\",\"weight\":1}]}]}\n"
         );
         require(BossRewards.reload(), "Boss config rejected");
         BossRewards.Pool pool = BossRewards.find("tnkloot/test").orElseThrow();
         ItemStack original = new ItemStack(Items.DIAMOND_SWORD);
         original.setDamageValue(15);
         ItemStack sealed = BossRewards.wrap(pool, original, player.level());
         require(sealed.getItem() == TnkLoot.bossToken(TnkConfig.Category.WEAPON).get(), "Boss used ordinary token");
         require(ItemStack.matches(original, UnidentifiedWeaponItem.baseResult(player.level(), sealed)), "Native boss components lost");
         require(BossRewards.exclusive("minecraft:diamond_sword"), "Boss item leaked to ordinary pool");
         Zombie zombie = (Zombie)EntityType.ZOMBIE.create(player.level());
         Collection<ItemEntity> drops = new ArrayList<>();
         drops.add(new ItemEntity(player.level(), 0.0, 0.0, 0.0, new ItemStack(Items.ROTTEN_FLESH)));
         LivingDropsEvent event = new LivingDropsEvent(zombie, player.damageSources().playerAttack(player), drops, true);
         BossRewards.drops(event);
         require(drops.size() == 2, "Material-only boss did not add one sealed reward");
         BossRewards.drops(event);
         require(drops.size() == 2, "Boss reward duplicated");
         require(drops.stream().anyMatch(d -> d.getItem().is(Items.ROTTEN_FLESH)), "Boss materials removed");
      } finally {
         Files.writeString(path, previous);
         BossRewards.reload();
      }
   }

   private static void creativeSeedChecks(ServerPlayer player) {
      ItemStack template = UnidentifiedWeaponItem.creativeSample(TnkConfig.Category.WEAPON, 0);
      HashSet<Long> seeds = new HashSet<>();

      for (int i = 0; i < 7; i++) {
         ItemStack copy = template.copy();
         require(UnidentifiedWeaponItem.bindRollSeed(copy), "Creative sample did not get a server seed");
         long seed = UnidentifiedWeaponItem.data(copy).getLong("roll_seed");
         require(seeds.add(seed), "Creative samples share a roll seed");
         require(!UnidentifiedWeaponItem.bindRollSeed(copy), "Bound sample got rerolled");
         require(UnidentifiedWeaponItem.data(copy).getLong("roll_seed") == seed, "Bound seed changed");
      }

      player.getInventory().clearContent();
      TnkMenu menu = new TnkMenu((MenuType<?>)TnkMenus.IDENTIFY.get(), 9, player.getInventory(), false);
      menu.getSlot(0).set(template.copy());
      menu.getSlot(1).set(template.copy());
      menu.broadcastChanges();
      ItemStack first = menu.getSlot(0).getItem().copy();
      require(
         UnidentifiedWeaponItem.data(first).getLong("roll_seed") != UnidentifiedWeaponItem.data(menu.getSlot(1).getItem()).getLong("roll_seed"),
         "Station did not bind copied creative samples independently"
      );
      menu.removed(player);
      TnkMenu reopened = new TnkMenu((MenuType<?>)TnkMenus.IDENTIFY.get(), 10, player.getInventory(), false);
      reopened.getSlot(0).set(first);
      reopened.broadcastChanges();
      require(
         UnidentifiedWeaponItem.data(first).getLong("roll_seed") == UnidentifiedWeaponItem.data(reopened.getSlot(0).getItem()).getLong("roll_seed"),
         "Reopening rerolled a sample"
      );
      reopened.removed(player);
      player.getInventory().clearContent();
   }

   private static void editorChecks(ServerPlayer player) throws Exception {
      Path path = TnkConfig.path();
      String original = Files.readString(path);

      try {
         player.getInventory().clearContent();
         player.getInventory().setItem(9, new ItemStack(Items.STICK, 16));
         TnkMenu editor = new TnkMenu((MenuType<?>)TnkMenus.ADMIN.get(), 4, player.getInventory(), true);
         require(editor.adminView() == 0, "Editor must open at item-group chooser");
         require(editor.clickMenuButton(player, 100 + TnkConfig.Category.WEAPON.ordinal()) && editor.adminView() == 1, "Group card did not open tier chooser");
         require(editor.clickMenuButton(player, 1000) && editor.adminView() == 2, "Tier card did not open group vault");

         for (int i = 0; i < 36; i++) {
            if (editor.getSlot(i).hasItem()) {
               require(TnkMenu.metadata(editor.getSlot(i).getItem()).getString("category").equals("WEAPON"), "Weapon vault contains another item group");
            }
         }

         editor.quickMoveStack(player, 37);
         require(player.getInventory().getItem(9).getCount() == 16, "Vault import consumed inventory stack");
         require(TnkConfig.tierWeapons(0).stream().anyMatch(w -> w.id().equals("minecraft:stick")), "Shift-click did not add item");
         int selected = editor.selectedVaultSlot();
         require(selected >= 0, "Imported item not selected");

         for (ClickType click : ClickType.values()) {
            editor.clicked(selected, 0, click, player);
            require(editor.getCarried().isEmpty(), "Ghost item escaped through " + click);
            require(player.getInventory().getItem(9).getCount() == 16, "Ghost click mutated inventory through " + click);
         }

         editor.clickMenuButton(player, 20);
         require(
            TnkConfig.tierWeapons(0).stream().filter(w -> w.id().equals("minecraft:stick")).noneMatch(TnkConfig.Weapon::enabled), "Disable did not persist"
         );
         require(WeaponSelector.pool(0).roll(() -> 0L).orElseThrow() != Items.STICK, "Disabled item remained rollable");
         editor.clickMenuButton(player, 20);
         editor.setSelectedChance(player, 1, "minecraft:stick", 7500);
         require(
            TnkConfig.tierWeapons(0).stream().filter(w -> w.id().equals("minecraft:stick")).findFirst().orElseThrow().weight() == 1,
            "Wrong-tier chance edit was accepted"
         );
         editor.setSelectedChance(player, 0, "minecraft:stick", 7500);
         List<TnkConfig.Weapon> weapons = TnkConfig.tierWeapons(0)
            .stream()
            .filter(w -> w.category() == TnkConfig.Category.WEAPON && WeaponSelector.available(w))
            .toList();
         long total = weapons.stream().mapToLong(TnkConfig.Weapon::weight).sum();
         require(
            (long)weapons.stream().filter(w -> w.id().equals("minecraft:stick")).findFirst().orElseThrow().weight() * 4L == total * 3L,
            "Chance setter did not produce actual 75 percent"
         );
         editor.setCarried(new ItemStack(Items.PAPER, 12));
         editor.clicked(0, 0, ClickType.PICKUP, player);
         require(editor.getCarried().is(Items.PAPER) && editor.getCarried().getCount() == 12, "Cursor import consumed item");
         editor.setCarried(ItemStack.EMPTY);
         int added = 0;

         for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR && !(item instanceof UnidentifiedWeaponItem)) {
               if (added++ >= 75) {
                  break;
               }

               editor.setCarried(new ItemStack(item));
               editor.clicked(0, 0, ClickType.PICKUP, player);
               editor.setCarried(ItemStack.EMPTY);
            }
         }

         require(editor.pageCount() >= 3, "Vault did not create extra pages");
         editor.clickMenuButton(player, 10);
         require(editor.page() >= 0 && editor.page() < editor.pageCount(), "Vault page outside valid bounds");
         require(editor.clickMenuButton(player, 30) && editor.adminView() == 1, "Vault back button did not open tiers");
         require(editor.clickMenuButton(player, 31) && editor.adminView() == 0, "Group home button failed");
         require(editor.clickMenuButton(player, 100 + TnkConfig.Category.ACCESSORY.ordinal()), "Accessories group cannot be opened");
         require(editor.clickMenuButton(player, 1000), "Empty accessory tier cannot be opened");

         for (int ix = 0; ix < 36; ix++) {
            if (editor.getSlot(ix).hasItem()) {
               require(TnkMenu.metadata(editor.getSlot(ix).getItem()).getString("category").equals("ACCESSORY"), "Accessory vault leaked other groups");
            }
         }

         for (int ixx = 8; ixx < 13; ixx++) {
            require(
               TnkConfig.addTier("nav_" + ixx, "Navigation " + ixx, TnkConfig.get().tiers().getFirst().apotheosisRarity()),
               "Could not create navigation test tier"
            );
         }

         editor.broadcastChanges();
         require(editor.clickMenuButton(player, 30), "Could not return to tiers");
         require(editor.clickMenuButton(player, 33) && editor.tierPage() == 1, "Tier chooser did not paginate");
         require(editor.clickMenuButton(player, 1012) && editor.selectedTier().rank() == 12, "Second tier page opened wrong tier");
         editor.removed(player);
         require(player.getInventory().getItem(9).getCount() == 16, "Closing editor leaked or lost display items");
      } finally {
         Files.writeString(path, original);
         require(TnkConfig.reload(), "Could not restore smoke-test config");
         player.getInventory().clearContent();
      }
   }

   private static void require(boolean ok, String message) {
      if (!ok) {
         throw new IllegalStateException(message);
      }
   }
}
