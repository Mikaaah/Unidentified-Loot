package dev.mikaaah.tnkloot.injection;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Unique native payloads are earned items, not interchangeable blank equipment. */
public final class LootProtection {
    private LootProtection() {}
    public static boolean keepNative(ItemStack stack) {
        if (stack.isEmpty() || FinalLoot.own(stack)) return false;
        if (dev.mikaaah.tnkloot.compat.PresetCleanup.requiresNativePayload(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) return true;
        var tag=dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem.data(stack);
        if(tag.getBoolean("tnk_random_pool")&&tag.getBoolean("tnk_external_generated"))return false;
        if(tag.getBoolean("tnk_fixed_native_payload")&&tag.getBoolean("tnk_external_generated"))return true;
        if (stack.has(DataComponents.CUSTOM_NAME) && !stack.getOrDefault(Apoth.Components.FROM_BOSS, false) && !AffixHelper.hasAffixes(stack)) return true;
        for (var entry : stack.getComponentsPatch().entrySet()) {
            var id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            if (entry.getValue().isPresent() && id != null && !java.util.Set.of("minecraft", "apotheosis", "tnkloot").contains(id.getNamespace())) return true;
        }
        return false;
    }
}
