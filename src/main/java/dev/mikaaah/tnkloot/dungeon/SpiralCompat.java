package dev.mikaaah.tnkloot.dungeon;

import dev.mikaaah.tnkloot.TnkLoot;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Optional adapter for Spiral 2.0.5; no Spiral classes are linked when it is absent. */
public final class SpiralCompat {
    private static final String ROOT = "com.pla.spiral_abyss.";
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();
    private SpiralCompat() {}
    public static Object get(Object value, String name) { return invoke(value.getClass(), value, name); }
    public static Object call(Object value, String name, Object... arguments) { return invoke(value.getClass(), value, name, arguments); }
    public static Object statics(String name, String method, Object... arguments) {
        try { return invoke(Class.forName(ROOT + name), null, method, arguments); }
        catch (ClassNotFoundException error) { throw new IllegalStateException("Spiral compatibility class unavailable: " + name, error); }
    }
    private static Class<?> boxed(Class<?> type) { return type == int.class ? Integer.class : type == boolean.class ? Boolean.class : type == double.class ? Double.class : type; }
    private static boolean matches(Class<?>[] types, Object[] arguments) {
        if (types.length != arguments.length) return false;
        for (int i = 0; i < types.length; i++) if (arguments[i] != null && !boxed(types[i]).isInstance(arguments[i])) return false;
        return true;
    }
    private static Object invoke(Class<?> type, Object target, String name, Object... arguments) {
        String key = type.getName() + "/" + name + Arrays.toString(Arrays.stream(arguments).map(a -> a == null ? null : a.getClass()).toArray());
        try {
            Method method = METHODS.computeIfAbsent(key, ignored -> Arrays.stream(type.getMethods()).filter(m -> m.getName().equals(name) && matches(m.getParameterTypes(), arguments)).findFirst().orElseThrow(() -> new IllegalStateException("Spiral method not found: " + key)));
            return method.invoke(target, arguments);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Spiral method failed: " + key, error.getCause());
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Spiral method unavailable: " + key, error); }
    }
    private static Object construct(String name, Object... arguments) {
        try {
            var type = Class.forName(ROOT + name);
            var constructor = Arrays.stream(type.getConstructors()).filter(c -> matches(c.getParameterTypes(), arguments)).findFirst().orElseThrow();
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot construct Spiral reward " + name, error); }
    }
    @SuppressWarnings("unchecked") private static List<Object> entries(Object rewards) { return (List<Object>)get(rewards, "entries"); }
    public static Optional<DungeonData.Table> table(Object rewards) {
        DungeonData.Table found = null;
        for (Object entry : entries(rewards)) {
            if ((boolean)get(entry, "item")) continue;
            var table = DungeonData.table((ResourceLocation)get(entry, "id"));
            if (table.isEmpty()) continue;
            DungeonData.require(found == null || found.id().equals(table.get().id()), "Use one dungeon reward definition per Spiral floor");
            found = table.get();
        }
        return Optional.ofNullable(found);
    }
    public static Optional<Object> roll(ServerLevel level, Vec3 position, Object rewards, int chestTier, int players) {
        return table(rewards).map(table -> roll(table, level, rewards, chestTier, players));
    }
    private static Object roll(DungeonData.Table table, ServerLevel level, Object offered, int chestTier, int players) {
        DungeonData.require(chestTier >= 1 && chestTier <= 3 && players > 0, "Invalid Spiral payout roster/tier");
        Random random = new Random(level.getRandom().nextLong());
        List<ItemStack> items = new ArrayList<>(); Set<Integer> rareIndices = new HashSet<>();
        int rolls = Math.multiplyExact(chestTier, players);
        var normal = table.availableNormal(); DungeonData.require(!normal.isEmpty(), "Dungeon reward table has no available normal entries: " + table.id());
        for (int i = 0; i < rolls; i++) {
            addSplit(items, DungeonEquipment.reward(DungeonData.choose(normal, random), level, random));
            // Keep floor-defined independent item bonuses; the custom table owns weighted table choices.
            for (Object bonus : entries(offered)) if ((boolean)get(bonus, "item") && !(boolean)get(bonus, "rare") && random.nextDouble() < (double)get(bonus, "chance")) {
                int min = (int)get(bonus, "minCount"), max = (int)get(bonus, "maxCount");
                addSplit(items, nativeStack(level, bonus, min + random.nextInt(max - min + 1)));
            }
        }
        if (chestTier == 3) {
            List<DungeonData.Entry> rare = rareEntries(table, offered, level);
            double base = rareChance(table);
            if (!rare.isEmpty() && random.nextDouble() < table.rare().partyChance(base, players)) {
                ItemStack prize = DungeonEquipment.reward(DungeonData.choose(rare, random), level, random);
                DungeonData.require(!prize.isEmpty() && prize.getCount() == 1 && !DungeonEquipment.sealed(prize), "Golden rare prize must be one identified item");
                rareIndices.add(items.size()); items.add(prize);
            }
        }
        int experience = (int)Math.round((int)get(offered, "experience") * (players / 3.0));
        return construct("encounter.FloorRewards$Bundle", items, experience, rareIndices);
    }
    private static void addSplit(List<ItemStack> items, ItemStack stack) {
        DungeonData.require(!stack.isEmpty(), "Dungeon reward generated an empty stack");
        for (int remaining = stack.getCount(); remaining > 0;) {
            int count = Math.min(remaining, stack.getMaxStackSize()); items.add(stack.copyWithCount(count)); remaining -= count;
        }
    }
    private static ItemStack nativeStack(ServerLevel level, Object entry, int count) { return (ItemStack)statics("encounter.RewardItems", "stack", level.registryAccess(), entry, count); }
    private static double rareChance(DungeonData.Table table) { return table.rare().chance().orElseGet(() -> ((Number)statics("encounter.RewardPool", "rareChance")).doubleValue()); }
    private static List<DungeonData.Entry> rareEntries(DungeonData.Table table, Object rewards, ServerLevel level) {
        if (table.rare().entries().isPresent()) return table.rare().entries().get().stream().filter(DungeonData.Entry::available).toList();
        List<DungeonData.Entry> entries = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (Object entry : entries(rewards)) if ((boolean)get(entry, "item") && (boolean)get(entry, "rare")) {
            ItemStack stack = nativeStack(level, entry, 1); if (stack.isEmpty()) continue;
            DungeonEquipment.requireNativePayload(stack);
            var id = (ResourceLocation)get(entry, "id"); String variant = id + "/" + get(entry, "enchantments").hashCode();
            if (!seen.add(variant)) continue;
            var key = ResourceLocation.fromNamespaceAndPath(table.id().getNamespace(), table.id().getPath() + "/encountered/" + id.getNamespace() + "/" + id.getPath() + "/" + Integer.toHexString(variant.hashCode()));
            var reward = new DungeonData.Entry(key, stack, (int)get(entry, "weight"), 1, 1, table.rare().settings(), null, Optional.empty());
            if (reward.available()) entries.add(reward);
        }
        return List.copyOf(entries);
    }
    public static Optional<List<?>> previews(MinecraftServer server, Object base, Set<ResourceLocation> enemies) {
        var table = table(base); if (table.isEmpty()) return Optional.empty();
        Object linked = statics("encounter.FloorRewards", "withMobRewards", base, enemies, statics("encounter.data.MobCatalog", "rewards"));
        return Optional.of(previews(server.overworld(), table.get(), linked));
    }
    private static List<?> previews(ServerLevel level, DungeonData.Table table, Object linked) {
        List<Object> result = new ArrayList<>();
        var normal = table.availableNormal();
        for (var entry : normal) {
            double chance = DungeonData.probability(entry, normal);
            var icon = DungeonEquipment.preview(entry, level, List.of(Component.literal(DungeonData.percentage(chance) + " per normal roll"), Component.literal("Copper / iron / gold: 1 / 2 / 3 rolls per player")));
            result.add(preview(entry.id(), icon, false, chance));
        }
        for (Object bonus : entries(linked)) if ((boolean)get(bonus, "item") && !(boolean)get(bonus, "rare")) {
            var id = (ResourceLocation)get(bonus, "id"); ItemStack stack = nativeStack(level, bonus, 1); if (stack.isEmpty()) continue;
            var entry = new DungeonData.Entry(ResourceLocation.fromNamespaceAndPath(table.id().getNamespace(), table.id().getPath() + "/bonus/" + id.getNamespace() + "/" + id.getPath()), stack, 1,
                (int)get(bonus, "minCount"), (int)get(bonus, "maxCount"), table.settings(), null, Optional.empty());
            double chance = (double)get(bonus, "chance");
            result.add(preview(entry.id(), DungeonEquipment.preview(entry, level, List.of(Component.literal(DungeonData.percentage(chance) + " independent bonus chance per normal roll"))), false, chance));
        }
        var rare = rareEntries(table, linked, level); double baseChance = rareChance(table);
        for (var entry : rare) {
            double chance = baseChance * DungeonData.probability(entry, rare);
            var notes = List.of(Component.literal("Gold only · 9 stars · " + DungeonData.percentage(chance) + " solo"),
                Component.literal("Rare pool: " + DungeonData.percentage(baseChance) + (table.rare().perPlayer() ? " per player" : " per chest")), Component.literal("Max 1 prize per party · announced on pickup"));
            result.add(preview(entry.id(), DungeonEquipment.preview(entry, level, notes), true, chance));
        }
        DungeonData.require(result.size() <= 128, "Spiral supports at most 128 preview variants; reduce this floor's reward list");
        return List.copyOf(result);
    }
    private static Object preview(ResourceLocation id, ItemStack icon, boolean rare, double chance) {
        return construct("network.AbyssNetwork$RewardPreview", id, false, rare, List.of(), false, icon, chance);
    }
    /** The journal and floor page must describe the same custom rare pool. */
    public static List<?> journal(ServerPlayer player, List<?> original) {
        List<Object> result = new ArrayList<>();
        Object snapshot = statics("encounter.data.FloorCatalog", "snapshot");
        Map<?, ?> floors = (Map<?, ?>)get(snapshot, "floors");
        Map<Integer, List<?>> custom = new HashMap<>();
        for (Object boss : original) {
            int floor = (int)get(boss, "floor"); Object definition = floors.get(floor);
            Object base = definition == null ? null : get(definition, "rewards");
            if (base == null || table(base).isEmpty()) { result.add(boss); continue; }
            List<?> rare = custom.computeIfAbsent(floor, ignored -> {
                Set<ResourceLocation> enemies = new LinkedHashSet<>();
                Object parties = statics("world.AbyssParties", "get", player.server);
                List<?> waves = (List<?>)get(definition, "waves");
                for (int i = 0; i < waves.size(); i++) for (Object enemy : (List<?>)call(parties, "cycleLineup", floor, i + 1, waves.get(i))) enemies.add((ResourceLocation)get(enemy, "id"));
                return previews(player.server, base, enemies).orElseThrow().stream().filter(p -> (boolean)get(p, "rare")).toList();
            });
            result.add(construct("network.AbyssNetwork$BannerBoss", floor, get(boss, "wave"), get(boss, "id"), rare, get(boss, "animation")));
        }
        return List.copyOf(result);
    }
}
