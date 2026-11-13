package dev.mikaaah.tnkloot.injection.mixin;

import dev.mikaaah.tnkloot.compat.PackEquipmentRules;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fill missing native categories only for the explicitly approved equipment. */
@Mixin(value=LootCategory.class,remap=false)
public abstract class ApothApprovedCategoryMixin {
    @Inject(method="forItem",at=@At("RETURN"),cancellable=true,remap=false)
    private static void tnkloot$approvedCategory(ItemStack stack,CallbackInfoReturnable<LootCategory> callback) {
        if(!callback.getReturnValue().isNone()||stack.isEmpty())return;
        var category=PackEquipmentRules.category(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if(category==null)return;
        Item example=switch(category) {
            case WEAPON -> Items.IRON_SWORD; case BOW -> Items.BOW; case TOOL -> Items.IRON_PICKAXE;
            case HELMET -> Items.IRON_HELMET; case CHESTPLATE -> Items.IRON_CHESTPLATE;
            case LEGGINGS -> Items.IRON_LEGGINGS; case BOOTS -> Items.IRON_BOOTS; default -> Items.AIR;
        };
        if(example!=Items.AIR)callback.setReturnValue(LootCategory.forItem(new ItemStack(example)));
    }
}
