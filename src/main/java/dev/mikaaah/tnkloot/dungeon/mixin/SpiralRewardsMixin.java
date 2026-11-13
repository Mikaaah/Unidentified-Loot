package dev.mikaaah.tnkloot.dungeon.mixin;
import dev.mikaaah.tnkloot.dungeon.SpiralCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.pla.spiral_abyss.encounter.FloorRewards", remap = false)
public abstract class SpiralRewardsMixin {
    @Inject(method = "roll", at = @At("HEAD"), cancellable = true)
    private static void tnkloot$dungeonRoll(ServerLevel level, Vec3 position, @Coerce Object rewards, int tier, int players, CallbackInfoReturnable<Object> callback) {
        SpiralCompat.roll(level, position, rewards, tier, players).ifPresent(callback::setReturnValue);
    }
}
