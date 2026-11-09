package dev.mikaaah.tnkloot.injection.mixin;
import dev.shadowsoffire.apotheosis.loot.*;
import java.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Keep native IDs loadable while exposing only the six active UL groups/materials. */
@Mixin(value=RarityRegistry.class,remap=false)
public abstract class ApothRarityRegistryMixin {
    @Redirect(method="onReload",at=@At(value="INVOKE",target="Ljava/util/Collection;iterator()Ljava/util/Iterator;"),remap=false)
    private Iterator<LootRarity> tnkloot$canonicalGroups(Collection<LootRarity> rarities) {
        return rarities.stream().filter(rarity->RarityRegistry.INSTANCE.getKey(rarity).getNamespace().equals("tnkloot")).iterator();
    }
}
