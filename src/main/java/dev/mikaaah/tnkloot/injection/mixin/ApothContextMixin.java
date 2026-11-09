package dev.mikaaah.tnkloot.injection.mixin;
import dev.mikaaah.tnkloot.apotheosis.ContextOwner;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=GenContext.class,remap=false)
public abstract class ApothContextMixin implements ContextOwner {
    @Unique private Player tnkloot$owner;
    @Unique private int tnkloot$sourceLevel;
    public Player tnkloot$player() { return tnkloot$owner; }
    public void tnkloot$player(Player player) { tnkloot$owner=player; }
    public int tnkloot$sourceLevel() { return tnkloot$sourceLevel; }
    public void tnkloot$sourceLevel(int level) { tnkloot$sourceLevel=level; }
    @Inject(method="forPlayerAtPos",at=@At("RETURN"),remap=false)
    private static void tnkloot$playerContext(RandomSource rand,Player player,BlockPos pos,CallbackInfoReturnable<GenContext> result) {
        ((ContextOwner)(Object)result.getReturnValue()).tnkloot$player(player);
    }
    @Inject(method="forLoot",at=@At("RETURN"),remap=false)
    private static void tnkloot$lootContext(LootContext context,CallbackInfoReturnable<GenContext> result) {
        if(result.getReturnValue()!=null) {
            var owner=(ContextOwner)(Object)result.getReturnValue();owner.tnkloot$player(GenContext.findPlayer(context));
            if(context.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY) instanceof net.minecraft.world.entity.LivingEntity entity && !(entity instanceof Player))
                owner.tnkloot$sourceLevel(dev.mikaaah.tnkloot.loot.TnkLevelBridge.mobLevel(entity));
        }
    }
}
