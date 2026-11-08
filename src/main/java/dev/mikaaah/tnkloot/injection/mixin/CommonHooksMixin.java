package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=CommonHooks.class,remap=false)
public abstract class CommonHooksMixin {
 @Inject(method="modifyLoot(Lnet/minecraft/resources/ResourceLocation;Lit/unimi/dsi/fastutil/objects/ObjectArrayList;Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",at=@At("RETURN"),require=1,remap=false)
 private static void tnkloot$finalLoot(ResourceLocation table,ObjectArrayList<ItemStack> loot,LootContext context,CallbackInfoReturnable<ObjectArrayList<ItemStack>> result){FinalLoot.table(result.getReturnValue(),context);}
}
