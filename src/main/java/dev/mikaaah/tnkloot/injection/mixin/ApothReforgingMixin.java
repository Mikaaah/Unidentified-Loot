package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.apotheosis.UnifiedApotheosis;
import dev.mikaaah.tnkloot.dungeon.DungeonEquipment;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.reforging.ReforgingMenu;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ReforgingMenu.class,remap=false)
public abstract class ApothReforgingMixin {
    @Shadow @Final protected Player player;
    @Inject(method="getRarity",at=@At("RETURN"),cancellable=true,remap=false)
    private void tnkloot$milestoneCap(CallbackInfoReturnable<LootRarity> result) {
        if(result.getReturnValue()!=null && !UnifiedApotheosis.canReforge(player,result.getReturnValue())) result.setReturnValue(null);
        var item = ((AbstractContainerMenu)(Object)this).getSlot(0).getItem();
        if(result.getReturnValue()!=null && DungeonEquipment.authored(item)) {
            var saved = AffixHelper.getRarity(item);
            if(!saved.isBound() || saved.get()!=result.getReturnValue()) result.setReturnValue(null);
        }
    }
}
