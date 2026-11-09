package dev.mikaaah.tnkloot.apotheosis;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.BaseCompat;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.rarity.*;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.*;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.tiers.*;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class UnifiedApotheosis {
    private UnifiedApotheosis() {}
    private static final Set<String> LEGACY=Set.of("common","uncommon","rare","epic","mythic");
    public static ResourceLocation alias(ResourceLocation id) {
        return id!=null && id.getNamespace().equals("apotheosis") && LEGACY.contains(id.getPath()) ? ResourceLocation.fromNamespaceAndPath("tnkloot",id.getPath()) : id;
    }
    public static LootRarity canonical(LootRarity rarity) {
        ResourceLocation key=RarityRegistry.INSTANCE.getKey(rarity);
        var mapped=RarityRegistry.INSTANCE.getValue(alias(key));
        return mapped==null?rarity:mapped;
    }
    public static RarityPolicy.Quality quality(LootRarity rarity) {
        var id=RarityRegistry.INSTANCE.getKey(canonical(rarity));
        if(id==null || !id.getNamespace().equals("tnkloot")) throw new IllegalArgumentException("No UL rarity mapping for "+id);
        return RarityPolicy.get().quality(id.getPath());
    }
    public static int rank(ItemStack stack, Player owner, GenContext context) {
        var tag=UnidentifiedWeaponItem.data(stack);
        if(tag.contains("tnk_tier")) return Math.clamp(tag.getInt("tnk_tier"),0,7);
        int wanted=owner instanceof ServerPlayer sp ? dev.mikaaah.tnkloot.progression.ProgressionService.getTier(sp) : switch(context.tier()) { case HAVEN->1; case FRONTIER->2; case ASCENT->4; case SUMMIT->6; case PINNACLE->7; };
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var defined=TnkConfig.get().weapons().stream().filter(w->w.id().equals(id)).findFirst();
        if(defined.isPresent()) return Math.min(wanted,defined.get().maxTier());
        var category=FinalLoot.category(stack);
        return category==null ? wanted : Math.min(wanted,BaseCompat.band(id,stack.getItem(),category)[1]);
    }
    public static ItemStack generate(ItemStack stack,LootCategory category,LootRarity requested,GenContext context) {
        if(stack.isEmpty() || category.isNone()) return stack;
        Player player=((ContextOwner)(Object)context).tnkloot$player();
        long seed=context.rand().nextLong(); int tier=rank(stack,player,context);
        var q=quality(requested);
        if(player instanceof ServerPlayer sp) q=RarityPolicy.get().quality(TnkLootRolls.RARITIES[Math.min(index(q.id()),TnkLevelBridge.rarityCap(sp))]);
        var rarity=RarityRegistry.INSTANCE.getValue(ResourceLocation.parse(q.apotheosis()));
        if(rarity==null) throw new IllegalStateException("Missing UL rarity "+q.apotheosis());
        RaritySupport.stamp(stack,q,tier);
        var tag=UnidentifiedWeaponItem.data(stack); tag.putBoolean("tnk_external_generated",true);
        if(player instanceof ServerPlayer sp) tag.putInt("tnk_source_level",TnkLootRolls.effective(sp,TnkLevelBridge.level(sp)));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        // Keep the exact item, enchantments, native components and socketed gems.
        if(player!=null) stack=NativeQuality.prepare(stack,tier,player,seed);
        return ApotheosisCompat.applyContext(stack,tier,category,rarity,context,seed);
    }
    public static int index(String id) { return List.of(TnkLootRolls.RARITIES).indexOf(id.replace("tnkloot:","")); }
    public static boolean canReforge(Player player,LootRarity rarity) {
        return !(player instanceof ServerPlayer sp) || index(quality(rarity).id())<=TnkLevelBridge.rarityCap(sp);
    }
    public static float maximum(ItemStack stack) {
        var tag=UnidentifiedWeaponItem.data(stack);
        if(!tag.contains("tnk_rarity")) return 1;
        return (float)RarityPolicy.get().bands().get(Math.clamp(tag.getInt("tnk_tier"),0,RarityPolicy.get().bands().size()-1)).max();
    }
    public static ItemAffixes bounded(ItemStack stack,ItemAffixes affixes) {
        var tag=UnidentifiedWeaponItem.data(stack);
        if(!tag.contains("tnk_rarity")) return affixes;
        var builder=affixes.toBuilder();float max=maximum(stack);
        for(var key:affixes.keySet()) {
            if(!key.getId().getNamespace().equals("tnkloot")) builder.remove(key);
            else builder.put(key,Math.min(max,affixes.getLevel(key)));
        }
        return builder.build();
    }
}
