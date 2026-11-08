package dev.mikaaah.tnkloot.menu;
import dev.mikaaah.tnkloot.workshop.SalvageMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;
public final class TnkMenus {
 public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"tnkloot");
 public static final DeferredHolder<MenuType<?>,MenuType<TnkMenu>> IDENTIFY=MENUS.register("identify",()->IMenuTypeExtension.create(TnkMenu.IDENTIFY_FACTORY));
 public static final DeferredHolder<MenuType<?>,MenuType<TnkMenu>> ADMIN=MENUS.register("tier_editor",()->IMenuTypeExtension.create(TnkMenu.ADMIN_FACTORY));
 public static final DeferredHolder<MenuType<?>,MenuType<SalvageMenu>> SALVAGE=MENUS.register("salvage",()->new MenuType<>(SalvageMenu::new,FeatureFlags.DEFAULT_FLAGS));
 public static final DeferredHolder<MenuType<?>,MenuType<dev.mikaaah.tnkloot.workshop.CatalogMenu>> PREVIEW=MENUS.register("reward_preview",()->new MenuType<>((id,inv)->new dev.mikaaah.tnkloot.workshop.CatalogMenu(id,inv,false),FeatureFlags.DEFAULT_FLAGS));
 public static final DeferredHolder<MenuType<?>,MenuType<dev.mikaaah.tnkloot.workshop.CatalogMenu>> COLLECTION=MENUS.register("collection_book",()->new MenuType<>((id,inv)->new dev.mikaaah.tnkloot.workshop.CatalogMenu(id,inv,true),FeatureFlags.DEFAULT_FLAGS));
 private TnkMenus(){}
}
