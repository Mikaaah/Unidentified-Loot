package dev.mikaaah.tnkloot.progression;

import dev.mikaaah.tnkloot.config.TnkConfig;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ProgressionService {
   private ProgressionService() {
   }

   public static String playerKey(ServerPlayer player) {
      return "player:" + player.getUUID();
   }

   public static int getTier(ServerPlayer player) {
      int level = Math.min(dev.mikaaah.tnkloot.loot.TnkLevelBridge.level(player), dev.mikaaah.tnkloot.loot.TnkLevelBridge.ceiling(player));
      return TnkConfig.get().tierForRank(dev.mikaaah.tnkloot.loot.LevelTierRoll.roll(level, 99)).rank();
   }

   public static boolean unlock(ServerPlayer player, int rank) {
      int boundedRank = TnkConfig.get().tierForRank(rank).rank();
      ProgressionData data = ProgressionData.of(player.getServer());
      boolean advanced = data.unlock(playerKey(player), boundedRank);
      Optional<String> team = TeamResolver.teamKey(player);
      if (team.isPresent()) {
         advanced |= data.unlock(team.get(), boundedRank);
      }

      if (advanced) {
         String name = TnkConfig.get().tierForRank(boundedRank).name();
         player.sendSystemMessage(Component.translatable("message.tnkloot.unlocked", new Object[]{boundedRank, name}));
      }

      return advanced;
   }

   public static void reset(ServerPlayer player) {
      ProgressionData data = ProgressionData.of(player.getServer());
      data.set(playerKey(player), 0);
      TeamResolver.teamKey(player).ifPresent(k -> data.set(k, 0));
   }
}
