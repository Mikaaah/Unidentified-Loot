package dev.mikaaah.tnkloot.progression;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

public final class TeamResolver {
   private static boolean warned;

   private TeamResolver() {
   }

   public static Optional<String> teamKey(ServerPlayer player) {
      if (TnkConfig.get().shareFtbTeams() && ModList.get().isLoaded("ftbteams")) {
         try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object api = apiClass.getMethod("api").invoke(null);
            Class<?> apiInterface = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            if (!(Boolean)apiInterface.getMethod("isManagerLoaded").invoke(api)) {
               return Optional.empty();
            } else {
               Object manager = apiInterface.getMethod("getManager").invoke(api);
               Method lookup = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager").getMethod("getTeamForPlayerID", UUID.class);
               if (lookup.invoke(manager, player.getUUID()) instanceof Optional<?> optional && !optional.isEmpty()) {
                  Object team = optional.get();
                  Object id = Class.forName("dev.ftb.mods.ftbteams.api.Team").getMethod("getId").invoke(team);
                  return Optional.of("team:" + id);
               }

               return Optional.empty();
            }
         } catch (RuntimeException | ReflectiveOperationException var10) {
            if (!warned) {
               warned = true;
               TnkLoot.LOG.warn("Optional FTB Teams API lookup unavailable; using personal tiers", var10);
            }

            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }
}
