package dev.mikaaah.tnkloot.rarity;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RarityCommands {
   private RarityCommands() {
   }

   public static void register(RegisterCommandsEvent var0) {
      var0.getDispatcher()
         .register(
            (LiteralArgumentBuilder)Commands.literal("tnkloot")
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("rarity").requires(var0x -> var0x.hasPermission(2)))
                     .then(
                        Commands.argument("quality", StringArgumentType.word())
                           .suggests(
                              (var0x, var1) -> SharedSuggestionProvider.suggest(RarityPolicy.get().qualities().stream().map(RarityPolicy.Quality::id), var1)
                           )
                           .executes(var0x -> {
                              ServerPlayer var1 = ((CommandSourceStack)var0x.getSource()).getPlayerOrException();

                              try {
                                 RaritySupport.force(var1.getMainHandItem(), StringArgumentType.getString(var0x, "quality"));
                                 ((CommandSourceStack)var0x.getSource()).sendSuccess(() -> Component.literal("Held unidentified item's rarity updated"), false);
                                 return 1;
                              } catch (IllegalArgumentException var3) {
                                 ((CommandSourceStack)var0x.getSource()).sendFailure(Component.literal(var3.getMessage()));
                                 return 0;
                              }
                           })
                     )
               )
         );
   }
}
