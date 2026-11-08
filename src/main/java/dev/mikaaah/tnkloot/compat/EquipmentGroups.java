package dev.mikaaah.tnkloot.compat;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.LevelTierRoll;
import java.util.Locale;
import net.minecraft.world.item.*;

/** Shared labels for the vault, collection and complete runtime export. */
public final class EquipmentGroups {
    private EquipmentGroups() {}

    public static String subgroup(Item item, TnkConfig.Category category) {
        if (category == TnkConfig.Category.ACCESSORY) return "Accessories";
        if (item instanceof ShieldItem) return "Shields";
        if (item instanceof FishingRodItem) return "Fishing";
        if (item instanceof HoeItem) return "Farming";
        if (item instanceof PickaxeItem || item instanceof ShovelItem) return "Mining";
        if (item instanceof AxeItem) return "Axes";
        if (category == TnkConfig.Category.BOW) return "Ranged weapons";
        if (category == TnkConfig.Category.WEAPON) {
            for (Class<?> type = item.getClass(); type != null; type = type.getSuperclass()) {
                String name = type.getSimpleName().toLowerCase(Locale.ROOT);
                if (name.contains("spellbook") || name.contains("staff") || name.contains("scepter") || name.contains("wand")) return "Spellcasting";
            }
            return "Melee weapons";
        }
        return category.name().charAt(0) + category.name().substring(1).toLowerCase(Locale.ROOT);
    }

    public static int firstLevel(int minimumTier) {
        for (int level = 1; level <= 100; level++) if (LevelTierRoll.roll(level, 99) >= minimumTier) return level;
        return 100;
    }
    public static int lastLevel(int maximumTier) {
        for (int level = 100; level >= 1; level--) if (LevelTierRoll.roll(level, 0) <= maximumTier) return level;
        return 1;
    }
    public static String levels(int minimumTier, int maximumTier) {
        return firstLevel(minimumTier) + "–" + lastLevel(maximumTier);
    }
    public static String levels(int tier) { return levels(tier, tier); }
}
