package dev.mikaaah.tnkloot.menu;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import dev.mikaaah.tnkloot.dimension.NativeDisplays;
import dev.mikaaah.tnkloot.dungeon.DungeonEquipment;
import dev.mikaaah.tnkloot.features.FeatureHooks;
import dev.mikaaah.tnkloot.loot.ApotheosisCompat;
import dev.mikaaah.tnkloot.loot.IdentifierBlock;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.loot.WeaponSelector;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import dev.mikaaah.tnkloot.workshop.ExperienceCosts;
import dev.mikaaah.tnkloot.workshop.WorkshopNavigation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.IContainerFactory;

public final class TnkMenu extends AbstractContainerMenu {
   public static final IContainerFactory<TnkMenu> IDENTIFY_FACTORY = (id, inv, buf) -> new TnkMenu(
         (MenuType<?>)TnkMenus.IDENTIFY.get(), id, inv, false, TnkConfig.parseTierSnapshot(buf.readUtf(32767))
      );
   public static final IContainerFactory<TnkMenu> ADMIN_FACTORY = (id, inv, buf) -> new TnkMenu(
         (MenuType<?>)TnkMenus.ADMIN.get(), id, inv, true, TnkConfig.parseTierSnapshot(buf.readUtf(32767))
      );
   public static final int INPUTS = 7;
   public static final int GROUPS = 0;
   public static final int TIERS = 1;
   public static final int VAULT = 2;
   public static final int TIERS_PER_PAGE = 12;
   public static final int VAULT_PAGE_SIZE = 36;
   public static final int VAULT_SLOTS = 37;
   private final SimpleContainer vault = new SimpleContainer(37);
   private TnkConfig.Settings seenSettings;
   private List<TnkConfig.Weapon> vaultEntries = List.of();
   private final boolean admin;
   private final ContainerData data = new SimpleContainerData(21);
   private final List<TnkConfig.Tier> displayTiers;
   private final SimpleContainer input = new SimpleContainer(7);
   private final Player owner;
   private final Vec3 openingPosition;
   private final ItemStack[] results = new ItemStack[7];
   private final List<List<ItemStack>> previews = new ArrayList<>();
   private int rollingSlot = -1;
   private int frame;
   private long lastTick = Long.MIN_VALUE;
   private boolean closing;
   private BlockPos station;

   public TnkMenu atStation(BlockPos pos) {
      this.station = pos.immutable();
      return this;
   }

   public TnkMenu(MenuType<?> type, int id, Inventory inventory, boolean admin) {
      this(type, id, inventory, admin, TnkConfig.get().tiers());
   }

