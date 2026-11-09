package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.apotheosis.UnifiedApotheosis;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.mobs.util.BossStats;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=Invader.class,remap=false)
public abstract class ApothInvaderMixin {
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
