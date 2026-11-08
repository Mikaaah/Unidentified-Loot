package dev.mikaaah.tnkloot.boss;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
public final class BossCommands {
 public static void register(RegisterCommandsEvent event){
  event.getDispatcher().register(Commands.literal("tnkloot").then(Commands.literal("boss").requires(s->s.hasPermission(2))
   .then(Commands.literal("editor").executes(ctx->{BossEditor.open(ctx.getSource().getPlayerOrException());return 1;}))
   .then(Commands.literal("add").then(Commands.argument("entity",net.minecraft.commands.arguments.ResourceLocationArgument.id())
    .suggests((ctx,b)->SharedSuggestionProvider.suggest(BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Object::toString),b))
    .executes(ctx->{try{String id=BossStore.add(net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx,"entity").toString());ctx.getSource().sendSuccess(()->Component.literal("Created disabled boss pool "+id+". Add equipment and enable it in /tnkloot editor."),false);return 1;}catch(IllegalArgumentException ex){ctx.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}})))));
 }
}
