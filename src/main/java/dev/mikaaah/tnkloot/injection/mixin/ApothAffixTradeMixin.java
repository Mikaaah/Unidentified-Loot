package dev.mikaaah.tnkloot.injection.mixin;

import dev.mikaaah.tnkloot.apotheosis.NativeLootPools;
import dev.shadowsoffire.apotheosis.affix.trades.*;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.Constraints;
import dev.shadowsoffire.apotheosis.loot.AffixLootEntry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Explicit datapack trades must not bypass the default random-pool policy. */
@Mixin(value={AffixTrade.class,AutomaticAffixTrade.class},remap=false)
public abstract class ApothAffixTradeMixin {
    @Shadow @Final protected List<DynamicHolder<AffixLootEntry>> entries;

    @Unique private static boolean eligible(AffixLootEntry entry,GenContext context) {
        return NativeLootPools.allowed(entry.stack(),context)&&Constraints.eval(context).test(entry)
            &&entry.weights().getWeight(context.tier(),context.luck())>0;
    }

    @Inject(method="getOffer",at=@At("HEAD"),cancellable=true,remap=false)
    private void tnkloot$emptyTradePool(Entity merchant,RandomSource random,CallbackInfoReturnable<MerchantOffer> result) {
        if(entries.isEmpty()||merchant.level().isClientSide)return;
        var player=merchant.level().getNearestPlayer(merchant,-1);
        if(player==null)return;
        var context=GenContext.forPlayer(random,player);
        if(entries.stream().filter(DynamicHolder::isBound).map(DynamicHolder::get).noneMatch(entry->eligible(entry,context)))result.setReturnValue(null);
    }

    @Redirect(method="getOffer",at=@At(value="INVOKE",target="Ljava/util/stream/Stream;toList()Ljava/util/List;"),remap=false)
    private List<WeightedEntry.Wrapper<AffixLootEntry>> tnkloot$filterTradePool(Stream<WeightedEntry.Wrapper<AffixLootEntry>> values,Entity merchant,RandomSource random) {
        var player=merchant.level().getNearestPlayer(merchant,-1);
        var context=GenContext.forPlayer(random,player);
        return values.filter(value->eligible(value.data(),context)).toList();
    }

    @Inject(method="getOffer",at=@At("RETURN"),cancellable=true,remap=false)
    private void tnkloot$tradePool(Entity merchant,RandomSource random,CallbackInfoReturnable<MerchantOffer> result) {
        if(result.getReturnValue()==null)return;
        var player=merchant.level().getNearestPlayer(merchant,-1);
        if(player==null||!NativeLootPools.allowed(result.getReturnValue().getResult(),GenContext.forPlayer(random,player)))result.setReturnValue(null);
    }
}
