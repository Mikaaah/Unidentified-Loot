package dev.mikaaah.tnkloot.loot;
import net.minecraft.server.level.ServerPlayer;
/** Required beta dependency, isolated to avoid bundling another mod's classes. Fails closed. */
public final class TnkLevelBridge {
 private static int call(String name,ServerPlayer p){try{return (int)Class.forName("dev.mikaaah.tnkprogression.ProgressionAPI").getMethod(name,ServerPlayer.class).invoke(null,p);}catch(ReflectiveOperationException e){throw new IllegalStateException("TNK Progression API is required by this beta",e);}}
 public static int ceiling(ServerPlayer p){return call("ceiling",p);}
 public static int level(ServerPlayer p){return call("level",p);}
 public static int rarityCap(ServerPlayer p){return call("rarityCap",p);}
}
