package dev.mikaaah.tnkloot.dungeon.mixin;
import dev.mikaaah.tnkloot.dungeon.SpiralCompat;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.pla.spiral_abyss.encounter.JournalPreviews", remap = false)
public abstract class SpiralJournalMixin {
    @Inject(method = "bosses", at = @At("RETURN"), cancellable = true)
    private static void tnkloot$dungeonJournal(ServerPlayer player, CallbackInfoReturnable<List<?>> callback) {
        callback.setReturnValue(SpiralCompat.journal(player, callback.getReturnValue()));
    }
}
