package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.apotheosis.UnifiedApotheosis;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.mobs.util.BossStats;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.RegistryAccess;
@Mixin(value=Invader.class,remap=false)
public abstract class ApothInvaderMixin {
    @Inject(method="modifyBossItem",at=@At("HEAD"),cancellable=true,remap=false)
    private static void tnkloot$prizePool(ItemStack stack,Component bossName,GenContext context,LootRarity rarity,int enchantmentLevel,RegistryAccess registries,boolean cursed,CallbackInfoReturnable<ItemStack> result) {
        var replacement=dev.mikaaah.tnkloot.apotheosis.NativeLootPools.invaderBase(stack,context);
        if(replacement!=stack)result.setReturnValue(replacement.isEmpty()?ItemStack.EMPTY:Invader.modifyBossItem(replacement,bossName,context,rarity,enchantmentLevel,registries,cursed));
    }
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,remap=false)
    private static Map<LootRarity,BossStats> tnkloot$sixBossGroups(Map<LootRarity,BossStats> values) {
        var mythic=RarityRegistry.INSTANCE.getValue(ResourceLocation.parse("tnkloot:mythic"));
        var legendary=RarityRegistry.INSTANCE.getValue(ResourceLocation.parse("tnkloot:legendary"));
        if(legendary!=null && values.containsKey(mythic) && !values.containsKey(legendary)) {
            var next=new HashMap<>(values);next.put(legendary,values.get(mythic));return Map.copyOf(next);
        }
        return values;
    }
    @ModifyVariable(method="initBoss",at=@At("HEAD"),argsOnly=true,remap=false)
    private LootRarity tnkloot$explicitRarity(LootRarity rarity) { return rarity==null?null:UnifiedApotheosis.canonical(rarity); }
}
