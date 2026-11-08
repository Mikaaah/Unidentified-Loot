package dev.mikaaah.tnkloot.loot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Required companion API, resolved once rather than for every drop. */
public final class TnkLevelBridge {
    private TnkLevelBridge() {}
    private static final class Api {
        static final Class<?> TYPE = type();
        static final Method LEVEL = method("level", ServerPlayer.class);
        static final Method CEILING = method("ceiling", ServerPlayer.class);
        static final Method RARITY = method("rarityCap", ServerPlayer.class);
        static final Method MOB_LEVEL = method("mobLevel", LivingEntity.class);
        private static Class<?> type() {
            try { return Class.forName("dev.mikaaah.tnkprogression.ProgressionAPI"); }
            catch (ClassNotFoundException ex) { throw new IllegalStateException("TNK Progression is required", ex); }
        }
        private static Method method(String name, Class<?> parameter) {
            try { return TYPE.getMethod(name, parameter); }
            catch (NoSuchMethodException ex) { throw new IllegalStateException("Incompatible TNK Progression API: " + name, ex); }
        }
    }
    private static int call(Method method, Object entity) {
        try { return ((Number) method.invoke(null, entity)).intValue(); }
        catch (InvocationTargetException ex) { throw new IllegalStateException("TNK Progression failed: " + method.getName(), ex.getCause()); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException("Cannot call TNK Progression: " + method.getName(), ex); }
    }
    public static int level(ServerPlayer player) { return Math.clamp(call(Api.LEVEL, player), 1, 100); }
    public static int ceiling(ServerPlayer player) { return Math.clamp(call(Api.CEILING, player), 1, 100); }
    public static int rarityCap(ServerPlayer player) { return Math.clamp(call(Api.RARITY, player), 0, 5); }
    public static int mobLevel(LivingEntity entity) { return Math.max(1, call(Api.MOB_LEVEL, entity)); }
}
