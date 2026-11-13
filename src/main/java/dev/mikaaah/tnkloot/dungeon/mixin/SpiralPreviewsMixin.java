package dev.mikaaah.tnkloot.dungeon.mixin;
import dev.mikaaah.tnkloot.dungeon.SpiralCompat;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.pla.spiral_abyss.encounter.RewardPreviews", remap = false)
public abstract class SpiralPreviewsMixin {
    @Shadow @Final private MinecraftServer server;
    @Inject(method = "floor", at = @At("HEAD"), cancellable = true)
    private void tnkloot$dungeonPreview(@Coerce Object rewards, Set<ResourceLocation> enemies, CallbackInfoReturnable<List<?>> callback) {
        SpiralCompat.previews(server, rewards, enemies).ifPresent(callback::setReturnValue);
    }
}
