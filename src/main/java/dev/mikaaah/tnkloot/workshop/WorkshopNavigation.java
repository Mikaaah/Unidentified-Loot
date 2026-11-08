package dev.mikaaah.tnkloot.workshop;
import dev.mikaaah.tnkloot.menu.TnkMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
public final class WorkshopNavigation {
 private static final java.lang.reflect.Field STATION=field();
 private static java.lang.reflect.Field field(){try{var f=TnkMenu.class.getDeclaredField("station");f.setAccessible(true);return f;}catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}}
 public static boolean handles(TnkMenu menu,Player player,int id){
  if(id!=90&&id!=91&&id!=92)return false;
  if(menu.admin()||menu.busy()||!menu.stillValid(player)||!(player instanceof ServerPlayer server))return true;
  try{if(id==90)SalvageMenu.open(server,(BlockPos)STATION.get(menu));else if(id==92)CatalogMenu.open(server,false,(BlockPos)STATION.get(menu));else{server.closeContainer();LootJournal.open(server);}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  return true;
 }
}
