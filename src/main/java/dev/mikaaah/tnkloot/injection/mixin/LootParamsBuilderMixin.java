package dev.mikaaah.tnkloot.injection.mixin;

import dev.mikaaah.tnkloot.injection.LootParamsSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootParams.Builder.class)
public abstract class LootParamsBuilderMixin {
    @Inject(method = "create", at = @At("RETURN"), require = 1)
    private void tnkloot$rememberRewardSource(LootContextParamSet type,
            CallbackInfoReturnable<LootParams> result) {
        ((LootParamsSource) result.getReturnValue()).tnkloot$setAdvancementReward(
                type == LootContextParamSets.ADVANCEMENT_REWARD);
        ((LootParamsSource) result.getReturnValue()).tnkloot$setChest(type == LootContextParamSets.CHEST);
    }
}
