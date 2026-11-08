package dev.mikaaah.tnkloot.progression;

import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

public final class ProgressionEvents {
   private ProgressionEvents() {
   }

   public static void onAdvancement(AdvancementEarnEvent event) {
      if (TnkConfig.get().mode() == TnkConfig.Mode.ADVANCEMENTS) {
         if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation var5 = event.getAdvancement().id();

            for (TnkConfig.Tier tier : TnkConfig.get().tiers()) {
               if (tier.advancements().contains(var5)) {
                  ProgressionService.unlock(player, tier.rank());
               }
            }
         }
      }
   }

   public static void onLogin(PlayerLoggedInEvent event) {
      if (TnkConfig.get().mode() == TnkConfig.Mode.ADVANCEMENTS) {
         if (event.getEntity() instanceof ServerPlayer player) {
            reconcile(player);
         }
      }
   }

   public static void reconcile(ServerPlayer player) {
      if (TnkConfig.get().mode() == TnkConfig.Mode.ADVANCEMENTS) {
         int highest = ProgressionService.getTier(player);

         for (TnkConfig.Tier tier : TnkConfig.get().tiers()) {
            for (ResourceLocation advancementId : tier.advancements()) {
               AdvancementHolder adv = player.getServer().getAdvancements().get(advancementId);
               if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone()) {
                  highest = Math.max(highest, tier.rank());
               }
            }
         }

         if (highest > ProgressionService.getTier(player)) {
            ProgressionService.unlock(player, highest);
         }
      }
   }
}
