package dev.mikaaah.tnkloot.menu;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.IContainerFactory;
import java.util.*;

/** Seven-slot counter. The server quotes, charges and commits an entire batch atomically. */
public final class TnkMenu extends AbstractContainerMenu {
    public static final IContainerFactory<TnkMenu> IDENTIFY_FACTORY = (id, inv, buf) -> new TnkMenu(TnkMenus.IDENTIFY.get(), id, inv, false, TnkConfig.parseTierSnapshot(buf.readUtf(32767)));
    public static final IContainerFactory<TnkMenu> ADMIN_FACTORY = (id, inv, buf) -> new TnkMenu(TnkMenus.ADMIN.get(), id, inv, true, TnkConfig.parseTierSnapshot(buf.readUtf(32767)));
    public static final int INPUTS = 7;
    private final boolean admin;
    // selected tier, cost low/high, busy, input count, currency item, affordability
    private final ContainerData data = new SimpleContainerData(7);
    private final List<TnkConfig.Tier> displayTiers;
    private final SimpleContainer input = new SimpleContainer(INPUTS);
    private final Player owner;
    private final Vec3 openingPosition;
    private final ItemStack[] results = new ItemStack[INPUTS];
    private final List<List<ItemStack>> previews = new ArrayList<>();
    private int rollingSlot = -1, frame;
    private long lastTick = Long.MIN_VALUE;
    private boolean closing;
    private net.minecraft.core.BlockPos station;
    public TnkMenu atStation(net.minecraft.core.BlockPos pos) { this.station = pos.immutable(); return this; }