   public TnkMenu(MenuType<?> type, int id, Inventory inventory, boolean admin, List<TnkConfig.Tier> displayTiers) {
      super(type, id);
      this.admin = admin;
      this.displayTiers = displayTiers;
      this.owner = inventory.player;
      this.openingPosition = this.owner.position();
      Arrays.fill(this.results, ItemStack.EMPTY);

      for (int i = 0; i < 7; i++) {
         this.previews.add(List.of());
      }

      this.addDataSlots(this.data);
      if (!admin) {
         for (int i = 0; i < 7; i++) {
            this.addSlot(new Slot(this.input, i, 25 + i * 24, 57) {
               public boolean mayPlace(ItemStack stack) {
                  return !TnkMenu.this.busy() && stack.getItem() instanceof UnidentifiedWeaponItem;
               }

               public boolean mayPickup(Player p) {
                  return !TnkMenu.this.busy();
               }

               public int getMaxStackSize() {
                  return 1;
               }
            });
         }

         for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
               this.addSlot(new Slot(inventory, col + row * 9 + 9, 25 + col * 18, 134 + row * 18));
            }
         }

         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 25 + col * 18, 192));
         }
      } else {
         for (int i = 0; i < 37; i++) {
            final int n = i;
            this.addSlot(new Slot(this.vault, i, i < 36 ? 16 + i % 9 * 18 : -10000, i < 36 ? 49 + i / 9 * 18 : -10000) {
               public boolean mayPlace(ItemStack stack) {
                  return false;
               }

               public boolean mayPickup(Player p) {
                  return false;
               }

               public boolean isActive() {
                  return n < 36 && TnkMenu.this.adminView() == 2;
               }
            });
         }

         for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
               this.addSlot(new Slot(inventory, col + row * 9 + 9, 16 + col * 18, 166 + row * 18) {
                  public boolean isActive() {
                     return TnkMenu.this.adminView() == 2;
                  }
               });
            }
         }

         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 16 + col * 18, 224) {
               public boolean isActive() {
                  return TnkMenu.this.adminView() == 2;
               }
            });
         }

         if (this.owner instanceof ServerPlayer) {
            this.refreshVault();
         }
      }
   }

   public boolean stillValid(Player player) {
      return this.admin
         ? player.isAlive() && (player.level().isClientSide || player.hasPermissions(2))
         : player.isAlive()
            && player.distanceToSqr(this.openingPosition) <= 64.0
            && TnkConfig.get().stationIdentification()
            && (this.station == null || player.level().getBlockState(this.station).getBlock() instanceof IdentifierBlock);
   }

   public boolean admin() {
      return this.admin;
   }

   public boolean busy() {
      return this.data.get(3) != 0;
   }

   public int quotedCost() {
      return this.data.get(1) & 65535 | (this.data.get(2) & 65535) << 16;
   }

   public int inputCount() {
      return this.data.get(4);
   }

   public boolean affordable() {
      return this.data.get(6) != 0;
   }

   public Item currency() {
      return Item.byId(this.data.get(5));
   }

   public int rollingIndex() {
      return this.admin ? -1 : this.data.get(0) - 1;
   }

   public int selectedIndex() {
      return this.data.get(0);
   }

   public List<TnkConfig.Tier> displayTiers() {
      return this.displayTiers;
   }

   public TnkConfig.Tier selectedTier() {
      List<TnkConfig.Tier> tiers = TnkConfig.get().tiers();
      return tiers.get(Math.floorMod(this.selectedIndex(), tiers.size()));
   }

   public void clicked(int slot, int button, ClickType type, Player player) {
      if (!this.busy()) {
         if (this.admin) {
            if (this.adminView() != 2) {
               return;
            }

            if (!player.level().isClientSide && (!player.hasPermissions(2) || !this.stillValid(player))) {
               return;
            }

            if (slot >= 0 && slot < 37) {
               if (slot < 36 && player instanceof ServerPlayer server && type == ClickType.PICKUP) {
                  if (!this.getCarried().isEmpty()) {
                     this.addInventoryItem(server, this.getCarried());
                  } else {
                     this.data.set(10, this.vault.getItem(slot).isEmpty() ? 0 : slot + 1);
                  }

                  this.broadcastChanges();
               }

               return;
            }
         }

         super.clicked(slot, button, type, player);
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (this.busy() || index < 0 || index >= this.slots.size()) {
         return ItemStack.EMPTY;
      } else if (this.admin) {
         if (this.adminView() == 2 && index >= 37 && player instanceof ServerPlayer server && server.hasPermissions(2)) {
            this.addInventoryItem(server, ((Slot)this.slots.get(index)).getItem());
         }

         return ItemStack.EMPTY;
      } else {
         Slot slot = (Slot)this.slots.get(index);
         if (!slot.hasItem()) {
            return ItemStack.EMPTY;
         } else {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index < 7) {
               if (!this.moveItemStackTo(stack, 7, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (!(stack.getItem() instanceof UnidentifiedWeaponItem) || !this.moveItemStackTo(stack, 0, 7, false)) {
               return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
               slot.setByPlayer(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }

            return original;
         }
      }
   }

   public boolean clickMenuButton(Player player, int id) {
      if (WorkshopNavigation.handles(this, player, id)) {
         return true;
      } else {
         if (!(player instanceof ServerPlayer server) || this.busy() || !this.stillValid(player)) {
            return false;
         }

         if (!this.admin) {
            if (id == 0) {
               this.startBatch(server);
            }

            return id == 0;
         } else if (!server.hasPermissions(2)) {
            return false;
         } else if (this.seenSettings != TnkConfig.get()) {
            this.refreshVault();
            this.broadcastChanges();
            return false;
         } else if (id >= 100 && id < 100 + TnkConfig.Category.values().length) {
            this.data.set(9, id - 100 + 1);
            this.data.set(14, 0);
            this.enterView(server, 1);
            return true;
         } else if (id >= 1000 && id < 1000 + TnkConfig.get().tiers().size() && this.adminView() == 1) {
            this.data.set(0, id - 1000);
            this.enterView(server, 2);
            return true;
         } else if (id == 30) {
            this.enterView(server, this.adminView() == 2 ? 1 : 0);
            return true;
         } else if (id == 31) {
            this.enterView(server, 0);
            return true;
         } else if (this.adminView() == 1 && (id == 32 || id == 33)) {
            this.data.set(14, Math.clamp((long)(this.tierPage() + (id == 32 ? -1 : 1)), 0, Math.max(0, (TnkConfig.get().tiers().size() - 1) / 12)));
            return true;
         } else if (this.adminView() == 2 && this.filterCategory() != null) {
            switch (id) {
               case 1:
                  this.data.set(0, Math.floorMod(this.selectedIndex() - 1, TnkConfig.get().tiers().size()));
                  this.resetVaultPage();
                  break;
               case 2:
                  this.data.set(0, (this.selectedIndex() + 1) % TnkConfig.get().tiers().size());
                  this.resetVaultPage();
                  break;
               case 3:
                  this.cycleRarity(server);
                  break;
               case 4:
               case 5:
               case 7:
               case 8:
               case 9:
               case 12:
               case 13:
               case 14:
               case 15:
               case 16:
               case 17:
               case 18:
               case 19:
               default:
                  return false;
               case 6:
                  this.addTier(server);
                  this.resetVaultPage();
                  break;
               case 10:
                  this.data.set(7, Math.max(0, this.page() - 1));
                  this.data.set(10, 0);
                  break;
               case 11:
                  this.data.set(7, Math.min(this.pageCount() - 1, this.page() + 1));
                  this.data.set(10, 0);
                  break;
               case 20: {
                  TnkConfig.Weapon w = this.selectedEntry();
                  if (w != null) {
                     TnkConfig.enableWeapon(w.id(), this.selectedTier().rank(), !w.enabled());
                  }
                  break;
               }
               case 21: {
                  TnkConfig.Weapon w = this.selectedEntry();
                  if (w != null) {
                     TnkConfig.removeWeaponFromTier(w.id(), w.category(), this.selectedTier().rank());
                     this.data.set(10, 0);
                  }
                  break;
               }
               case 22: {
                  TnkConfig.Weapon w = this.selectedEntry();
                  TnkConfig.equalChances(this.selectedTier().rank(), w != null ? w.category() : this.filterCategory());
               }
            }

            this.refreshVault();
            return true;
         } else {
            return false;
         }
      }
   }

   private void quote(ServerPlayer var1) {
      FeatureHooks.quote(this, var1);
   }

   private void startBatch(ServerPlayer player) {
      this.quote(player);
      if (!this.affordable()) {
         player.sendSystemMessage(Component.translatable("message.tnkloot.batch_unavailable"));
      } else {
         ItemStack[] prepared = new ItemStack[7];

         try {
            for (int i = 0; i < 7; i++) {
               ItemStack token = this.input.getItem(i);
               if (token.getItem() instanceof UnidentifiedWeaponItem item) {
                  if (token.getCount() != 1) {
                     throw new IllegalStateException("Only one token per slot is allowed");
                  }

                  int rank = UnidentifiedWeaponItem.tier(token);
                  ItemStack base = UnidentifiedWeaponItem.baseResult(player.level(), token);
                  if (base.isEmpty()) {
                     throw new IllegalStateException("Empty item pool for tier " + rank);
                  }

                  prepared[i] = ApotheosisCompat.apply(base, rank, player, UnidentifiedWeaponItem.data(token).getLong("roll_seed"));
                  List<ItemStack> options = TnkConfig.tierWeapons(rank)
                     .stream()
                     .filter(
                        w -> w.category() == item.category()
                              && rank >= w.minTier()
                              && rank <= w.maxTier()
                              && WeaponSelector.available(w)
                              && !BossRewards.exclusive(w.id())
                     )
                     .map(w -> (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(w.id())))
                     .distinct()
                     .<ItemStack>map(ItemStack::new)
                     .toList();
                  if (DungeonEquipment.sealed(token)) {
                     options = List.of(DungeonEquipment.hint(player.level(), token));
                  } else if (item.boss()) {
                     options = BossRewards.find(UnidentifiedWeaponItem.data(token).getString("boss_pool"))
                        .map(p -> BossRewards.options(p, item.category()))
                        .orElse(List.of());
                  }

                  if (options.isEmpty()) {
                     throw new IllegalStateException("No preview options for " + item.category());
                  }

                  DimensionLoot.setPreview(this.previews, i, options, this, player);
               } else {
                  prepared[i] = ItemStack.EMPTY;
               }
            }
         } catch (RuntimeException var9) {
            TnkLoot.LOG.error("Identification batch rejected before payment", var9);
            player.sendSystemMessage(Component.translatable("message.tnkloot.affix_failed"));
            return;
         }

         if (this.quotedCost() <= 0 || ExperienceCosts.pay(player, this.currency(), this.quotedCost())) {
            for (int ix = 0; ix < 7; ix++) {
               this.results[ix] = prepared[ix];
            }

            this.data.set(3, 1);
            this.rollingSlot = -1;
            this.frame = 0;
            FeatureHooks.committed(this, player);
            this.nextRoll();
            player.getInventory().setChanged();
         }
      }
   }

   private void nextRoll() {
      this.rollingSlot++;

      while (this.rollingSlot < 7 && this.results[this.rollingSlot].isEmpty()) {
         this.rollingSlot++;
      }

      this.frame = 0;
      this.data.set(0, this.rollingSlot >= 7 ? 0 : this.rollingSlot + 1);
      if (this.rollingSlot >= 7) {
         this.data.set(3, 0);
         Arrays.fill(this.results, ItemStack.EMPTY);
      }
   }

   private void animate(ServerPlayer player) {
      List<ItemStack> options = this.previews.get(this.rollingSlot);
      int fastFrames = Math.max(12, options.size());
      int fastTicks = fastFrames * 2;
      int duration = fastTicks + 18;
      if (this.frame < fastTicks && this.frame % 2 == 0 || this.frame >= fastTicks && (this.frame - fastTicks) % 6 == 0) {
         int idx = this.frame < fastTicks ? this.frame / 2 : fastFrames + (this.frame - fastTicks) / 6;
         FeatureHooks.setReveal(this.input, this.rollingSlot, options.get(idx % options.size()).copy(), player);
         player.playNotifySound((SoundEvent)SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.35F, this.frame < fastTicks ? 1.3F : 0.8F);
      }

      if (++this.frame >= duration) {
         FeatureHooks.setReveal(this.input, this.rollingSlot, this.results[this.rollingSlot], player);
         player.playNotifySound((SoundEvent)SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.6F, 1.2F + (float)this.rollingSlot * 0.08F);
         this.nextRoll();
      }
   }

   public void broadcastChanges() {
      NativeDisplays.identifier(this);
      if (this.admin && this.owner instanceof ServerPlayer && this.seenSettings != TnkConfig.get()) {
         this.refreshVault();
      }

      if (!this.admin && !this.closing && this.owner instanceof ServerPlayer server) {
         long now = server.level().getGameTime();
         if (this.lastTick != now) {
            this.lastTick = now;
            if (this.busy()) {
               this.animate(server);
            } else {
               this.quote(server);
            }
         }
      }

      super.broadcastChanges();
   }

   public void removed(Player player) {
      this.closing = true;
      if (!player.level().isClientSide && !this.admin) {
         if (this.busy()) {
            for (int i = 0; i < 7; i++) {
               if (!this.results[i].isEmpty()) {
                  this.input.setItem(i, this.results[i]);
               }
            }
         }

         this.clearContainer(player, this.input);
         Arrays.fill(this.results, ItemStack.EMPTY);
         this.data.set(3, 0);
      }

      super.removed(player);
   }

   private void cycleRarity(ServerPlayer var1) {
      RaritySupport.explain(var1, this.selectedTier().rank());
   }

   private void enterView(ServerPlayer player, int view) {
      if (!this.getCarried().isEmpty()) {
         player.getInventory().placeItemBackInInventory(this.getCarried());
         this.setCarried(ItemStack.EMPTY);
      }

      this.data.set(12, view);
      this.resetVaultPage();
      this.refreshVault();
      this.broadcastChanges();
   }

   public int adminView() {
      return this.data.get(12);
   }

   public int tierPage() {
      return this.data.get(14);
   }

   public List<TnkConfig.Tier> navigationTiers() {
      String json = this.header().getString("tiers");
      return json.isEmpty() ? this.displayTiers : TnkConfig.parseTierSnapshot(json);
   }

   public int navigationPoolCount(int rank) {
      return this.header().getCompound("counts").getInt(Integer.toString(rank));
   }

   private void resetVaultPage() {
      this.data.set(7, 0);
      this.data.set(10, 0);
   }

   public int page() {
      return this.data.get(7);
   }

   public int pageCount() {
      return Math.max(1, this.data.get(8));
   }

   public int filterIndex() {
      return this.data.get(9);
   }

   public TnkConfig.Category filterCategory() {
      return this.filterIndex() == 0 ? null : TnkConfig.Category.values()[this.filterIndex() - 1];
   }

   public int selectedVaultSlot() {
      return this.data.get(10) - 1;
   }

   public int totalEntries() {
      return this.data.get(11);
   }

   public CompoundTag header() {
      return metadata(this.vault.getItem(36));
   }

   public CompoundTag selectedMetadata() {
      int slot = this.selectedVaultSlot();
      return slot >= 0 && slot < 36 ? metadata(this.vault.getItem(slot)) : new CompoundTag();
   }

   public static CompoundTag metadata(ItemStack stack) {
      return ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
   }

   private TnkConfig.Weapon selectedEntry() {
      int n = this.page() * 36 + this.selectedVaultSlot();
      return this.selectedVaultSlot() >= 0 && n >= 0 && n < this.vaultEntries.size() ? this.vaultEntries.get(n) : null;
   }

   public void setSelectedChance(ServerPlayer player, int rank, String item, int basisPoints) {
      if (this.admin && this.adminView() == 2 && player.hasPermissions(2) && this.stillValid(player) && this.selectedTier().rank() == rank) {
         TnkConfig.Weapon selected = this.selectedEntry();
         if (selected != null && selected.id().equals(item)) {
            boolean ok = TnkConfig.setChance(
               item,
               rank,
               basisPoints,
               w -> BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(w.id()))
                     && BuiltInRegistries.ITEM.get(ResourceLocation.parse(w.id())) != Items.AIR
            );
            if (!ok) {
               player.sendSystemMessage(Component.literal("Could not set chance. A partial percentage needs another enabled item of the same category."));
            }

            this.refreshVault();
            this.broadcastChanges();
         }
      }
   }

   private void addInventoryItem(ServerPlayer player, ItemStack stack) {
      if (this.adminView() == 2 && !stack.isEmpty() && !(stack.getItem() instanceof UnidentifiedWeaponItem)) {
         String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         if (BossRewards.exclusive(id)) {
            player.sendSystemMessage(Component.literal("This is boss-exclusive equipment. Configure it in tnkloot/boss-rewards.json."));
         } else {
            TnkConfig.Category category = TnkConfig.get()
               .weapons()
               .stream()
               .filter(w -> w.id().equals(id))
               .map(TnkConfig.Weapon::category)
               .findFirst()
               .orElseGet(() -> this.filterCategory() == null ? inferCategory(stack) : this.filterCategory());
            if (!TnkConfig.addWeaponToTier(id, category, this.selectedTier().rank())) {
               player.sendSystemMessage(Component.literal("Could not save this item to the tier."));
            } else {
               if (this.filterCategory() != null && this.filterCategory() != category) {
                  this.data.set(9, category.ordinal() + 1);
               }

               this.refreshVault();

               for (int i = 0; i < this.vaultEntries.size(); i++) {
                  if (this.vaultEntries.get(i).id().equals(id)) {
                     this.data.set(7, i / 36);
                     this.data.set(10, i % 36 + 1);
                     break;
                  }
               }

               this.refreshVault();
               this.broadcastChanges();
            }
         }
      }
   }

   private void refreshVault() {
      TnkConfig.Weapon prior = this.selectedEntry();
      int priorRank = this.header().getInt("rank");
      this.seenSettings = TnkConfig.get();
      TnkConfig.Tier tier = this.selectedTier();
      List<TnkConfig.Weapon> all = TnkConfig.tierWeapons(tier.rank()).stream().filter(w -> dev.mikaaah.tnkloot.compat.PackItemPolicy.allowed(w.id()) && !BossRewards.exclusive(w.id())).toList();
      this.vaultEntries = all.stream()
         .filter(w -> this.adminView() == 2 && w.category() == this.filterCategory())
         .sorted(Comparator.<TnkConfig.Weapon>comparingInt(w -> w.category().ordinal()).thenComparing(TnkConfig.Weapon::id))
         .toList();
      if (prior != null) {
         int found = -1;
         if (priorRank == tier.rank()) {
            for (int i = 0; i < this.vaultEntries.size(); i++) {
               if (this.vaultEntries.get(i).id().equals(prior.id())) {
                  found = i;
                  break;
               }
            }
         }

         if (found < 0) {
            this.data.set(10, 0);
         } else {
            this.data.set(7, found / 36);
            this.data.set(10, found % 36 + 1);
         }
      }

      this.data.set(8, Math.max(1, (this.vaultEntries.size() + 36 - 1) / 36));
      this.data.set(7, Math.clamp((long)this.page(), 0, this.pageCount() - 1));
      this.data.set(11, this.vaultEntries.size());
      Map<TnkConfig.Category, Long> totals = new EnumMap<>(TnkConfig.Category.class);

      for (TnkConfig.Weapon w : all) {
         if (WeaponSelector.available(w)) {
            totals.merge(w.category(), (long)w.weight(), Long::sum);
         }
      }

      for (int slot = 0; slot < 36; slot++) {
         int index = this.page() * 36 + slot;
         if (index >= this.vaultEntries.size()) {
            this.vault.setItem(slot, ItemStack.EMPTY);
         } else {
            TnkConfig.Weapon wx = this.vaultEntries.get(index);
            Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(wx.id())).orElse(Items.BARRIER);
            boolean present = BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(wx.id())) && item != Items.AIR;
            if (item == Items.AIR) {
               item = Items.BARRIER;
            }

            ItemStack ghost = new ItemStack(item);
            double chance = WeaponSelector.available(wx) ? 100.0 * (double)wx.weight() / (double)totals.getOrDefault(wx.category(), 1L).longValue() : 0.0;
            CompoundTag tag = new CompoundTag();
            tag.putString("item", wx.id());
            tag.putString("category", wx.category().name());
            tag.putBoolean("enabled", wx.enabled());
            tag.putBoolean("present", present);
            tag.putInt("weight", wx.weight());
            tag.putString("chance", String.format(Locale.ROOT, "%.2f", chance));
            ghost.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            ghost.set(
               DataComponents.LORE,
               new ItemLore(
                  List.of(
                     Component.literal(wx.id()).withStyle(ChatFormatting.DARK_GRAY),
                     Component.literal(
                           wx.category().name().toLowerCase(Locale.ROOT) + " · " + (present ? (wx.enabled() ? "Enabled" : "Disabled") : "Missing mod item")
                        )
                        .withStyle(wx.enabled() && present ? ChatFormatting.GREEN : ChatFormatting.RED),
                     Component.literal(String.format(Locale.ROOT, "Chance: %.2f%% within this category", chance)).withStyle(ChatFormatting.GOLD),
                     Component.literal("Levels " + dev.mikaaah.tnkloot.compat.EquipmentGroups.levels(wx.minTier(), wx.maxTier()) + " · " + dev.mikaaah.tnkloot.compat.EquipmentGroups.subgroup(item, wx.category())).withStyle(ChatFormatting.AQUA),
                     Component.literal("Click to edit · Display copy").withStyle(ChatFormatting.GRAY)
                  )
               )
            );
            this.vault.setItem(slot, ghost);
         }
      }

      if (this.selectedVaultSlot() >= 0 && this.vault.getItem(this.selectedVaultSlot()).isEmpty()) {
         this.data.set(10, 0);
      }

      ItemStack header = new ItemStack(Items.PAPER);
      CompoundTag tag = new CompoundTag();
      tag.putString("tiers", TnkConfig.tierSnapshot());
      CompoundTag counts = new CompoundTag();

      for (TnkConfig.Tier t : TnkConfig.get().tiers()) {
         counts.putInt(Integer.toString(t.rank()), (int)TnkConfig.tierWeapons(t.rank()).stream().filter(wx -> wx.category() == this.filterCategory()).count());
      }

      tag.put("counts", counts);
      tag.putInt("rank", tier.rank());
      tag.putString("id", tier.id());
      tag.putString("name", tier.name());
      tag.putString("rarity", tier.apotheosisRarity().toString());
      header.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      this.vault.setItem(36, header);
      NativeDisplays.identifier(this);
   }

   private static TnkConfig.Category inferCategory(ItemStack stack) {
      Item item = stack.getItem();
      if (item instanceof BowItem || item instanceof CrossbowItem) {
         return TnkConfig.Category.BOW;
      } else if (item instanceof ArmorItem armor) {
         return switch (armor.getEquipmentSlot()) {
            case HEAD -> TnkConfig.Category.HELMET;
            case CHEST -> TnkConfig.Category.CHESTPLATE;
            case LEGS -> TnkConfig.Category.LEGGINGS;
            case FEET -> TnkConfig.Category.BOOTS;
            default -> TnkConfig.Category.ACCESSORY;
         };
      } else {
         return item instanceof DiggerItem ? TnkConfig.Category.TOOL : TnkConfig.Category.WEAPON;
      }
   }

   private void addTier(ServerPlayer player) {
      int rank = TnkConfig.get().highestTier() + 1;
      ResourceLocation rarity = this.selectedTier().apotheosisRarity();
      boolean ok = TnkConfig.addTier("tier_" + rank, "Tier " + rank, rarity);
      if (ok) {
         this.data.set(0, TnkConfig.get().tiers().size() - 1);
      }

      player.sendSystemMessage(Component.literal(ok ? "Created Tier " + rank + ". Rename and tune advancements in tiers.json." : "Could not save tier."));
   }

   private void report(ServerPlayer player) {
      TnkConfig.Tier t = this.selectedTier();
      player.sendSystemMessage(Component.literal("Editing " + t.name() + " (rank " + t.rank() + "), Apotheosis group " + t.apotheosisRarity()));
   }
}
