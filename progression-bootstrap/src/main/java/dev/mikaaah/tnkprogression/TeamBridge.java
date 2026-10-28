package dev.mikaaah.tnkprogression;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
public final class TeamBridge {
 private static boolean warned;
 public static Optional<String> key(ServerPlayer p) {
  if(!ProgressionConfig.current.shareTeamUnlocks || !ModList.get().isLoaded("ftbteams"))return Optional.empty();
  try {
   Object api=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null);
   Class<?> type=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
   if(api==null || !(boolean)type.getMethod("isManagerLoaded").invoke(api))return Optional.empty();
   Object manager=type.getMethod("getManager").invoke(api);
   Optional<?> team=(Optional<?>)Class.forName("dev.ftb.mods.ftbteams.api.TeamManager").getMethod("getTeamForPlayerID",UUID.class).invoke(manager,p.getUUID());
   if(team.isEmpty())return Optional.empty();
   return Optional.of("team:"+Class.forName("dev.ftb.mods.ftbteams.api.Team").getMethod("getId").invoke(team.get()));
  }catch(ReflectiveOperationException|RuntimeException e){if(!warned){warned=true;TnkProgression.LOG.warn("FTB Teams lookup failed; personal progression remains active",e);}return Optional.empty();}
 }
}
