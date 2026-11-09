package dev.mikaaah.tnkloot.injection.mixin;

import dev.mikaaah.tnkloot.apotheosis.UnifiedApotheosis;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.util.AffixItemIngredient;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=AffixItemIngredient.class,remap=false)
public abstract class ApothAffixIngredientMixin {
    @Shadow @Final protected DynamicHolder<LootRarity> rarity;

    @Inject(method="test",at=@At("HEAD"),cancellable=true,remap=false)
    private void tnkloot$canonicalIngredient(ItemStack stack,CallbackInfoReturnable<Boolean> result) {
        var actual=AffixHelper.getRarity(stack);
        result.setReturnValue(actual.isBound()&&rarity.isBound()&&AffixHelper.hasAffixes(stack)
            &&UnifiedApotheosis.canonical(actual.get())==UnifiedApotheosis.canonical(rarity.get()));
    }
}
