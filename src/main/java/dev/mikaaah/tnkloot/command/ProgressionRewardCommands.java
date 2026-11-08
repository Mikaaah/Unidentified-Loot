package dev.mikaaah.tnkloot.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.ProgressionRewards;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ProgressionRewardCommands {
   private ProgressionRewardCommands() {
   }

   public static void register(RegisterCommandsEvent event) {
      LiteralArgumentBuilder<CommandSourceStack> reward = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("reward")
         .requires(s -> s.hasPermission(2));
      RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targets = Commands.argument("players", EntityArgument.players());
      targets.then(
         Commands.literal("equipment")
            .then(
               Commands.argument("category", StringArgumentType.word())
                  .suggests(
                     (ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(TnkConfig.Category.values()).map(c -> c.name().toLowerCase(Locale.ROOT)), b)
                  )
                  .executes(
                     ctx -> {
                        TnkConfig.Category category;
                        try {
                           category = TnkConfig.Category.parse(StringArgumentType.getString(ctx, "category"));
                        } catch (IllegalArgumentException var6) {
                           ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Unknown equipment category"));
                           return 0;
                        }

                        int count = 0;

                        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                           ItemStack token = ProgressionRewards.equipment(player, category);
                           if (token.isEmpty()) {
                              ((CommandSourceStack)ctx.getSource())
                                 .sendFailure(
                                    Component.literal("No available " + category.name().toLowerCase(Locale.ROOT) + " pool for " + player.getScoreboardName())
                                 );
                           } else {
                              if (!player.getInventory().add(token)) {
                                 player.drop(token, false);
                              }

                              count++;
                           }
                        }

                        return count;
                     }
                  )
            )
      );
      targets.then(
         Commands.literal("boss")
            .then(
               Commands.argument("pool", StringArgumentType.greedyString())
                  .suggests((ctx, b) -> SharedSuggestionProvider.suggest(BossRewards.active().stream().map(BossRewards.Pool::id), b))
                  .executes(ctx -> {
                     Optional<BossRewards.Pool> pool = BossRewards.find(StringArgumentType.getString(ctx, "pool"));
                     if (pool.isEmpty()) {
                        ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Unknown or disabled boss pool"));
                        return 0;
                     } else {
                        int count = 0;

                        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                           ItemStack token = ProgressionRewards.boss(player, pool.get());
                           if (!token.isEmpty()) {
                              if (!player.getInventory().add(token)) {
                                 player.drop(token, false);
                              }

                              count++;
                           }
                        }

                        return count;
                     }
                  })
            )
      );
      event.getDispatcher().register((LiteralArgumentBuilder)Commands.literal("tnkloot").then(reward.then(targets)));
   }
}
