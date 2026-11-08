package dev.mikaaah.tnkloot.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.loot.WeaponSelector;
import dev.mikaaah.tnkloot.menu.TnkMenuProvider;
import dev.mikaaah.tnkloot.progression.ProgressionEvents;
import dev.mikaaah.tnkloot.progression.ProgressionService;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TnkCommands {
   private TnkCommands() {
   }

   public static void register(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "tnkloot"
                              )
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("boss").requires(source -> source.hasPermission(2)))
                                    .then(
                                       Commands.argument("players", EntityArgument.players())
                                          .then(
                                             Commands.argument("pool", StringArgumentType.string())
                                                .suggests(
                                                   (ctx, builder) -> SharedSuggestionProvider.suggest(
                                                         BossRewards.active().stream().map(BossRewards.Pool::id), builder
                                                      )
                                                )
                                                .then(Commands.argument("category", StringArgumentType.word()).executes(ctx -> {
                                                   Optional<BossRewards.Pool> pool = BossRewards.find(StringArgumentType.getString(ctx, "pool"));
                                                   TnkConfig.Category category = parseCategory(StringArgumentType.getString(ctx, "category"));
                                                   if (!pool.isEmpty() && category != null && !BossRewards.options(pool.get(), category).isEmpty()) {
                                                      Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");

                                                      for (ServerPlayer player : players) {
                                                         ItemStack stack = BossRewards.token(pool.get(), category, player.level(), ItemStack.EMPTY);
                                                         if (!player.getInventory().add(stack)) {
                                                            player.drop(stack, false);
                                                         }
                                                      }

                                                      return players.size();
                                                   } else {
                                                      ((CommandSourceStack)ctx.getSource())
                                                         .sendFailure(Component.literal("No active boss reward for that pool/category"));
                                                      return 0;
                                                   }
                                                }))
                                          )
                                    )
                              ))
                           .then(((LiteralArgumentBuilder)Commands.literal("editor").requires(source -> source.hasPermission(2))).executes(ctx -> {
                              TnkMenuProvider.openEditor(((CommandSourceStack)ctx.getSource()).getPlayerOrException());
                              return 1;
                           })))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("tier")
                                       .executes(
                                          ctx -> {
                                             ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                                             int rank = ProgressionService.getTier(p);
                                             ((CommandSourceStack)ctx.getSource())
                                                .sendSuccess(
                                                   () -> Component.literal(
                                                         "Guild tier: "
                                                            + rank
                                                            + " ("
                                                            + TnkConfig.get().tierForRank(rank).name()
                                                            + "); progression mode "
                                                            + TnkConfig.get().mode()
                                                      ),
                                                   false
                                                );
                                             return rank + 1;
                                          }
                                       ))
                                    .then(Commands.literal("list").executes(ctx -> {
                                       for (TnkConfig.Tier tier : TnkConfig.get().tiers()) {
                                          ((CommandSourceStack)ctx.getSource())
                                             .sendSuccess(() -> Component.literal(tier.rank() + " / " + tier.id() + " — " + tier.name()), false);
                                       }

                                       return TnkConfig.get().tiers().size();
                                    })))
                                 .then(
                                    ((LiteralArgumentBuilder)Commands.literal("unlock").requires(source -> source.hasPermission(2)))
                                       .then(
                                          Commands.argument("players", EntityArgument.players())
                                             .then(
                                                Commands.argument("tier", StringArgumentType.word())
                                                   .executes(
                                                      ctx -> change(
                                                            (CommandSourceStack)ctx.getSource(),
                                                            EntityArgument.getPlayers(ctx, "players"),
                                                            StringArgumentType.getString(ctx, "tier")
                                                         )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("reset").requires(source -> source.hasPermission(2)))
                                    .then(Commands.argument("players", EntityArgument.players()).executes(ctx -> {
                                       Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                                       players.forEach(ProgressionService::reset);
                                       ((CommandSourceStack)ctx.getSource())
                                          .sendSuccess(() -> Component.literal("Reset Guild tier for " + players.size() + " target(s)"), true);
                                       return players.size();
                                    }))
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("reload").requires(source -> source.hasPermission(2)))
                           .executes(
                              ctx -> {
                                 boolean ok = RaritySupport.reloadAll() && BossRewards.reload();
                                 if (ok) {
                                    ((CommandSourceStack)ctx.getSource()).getServer().getPlayerList().getPlayers().forEach(ProgressionEvents::reconcile);
                                 }

                                 ((CommandSourceStack)ctx.getSource())
                                    .sendSuccess(() -> Component.literal(ok ? "T&K loot config reloaded" : "Invalid config; old settings preserved"), true);
                                 return ok ? 1 : 0;
                              }
                           )
                     ))
                  .then(((LiteralArgumentBuilder)Commands.literal("validate").requires(source -> source.hasPermission(2))).executes(ctx -> {
                     int problems = 0;

                     for (TnkConfig.Weapon weapon : TnkConfig.get().weapons()) {
                        if (weapon.enabled() && !WeaponSelector.available(weapon)) {
                           ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Unavailable item: " + weapon.id()));
                           problems++;
                        }
                     }

                     for (TnkConfig.Tier tier : TnkConfig.get().tiers()) {
                        if (WeaponSelector.pool(tier.rank()).isEmpty()) {
                           ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Empty live weapon pool: " + tier.id()));
                           problems++;
                        }
                     }

                     for (TnkConfig.Category category : TnkConfig.get().weapons().stream().map(TnkConfig.Weapon::category).distinct().toList()) {
                        for (TnkConfig.Tier tierx : TnkConfig.get().tiers()) {
                           if (WeaponSelector.pool(tierx.rank(), category).isEmpty()) {
                              ((CommandSourceStack)ctx.getSource())
                                 .sendFailure(Component.literal("Empty " + category.name().toLowerCase() + " pool: " + tierx.id()));
                              problems++;
                           }
                        }
                     }

                     int count = problems;
                     ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Validation: " + count + " issue(s)"), false);
                     return problems == 0 ? 1 : 0;
                  })))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("unidentified").requires(source -> source.hasPermission(2)))
                     .then(
                        ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("players", EntityArgument.players())
                                 .executes(
                                    ctx -> give((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), null, TnkConfig.Category.WEAPON)
                                 ))
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("tier", StringArgumentType.word())
                                       .executes(
                                          ctx -> give(
                                                (CommandSourceStack)ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "players"),
                                                StringArgumentType.getString(ctx, "tier"),
                                                TnkConfig.Category.WEAPON
                                             )
                                       ))
                                    .then(
                                       Commands.argument("category", StringArgumentType.word())
                                          .executes(
                                             ctx -> give(
                                                   (CommandSourceStack)ctx.getSource(),
                                                   EntityArgument.getPlayers(ctx, "players"),
                                                   StringArgumentType.getString(ctx, "tier"),
                                                   parseCategory(StringArgumentType.getString(ctx, "category"))
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.argument("category", StringArgumentType.word())
                                 .executes(
                                    ctx -> give(
                                          (CommandSourceStack)ctx.getSource(),
                                          EntityArgument.getPlayers(ctx, "players"),
                                          null,
                                          parseCategory(StringArgumentType.getString(ctx, "category"))
                                       )
                                 )
                           )
                     )
               )
         );
   }

   private static int parseTier(String token) {
      try {
         int rank = Integer.parseInt(token);
         if (TnkConfig.get().tiers().stream().anyMatch(t -> t.rank() == rank)) {
            return rank;
         }
      } catch (NumberFormatException var2) {
      }

      return TnkConfig.get().tierForName(token).map(TnkConfig.Tier::rank).orElse(-1);
   }

   private static int change(CommandSourceStack source, Collection<ServerPlayer> players, String tierToken) {
      int rank = parseTier(tierToken);
      if (rank < 0) {
         source.sendFailure(Component.literal("Unknown tier '" + tierToken + "'. Use /tnkloot tier list"));
         return 0;
      } else {
         int count = 0;

         for (ServerPlayer player : players) {
            if (ProgressionService.unlock(player, rank)) {
               count++;
            }
         }

         int changes = count;
         source.sendSuccess(() -> Component.literal("Unlocked tier " + rank + " for " + changes + " target(s)"), true);
         return count;
      }
   }

   private static int give(CommandSourceStack source, Collection<ServerPlayer> players, String tierToken, TnkConfig.Category category) {
      if (category == null) {
         source.sendFailure(Component.literal("Unknown category; use weapon, bow, helmet, chestplate, leggings, boots, tool or accessory"));
         return 0;
      } else {
         int rank = tierToken == null ? -1 : parseTier(tierToken);
         if (tierToken != null && rank < 0) {
            source.sendFailure(Component.literal("Unknown tier '" + tierToken + "'"));
            return 0;
         } else {
            for (ServerPlayer player : players) {
               int effective = tierToken == null ? ProgressionService.getTier(player) : rank;
               ItemStack stack = UnidentifiedWeaponItem.create(category, effective);
               if (!player.getInventory().add(stack)) {
                  player.drop(stack, false);
               }
            }

            source.sendSuccess(
               () -> Component.literal("Given unidentified " + category.name().toLowerCase() + " tokens to " + players.size() + " target(s)"), true
            );
            return players.size();
         }
      }
   }

   private static TnkConfig.Category parseCategory(String raw) {
      try {
         return TnkConfig.Category.parse(raw);
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }
}
