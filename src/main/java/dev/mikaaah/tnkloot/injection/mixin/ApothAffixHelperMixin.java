package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.apotheosis.UnifiedApotheosis;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AffixHelper.class,remap=false)
public abstract class ApothAffixHelperMixin {
    @Inject(method="setAffixes",at=@At("HEAD"),cancellable=true,remap=false)
    private static void tnkloot$boundAffixes(ItemStack stack,ItemAffixes affixes,CallbackInfo callback) {
        if(UnidentifiedWeaponItem.data(stack).contains("tnk_rarity")) {
            stack.set(Apoth.Components.AFFIXES,UnifiedApotheosis.bounded(stack,affixes));callback.cancel();
        }
    }
}
