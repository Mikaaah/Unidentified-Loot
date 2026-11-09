package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.apotheosis.ApotheosisIntegration;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=WorldTier.class,remap=false)
public abstract class ApothWorldTierMixin {
    @ModifyVariable(method="setTier",at=@At("HEAD"),argsOnly=true,remap=false)
    private static WorldTier tnkloot$highestTier(WorldTier requested) { return ApotheosisIntegration.forcePinnacle()?WorldTier.PINNACLE:requested; }
    @Inject(method="setTier",at=@At("HEAD"),cancellable=true,remap=false)
    private static void tnkloot$fakePlayer(Player player,WorldTier tier,CallbackInfo callback) {
        if(player instanceof FakePlayer && ApotheosisIntegration.forcePinnacle()) { player.setData(Apoth.Attachments.WORLD_TIER,WorldTier.PINNACLE);callback.cancel(); }
    }
    @Inject(method="isUnlocked",at=@At("HEAD"),cancellable=true,remap=false)
    private static void tnkloot$noSecondProgressionGate(Player player,WorldTier tier,CallbackInfoReturnable<Boolean> result) {
        if(ApotheosisIntegration.forcePinnacle()) result.setReturnValue(true);
    }
}
