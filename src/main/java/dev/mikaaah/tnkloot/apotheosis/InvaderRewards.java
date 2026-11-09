package dev.mikaaah.tnkloot.apotheosis;

import dev.mikaaah.tnkloot.compat.PackItemPolicy;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.rarity.*;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** Preserve the exact native invader prize, while identifying it on the UL scale. */
public final class InvaderRewards {
    private InvaderRewards() {}
    public static boolean raw(ItemStack stack) {
        var tag=UnidentifiedWeaponItem.data(stack);
        return !(stack.getItem() instanceof UnidentifiedWeaponItem) && stack.getOrDefault(Apoth.Components.FROM_BOSS,false)
            && (!tag.contains("tnk_rarity") || tag.getBoolean("tnk_external_generated"));
    }
    public static ItemStack token(ItemStack original,Level level,ServerPlayer player) {
        var category=FinalLoot.category(original);if(category==null)return ItemStack.EMPTY;
        var context=player==null?dev.shadowsoffire.apotheosis.tiers.GenContext.dummy(level.random):dev.shadowsoffire.apotheosis.tiers.GenContext.forPlayer(player);
        int tier=UnifiedApotheosis.rank(original,player,context);
        var result=UnidentifiedWeaponItem.create(category,tier,BuiltInRegistries.ITEM.getKey(original.getItem()).toString(),"boss_reward");
        var tag=UnidentifiedWeaponItem.data(result);tag.putBoolean("apotheosis_invader",true);tag.putString("boss_name","Apotheosis Invader");
        ItemStack base=original.copyWithCount(1);
        base.remove(Apoth.Components.AFFIXES);base.remove(Apoth.Components.RARITY);base.remove(Apoth.Components.AFFIX_NAME);base.remove(Apoth.Components.DURABILITY_BONUS);
        if(base.getOrDefault(Apoth.Components.SOCKETED_GEMS,ItemContainerContents.EMPTY).nonEmptyStream().findAny().isEmpty())base.remove(Apoth.Components.SOCKETS);
        var data=UnidentifiedWeaponItem.data(base);data.remove("tnk_external_generated");base.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        tag.put("tnk_exact_result",base.save(level.registryAccess()));result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        var nativeRarity=AffixHelper.getRarity(original);
        if(nativeRarity.isBound()) RaritySupport.force(result,UnifiedApotheosis.quality(nativeRarity.get()).id());
        return result;
    }
    public static ItemStack base(Level level,ItemStack token) {
        var tag=UnidentifiedWeaponItem.data(token);if(!tag.contains("tnk_exact_result",10))return ItemStack.EMPTY;
        var result=ItemStack.parseOptional(level.registryAccess(),tag.getCompound("tnk_exact_result"));
        return result.isEmpty() || !PackItemPolicy.allowed(BuiltInRegistries.ITEM.getKey(result.getItem()).toString()) ? ItemStack.EMPTY : result;
    }
}
