package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.apotheosis.UnifiedApotheosis;
import dev.shadowsoffire.apotheosis.affix.*;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import java.util.stream.Stream;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=LootController.class,remap=false)
public abstract class ApothLootControllerMixin {
    @Inject(method="createAffixItemFromPools",at=@At("HEAD"),cancellable=true,remap=false)
    private static void tnkloot$explicitPools(Set<DynamicHolder<LootRarity>> rarities,Set<DynamicHolder<AffixLootEntry>> entries,GenContext context,CallbackInfoReturnable<ItemStack> result) {
        result.setReturnValue(dev.mikaaah.tnkloot.apotheosis.NativeLootPools.fromPools(rarities,entries,context));
    }
    @Inject(method="createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/loot/LootCategory;Ldev/shadowsoffire/apotheosis/loot/LootRarity;Ldev/shadowsoffire/apotheosis/tiers/GenContext;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"),cancellable=true,remap=false)
    private static void tnkloot$generate(ItemStack stack,LootCategory category,LootRarity rarity,GenContext context,CallbackInfoReturnable<ItemStack> result) {
        result.setReturnValue(UnifiedApotheosis.generate(stack,category,rarity,context));
    }
    @Inject(method="getAvailableAffixes",at=@At("RETURN"),cancellable=true,remap=false)
    private static void tnkloot$affixes(ItemStack stack,LootRarity rarity,AffixType type,CallbackInfoReturnable<Stream<DynamicHolder<Affix>>> result) {
        result.setReturnValue(result.getReturnValue().filter(affix->affix.getId().getNamespace().equals("tnkloot")));
    }
}
