package dev.mikaaah.tnkloot.menu;
import dev.mikaaah.tnkloot.boss.BossEditor;
import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
public final class TnkMenuProvider {
 public static void openIdentifier(ServerPlayer player,BlockPos station){
  player.openMenu(new dev.mikaaah.tnkloot.menu.StationMenuProvider((id,inv,p)->{var menu=new TnkMenu(TnkMenus.IDENTIFY.get(),id,inv,false);return station==null?menu:menu.atStation(station);},Component.translatable("menu.tnkloot.identifier")),buf->buf.writeUtf(TnkConfig.tierSnapshot()));
 }
 public static void openEditor(ServerPlayer player){BossEditor.open(player);}
 private TnkMenuProvider(){}
}
