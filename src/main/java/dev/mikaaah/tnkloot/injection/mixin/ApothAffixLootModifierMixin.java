package dev.mikaaah.tnkloot.injection.mixin;

import dev.mikaaah.tnkloot.apotheosis.NativeLootPools;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.modifiers.AffixLootModifier;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Default and explicitly named GLM pools share the same safe selector. */
@Mixin(value=AffixLootModifier.class,remap=false)
public abstract class ApothAffixLootModifierMixin {
    @Shadow @Final protected List<AffixLootModifier.AffixTableEntry> entries;

    @Inject(method="doApply(Lit/unimi/dsi/fastutil/objects/ObjectArrayList;Lnet/minecraft/world/level/storage/loot/LootContext;Ldev/shadowsoffire/apotheosis/tiers/GenContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",at=@At("HEAD"),cancellable=true,remap=false)
    private void tnkloot$eligiblePools(ObjectArrayList<ItemStack> loot,LootContext context,GenContext generation,CallbackInfoReturnable<ObjectArrayList<ItemStack>> result) {
        for(var entry:entries)if(entry.pattern().matches(context.getQueriedLootTableId())) {
            if(context.getRandom().nextFloat()<=entry.chance()) {
                var stack=NativeLootPools.fromPools(entry.rarities(),entry.entries(),generation);
                if(!stack.isEmpty()) { stack.set(Apoth.Components.FROM_CHEST,true);loot.add(stack); }
            }
            break;
        }
        result.setReturnValue(loot);
    }
}
