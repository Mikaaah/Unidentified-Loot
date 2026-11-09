package dev.mikaaah.tnkloot.injection.mixin;

import dev.mikaaah.tnkloot.apotheosis.NativeLootPools;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=AffixLootRegistry.class,remap=false)
public abstract class ApothAffixLootRegistryMixin {
    @Inject(method="getRandomItem(Ldev/shadowsoffire/apotheosis/tiers/GenContext;)Ldev/shadowsoffire/apotheosis/loot/AffixLootEntry;",at=@At("HEAD"),cancellable=true,remap=false)
    private void tnkloot$randomPool(GenContext context,CallbackInfoReturnable<AffixLootEntry> result) {
        result.setReturnValue(NativeLootPools.randomEntry(context));
    }
}