    public TnkMenu(MenuType<?> type, int id, Inventory inventory, boolean admin) {
        this(type, id, inventory, admin, TnkConfig.get().tiers());
    }
    public TnkMenu(MenuType<?> type, int id, Inventory inventory, boolean admin, List<TnkConfig.Tier> displayTiers) {
        super(type, id);
        this.admin = admin; this.displayTiers = displayTiers;
        owner = inventory.player; openingPosition = owner.position();
        Arrays.fill(results, ItemStack.EMPTY);
        for (int i = 0; i < INPUTS; i++) previews.add(List.of());
        addDataSlots(data);
        if (!admin) {
            for (int i = 0; i < INPUTS; i++) addSlot(new Slot(input, i, 8 + i * 18, 30) {
                @Override public boolean mayPlace(ItemStack stack) { return !busy() && stack.getItem() instanceof UnidentifiedWeaponItem; }
                @Override public boolean mayPickup(Player p) { return !busy(); }
                @Override public int getMaxStackSize() { return 1; }
            });
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 74 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 132));
        }
    }
    @Override public boolean stillValid(Player player) {
        return admin || player.isAlive() && player.distanceToSqr(openingPosition) <= 64
            && TnkConfig.get().stationIdentification()
            && (station == null || player.level().getBlockState(station).getBlock() instanceof IdentifierBlock);
    }
    public boolean admin() { return admin; }
    public boolean busy() { return data.get(3) != 0; }
    public int quotedCost() { return (data.get(1) & 65535) | ((data.get(2) & 65535) << 16); }
    public int inputCount() { return data.get(4); }
    public boolean affordable() { return data.get(6) != 0; }
    public Item currency() { return Item.byId(data.get(5)); }
    public int selectedIndex() { return data.get(0); }
    public List<TnkConfig.Tier> displayTiers() { return displayTiers; }
    public TnkConfig.Tier selectedTier() {
        var tiers = TnkConfig.get().tiers();
        return tiers.get(Math.floorMod(selectedIndex(), tiers.size()));
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        // Includes number keys, drag, double-click, throw, swap and creative clone.
        if (!busy()) super.clicked(slot, button, type, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (admin || busy() || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < INPUTS) {
            if (!moveItemStackTo(stack, INPUTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!(stack.getItem() instanceof UnidentifiedWeaponItem) || !moveItemStackTo(stack, 0, INPUTS, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server) || busy() || !stillValid(player)) return false;
        if (!admin) {
            if (id == 0) startBatch(server);
            return id == 0;
        }
        if (!server.hasPermissions(2)) return false;
        switch (id) {
            case 1 -> { data.set(0, Math.floorMod(selectedIndex() - 1, TnkConfig.get().tiers().size())); report(server); }
            case 2 -> { data.set(0, (selectedIndex() + 1) % TnkConfig.get().tiers().size()); report(server); }
            case 3 -> cycleRarity(server);
            case 4 -> addHeld(server);
            case 5 -> removeHeld(server);
            case 6 -> addTier(server);
            default -> { return false; }
        }
        return true;
    }
    private void quote(ServerPlayer player) {
        long cost = 0; int count = 0; boolean valid = TnkConfig.get().enableIdentification();
        for (int i = 0; i < INPUTS; i++) {
            ItemStack token = input.getItem(i);
            if (!(token.getItem() instanceof UnidentifiedWeaponItem)) continue;
            count++;
            ItemStack base = UnidentifiedWeaponItem.baseResult(player.level(), token);
            if (base.isEmpty()) valid = false;
            else cost += UnidentifiedWeaponItem.cost(token, base);
        }
        int total = (int)Math.min(Integer.MAX_VALUE, cost);
        Item currency = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(TnkConfig.get().costs().item())).orElse(net.minecraft.world.item.Items.AIR);
        data.set(1, total & 65535); data.set(2, total >>> 16);
        data.set(4, count); data.set(5, Item.getId(currency));
        data.set(6, valid && cost <= Integer.MAX_VALUE && count > 0
            && (total == 0 || UnidentifiedWeaponItem.has(player, currency, total)) ? 1 : 0);
    }
    private void startBatch(ServerPlayer player) {
        quote(player);
        if (!affordable()) {
            player.sendSystemMessage(Component.translatable("message.tnkloot.batch_unavailable"));
            return;
        }
        // Build every result before charging or consuming anything.
        ItemStack[] prepared = new ItemStack[INPUTS];
        try {
            for (int i = 0; i < INPUTS; i++) {
                ItemStack token = input.getItem(i);
                if (!(token.getItem() instanceof UnidentifiedWeaponItem item)) { prepared[i] = ItemStack.EMPTY; continue; }
                if (token.getCount() != 1) throw new IllegalStateException("Only one token per slot is allowed");
                int rank = UnidentifiedWeaponItem.tier(token);
                ItemStack base = UnidentifiedWeaponItem.baseResult(player.level(), token);
                if (base.isEmpty()) throw new IllegalStateException("Empty item pool for tier " + rank);
                prepared[i] = ApotheosisCompat.applyToken(base, token, player);
                List<ItemStack> options = TnkConfig.get().weapons().stream()
                    .filter(w -> w.category() == item.category() && rank >= w.minTier() && rank <= w.maxTier() && WeaponSelector.available(w))
                    .map(w -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(w.id())))
                    .distinct().map(ItemStack::new).toList();
                previews.set(i, options);
            }
        } catch (RuntimeException ex) {
            TnkLoot.LOG.error("Identification batch rejected before payment", ex);
            player.sendSystemMessage(Component.translatable("message.tnkloot.affix_failed"));
            return;
        }
        if (quotedCost() > 0) UnidentifiedWeaponItem.remove(player, currency(), quotedCost());
        for (int i = 0; i < INPUTS; i++) results[i] = prepared[i];
        data.set(3, 1); rollingSlot = -1; frame = 0;
        nextRoll();
        player.getInventory().setChanged();
    }
    private void nextRoll() {
        rollingSlot++;
        while (rollingSlot < INPUTS && results[rollingSlot].isEmpty()) rollingSlot++;
        frame = 0;
        if (rollingSlot >= INPUTS) {
            data.set(3, 0);
            Arrays.fill(results, ItemStack.EMPTY);
        }
    }
    private void animate(ServerPlayer player) {
        List<ItemStack> options = previews.get(rollingSlot);
        // Show each available option at least once, then slow down before revealing.
        int fastFrames = Math.max(12, options.size());
        int fastTicks = fastFrames * 2, duration = fastTicks + 18;
        if (frame < fastTicks && frame % 2 == 0 || frame >= fastTicks && (frame - fastTicks) % 6 == 0) {
            int idx = frame < fastTicks ? frame / 2 : fastFrames + (frame - fastTicks) / 6;
            input.setItem(rollingSlot, options.get(idx % options.size()).copy());
            player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.35F, frame < fastTicks ? 1.3F : 0.8F);
        }
        if (++frame >= duration) {
            input.setItem(rollingSlot, results[rollingSlot]);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.6F, 1.2F + rollingSlot * 0.08F);
            nextRoll();
        }
    }
    @Override public void broadcastChanges() {
        if (!admin && !closing && owner instanceof ServerPlayer server) {
            long now = server.level().getGameTime();
            if (lastTick != now) {
                lastTick = now;
                if (busy()) animate(server); else quote(server);
            }
        }
        super.broadcastChanges();
    }
    @Override public void removed(Player player) {
        closing = true;
        if (!player.level().isClientSide && !admin) {
            // Paid results replace all temporary preview stacks before close/disconnect/death.
            if (busy()) for (int i = 0; i < INPUTS; i++)
                if (!results[i].isEmpty()) input.setItem(i, results[i]);
            clearContainer(player, input);
            Arrays.fill(results, ItemStack.EMPTY);
            data.set(3, 0);
        }
        super.removed(player);
    }
    private void cycleRarity(ServerPlayer player) {
        var t = selectedTier();
        var all = new java.util.ArrayList<>(java.util.List.of("common", "uncommon", "rare", "epic", "legendary", "mythic", "ancient")
            .stream().map(name -> ResourceLocation.fromNamespaceAndPath("tnkloot", name)).toList());
        TnkConfig.get().tiers().stream().map(TnkConfig.Tier::apotheosisRarity).filter(id -> !all.contains(id)).forEach(all::add);
        int next = (all.indexOf(t.apotheosisRarity()) + 1) % all.size();
        TnkConfig.editTier(t.rank(), null, all.get(next)); report(player);
    }
    private void addHeld(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { player.sendSystemMessage(Component.literal("Hold an item to add it to this tier.")); return; }
        var category = TnkConfig.get().weapons().stream().filter(w -> w.id().equals(BuiltInRegistries.ITEM.getKey(held.getItem()).toString()))
            .map(TnkConfig.Weapon::category).findFirst().orElseGet(() -> inferCategory(held));
        boolean ok = TnkConfig.addWeaponToTier(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), category, selectedTier().rank());
        player.sendSystemMessage(Component.literal(ok ? "Added item to tier " + selectedTier().id() : "Could not save the item pool."));
    }
    private static TnkConfig.Category inferCategory(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof net.minecraft.world.item.BowItem || item instanceof net.minecraft.world.item.CrossbowItem) return TnkConfig.Category.BOW;
        if (item instanceof net.minecraft.world.item.ArmorItem armor) return switch (armor.getEquipmentSlot()) {
            case HEAD -> TnkConfig.Category.HELMET; case CHEST -> TnkConfig.Category.CHESTPLATE;
            case LEGS -> TnkConfig.Category.LEGGINGS; case FEET -> TnkConfig.Category.BOOTS; default -> TnkConfig.Category.ACCESSORY;
        };
        if (item instanceof net.minecraft.world.item.DiggerItem) return TnkConfig.Category.TOOL;
        return TnkConfig.Category.WEAPON;
    }
    private void removeHeld(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { player.sendSystemMessage(Component.literal("Hold an item to remove it from this tier.")); return; }
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        boolean removed = false;
        for (var category : TnkConfig.Category.values()) if (TnkConfig.removeWeaponFromTier(id, category, selectedTier().rank())) removed = true;
        player.sendSystemMessage(Component.literal(removed ? "Removed item from this tier." : "That item has no tier-specific entry to remove."));
    }
    private void addTier(ServerPlayer player) {
        int rank = TnkConfig.get().highestTier() + 1;
        var rarity = selectedTier().apotheosisRarity();
        boolean ok = TnkConfig.addTier("tier_" + rank, "Tier " + rank, rarity);
        if (ok) data.set(0, TnkConfig.get().tiers().size() - 1);
        player.sendSystemMessage(Component.literal(ok ? "Created Tier " + rank + ". Rename and tune advancements in tiers.json." : "Could not save tier."));
    }
    private void report(ServerPlayer player) {
        var t = selectedTier(); player.sendSystemMessage(Component.literal("Editing " + t.name() + " (rank " + t.rank() + "), Apotheosis group " + t.apotheosisRarity()));
    }
}
