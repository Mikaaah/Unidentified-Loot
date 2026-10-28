package dev.mikaaah.tnkprogression;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.arguments.*;
public final class ProgressionCommands {
 public static void register(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("tnkprogress")
   .then(Commands.literal("status").executes(c->{var p=c.getSource().getPlayerOrException();var n=ProgressionAPI.profile(p);c.getSource().sendSuccess(()->Component.literal("Level "+ProgressionAPI.level(p)+" / ceiling "+ProgressionAPI.ceiling(p)+" | XP "+n.getLong("xp")+" / "+ProgressionMath.levelCost(ProgressionAPI.level(p))+" | points "+ProgressionAPI.level(p)),false);return 1;}))
   .then(Commands.literal("reload").requires(s->s.hasPermission(2)).executes(c->ProgressionConfig.reload()?1:0))
   .then(Commands.literal("xp").requires(s->s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("amount",LongArgumentType.longArg(1,1000000000L)).executes(c->{ProgressionAPI.awardXp(EntityArgument.getPlayer(c,"player"),LongArgumentType.getLong(c,"amount"));return 1;}))))
   .then(Commands.literal("reward").requires(s->s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("id",StringArgumentType.word()).then(Commands.argument("amount",LongArgumentType.longArg(1,1000000000L)).executes(c->ProgressionAPI.rewardOnce(EntityArgument.getPlayer(c,"player"),StringArgumentType.getString(c,"id"),LongArgumentType.getLong(c,"amount"))?1:0)))))
   .then(Commands.literal("unlock").requires(s->s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("ceiling",IntegerArgumentType.integer(1,100)).executes(c->{int cap=IntegerArgumentType.getInteger(c,"ceiling");if(java.util.Arrays.stream(ProgressionConfig.current.ceilings).noneMatch(v->v==cap)){c.getSource().sendFailure(Component.literal("Not a configured level ceiling"));return 0;}ProgressionAPI.unlock(EntityArgument.getPlayer(c,"player"),cap);return 1;})))));
 }
}
