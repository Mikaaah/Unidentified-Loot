package dev.mikaaah.tnkloot.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;

/** Replace a station screen directly, preserving the free cursor between station tabs. */
public record StationMenuProvider(MenuConstructor factory,Component title) implements MenuProvider {
 @Override public Component getDisplayName(){return title;}
 @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return factory.createMenu(id,inventory,player);}
 @Override public boolean shouldTriggerClientSideContainerClosingOnOpen(){return false;}
}
