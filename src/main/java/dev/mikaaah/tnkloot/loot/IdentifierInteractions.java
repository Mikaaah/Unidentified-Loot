package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.menu.TnkMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Post;

public final class IdentifierInteractions {
   private static final String PREFIX = "tnkloot.identifier.";

   private IdentifierInteractions() {
   }

   public static boolean isIdentifier(Entity entity) {
      return entity instanceof Villager && entity.getTags().stream().anyMatch(t -> t.startsWith("tnkloot.identifier."));
   }

   public static void onEntityInteract(EntityInteract event) {
      if (isIdentifier(event.getTarget())) {
         event.setCancellationResult(InteractionResult.SUCCESS);
         event.setCanceled(true);
         if (event.getEntity() instanceof ServerPlayer player && TnkConfig.get().stationIdentification()) {
            BlockPos anchor = anchor(event.getTarget());
            if (anchor != null && player.level().getBlockState(anchor).getBlock() instanceof IdentifierBlock) {
               TnkMenuProvider.openIdentifier(player, anchor);
            }
         }
      }
   }

   private static BlockPos anchor(Entity entity) {
      String tag = entity.getTags().stream().filter(t -> t.startsWith("tnkloot.identifier.")).findFirst().orElse("");

      try {
         String[] coordinates = tag.substring("tnkloot.identifier.".length()).split("\\.");
         return new BlockPos(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
      } catch (IndexOutOfBoundsException | IllegalArgumentException var3) {
         return null;
      }
   }

   public static void onAttack(AttackEntityEvent event) {
      if (isIdentifier(event.getTarget())) {
         event.setCanceled(true);
      }
   }

   public static void onDamage(LivingIncomingDamageEvent event) {
      if (isIdentifier(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   public static void onDeath(LivingDeathEvent event) {
      if (isIdentifier(event.getEntity())) {
         event.setCanceled(true);
         event.getEntity().setHealth(event.getEntity().getMaxHealth());
      }
   }

   public static void onTick(Post event) {
      if (event.getEntity() instanceof Villager v && v.level() instanceof ServerLevel level && isIdentifier(v)) {
         BlockPos pos = anchor(v);
         if (pos != null && level.hasChunkAt(pos)) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof IdentifierBlock)) {
               IdentifierBlock.particles(level, v, false);
               v.discard();
               return;
            }

            if (!(v instanceof IdentifierEntity)) {
               IdentifierEntity replacement = (IdentifierEntity)TnkLoot.IDENTIFIER_ENTITY.get().create(level);
               if (replacement != null) {
                  v.getTags().forEach(replacement::addTag);
                  IdentifierBlock.position(replacement, pos, state);
                  level.addFreshEntity(replacement);
                  v.discard();
               }

               return;
            }

            IdentifierBlock.position(v, pos, state);
            return;
         }

         return;
      }
   }
}
