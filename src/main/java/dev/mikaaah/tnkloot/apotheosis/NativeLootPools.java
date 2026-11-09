package dev.mikaaah.tnkloot.apotheosis;

import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import dev.mikaaah.tnkloot.dimension.DimensionRules;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.mikaaah.tnkloot.loot.*;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.tiers.*;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Random native rewards use the same item eligibility as sealed UL loot. Owned-item reforging is separate. */
public final class NativeLootPools {
    private NativeLootPools() {}

    public static int sourceLevel(GenContext context) {
        var owner=(ContextOwner)(Object)context;
        if(owner.tnkloot$player() instanceof ServerPlayer player) {
            int source=owner.tnkloot$sourceLevel()>0?owner.tnkloot$sourceLevel():TnkLevelBridge.level(player);
            return TnkLootRolls.effective(player,source);
        }
        if(owner.tnkloot$player()!=null)return Math.min(ApotheosisClientState.level,ApotheosisClientState.ceiling);
        // Standalone native world-generation contexts still have an explicit world tier.
        return switch(context.tier()) { case HAVEN->10; case FRONTIER->30; case ASCENT->70; case SUMMIT->94; case PINNACLE->100; };
    }

    public static GenContext context(LootContext loot) {
        var context=GenContext.forLoot(loot);
        if(context!=null)return context;
        var pos=loot.getParamOrNull(LootContextParams.ORIGIN);
        return GenContext.standalone(loot.getRandom(),WorldTier.HAVEN,loot.getLuck(),loot.getLevel(),pos==null?BlockPos.ZERO:BlockPos.containing(pos));
    }

    public static boolean allowed(ItemStack stack,GenContext context) {
        if(stack.isEmpty()||LootCategory.forItem(stack).isNone())return false;
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if(BossRewards.exclusive(id)||!DimensionRules.allowed(id,context.dimension().location().toString()))return false;
        int maximum=LevelTierRoll.roll(sourceLevel(context),99);
        return TnkConfig.get().weapons().stream().anyMatch(w->w.id().equals(id)&&WeaponSelector.available(w)&&w.minTier()<=maximum);
    }

    private static List<TnkConfig.Weapon> candidates(int tier,TnkConfig.Category category,GenContext context) {
        return DimensionLoot.pool(tier,category,context.dimension().location().toString()).stream()
            .filter(w->!LootCategory.forItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(w.id())))).isNone()).toList();
    }

    public static ItemStack rollBase(GenContext context,TnkConfig.Category preferred) {
        int requested=LevelTierRoll.roll(sourceLevel(context),context.rand().nextInt(100));
        for(int tier=requested;tier>=0;tier--) {
            final int rank=tier;
            var categories=Arrays.stream(TnkConfig.Category.values()).filter(c->preferred==null||c==preferred)
                .filter(c->!candidates(rank,c,context).isEmpty()).toList();
            if(categories.isEmpty())continue;
            var category=categories.get(context.rand().nextInt(categories.size()));
            var pool=new WeightedPool<TnkConfig.Weapon>(candidates(tier,category,context).stream().map(w->new WeightedPool.Entry<>(w,w.weight())).toList());
            var chosen=pool.roll(context.rand()::nextLong).orElseThrow();
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(chosen.id())));
            var tag=UnidentifiedWeaponItem.data(stack);tag.putInt("tnk_tier",tier);tag.putBoolean("tnk_random_pool",true);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public static AffixLootEntry randomEntry(GenContext context) {
        var stack=rollBase(context,null);
        return stack.isEmpty()?null:new AffixLootEntry(TieredWeights.forAllTiers(1,0),stack);
    }

    public static ItemStack fromPools(Set<DynamicHolder<LootRarity>> rarities,Set<DynamicHolder<AffixLootEntry>> entries,GenContext context) {
        if(entries.isEmpty())return LootController.createRandomLootItem(context,LootRarity.randomFromHolders(context,rarities));
        var pool=entries.stream().filter(DynamicHolder::isBound).map(DynamicHolder::get)
            .filter(Constraints.eval(context)).filter(entry->allowed(entry.stack(),context))
            .filter(entry->entry.weights().getWeight(context.tier(),context.luck())>0)
            .map(entry->new WeightedPool.Entry<>(entry,entry.weights().getWeight(context.tier(),context.luck()))).toList();
        var selected=new WeightedPool<AffixLootEntry>(pool).roll(context.rand()::nextLong);
        if(selected.isEmpty())return ItemStack.EMPTY;
        var entry=selected.get();
        var rarity=rarities.isEmpty()?LootRarity.random(context,entry.rarities()):LootRarity.randomFromHolders(context,rarities);
        var stack=entry.stack();var tag=UnidentifiedWeaponItem.data(stack);tag.putBoolean("tnk_random_pool",true);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        return LootController.createLootItem(stack,rarity,context);
    }

    public static ItemStack invaderBase(ItemStack original,GenContext context) {
        return allowed(original,context)?original:rollBase(context,FinalLoot.category(original));
    }
}
