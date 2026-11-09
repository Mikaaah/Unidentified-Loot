package dev.mikaaah.tnkloot.injection.mixin;

import dev.mikaaah.tnkloot.injection.LootParamsSource;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LootParams.class)
public abstract class LootParamsMixin implements LootParamsSource {
    @Unique private boolean tnkloot$advancementReward;
    @Unique private boolean tnkloot$chest;

    @Override public boolean tnkloot$isAdvancementReward() {
        return tnkloot$advancementReward;
    }

    @Override public void tnkloot$setAdvancementReward(boolean value) {
        tnkloot$advancementReward = value;
    }

    @Override public boolean tnkloot$isChest() { return tnkloot$chest; }
    @Override public void tnkloot$setChest(boolean value) { tnkloot$chest = value; }
}
