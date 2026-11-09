package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.apotheosis.UnifiedApotheosis;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.reload.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=DynamicHolder.class,remap=false)
@SuppressWarnings("rawtypes")
public abstract class ApothLegacyRarityMixin {
    @Shadow @Final protected DynamicRegistry registry;
    @Shadow @Final protected ResourceLocation id;
    @Shadow protected CodecProvider value;
    @Inject(method="bind",at=@At("HEAD"),cancellable=true,remap=false)
    private void tnkloot$legacyAlias(CallbackInfo callback) {
        if(registry==RarityRegistry.INSTANCE) {
            var mapped=UnifiedApotheosis.alias(id);
            if(!mapped.equals(id)) {
                var rarity=RarityRegistry.INSTANCE.getValue(mapped);
                if(rarity!=null) { value=rarity;callback.cancel(); }
            }
        }
    }
}
