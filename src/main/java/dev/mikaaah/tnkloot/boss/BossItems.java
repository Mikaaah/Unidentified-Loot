package dev.mikaaah.tnkloot.boss;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
public final class BossItems {
    public static final DeferredItem<UnidentifiedWeaponItem> DROP=TnkLoot.ITEMS.registerItem("unidentified_boss_drop",p->new UnidentifiedWeaponItem(p,TnkConfig.Category.WEAPON,true),new Item.Properties().stacksTo(1));
    public static void initialize(){dev.mikaaah.tnkloot.features.FeatureItems.initialize();}
}
