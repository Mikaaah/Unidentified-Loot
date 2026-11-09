package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.apotheosis.UnifiedApotheosis;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=AugmentingMenu.class,remap=false)
public abstract class ApothAugmentingMixin {
    @Inject(method="canAugment",at=@At("RETURN"),cancellable=true,remap=false)
    private static void tnkloot$upgradeLimit(AffixInstance instance,CallbackInfoReturnable<Boolean> result) {
        if(result.getReturnValue() && instance.level()>=UnifiedApotheosis.maximum(instance.stack())) result.setReturnValue(false);
    }
}
