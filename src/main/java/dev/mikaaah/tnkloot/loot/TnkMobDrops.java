package dev.mikaaah.tnkloot.loot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
public final class TnkMobDrops {
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void drop(LivingDropsEvent e){
  if(!(e.getEntity() instanceof Mob mob) || !(mob instanceof Enemy) || !(e.getSource().getEntity() instanceof ServerPlayer p) || !TnkConfig.get().enableIdentification() || !TnkConfig.get().replaceWeaponLoot())return;
  // Own equipment generation once per death. Preserve gems, materials and other loot.
  // Remove equipment from both normal and Apotheosis paths to avoid a second rarity economy.
  e.getDrops().removeIf(drop->{var stack=drop.getItem();if(stack.getItem() instanceof UnidentifiedWeaponItem)return true;String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();return AffixHelper.hasAffixes(stack)||TnkConfig.get().weapons().stream().anyMatch(w->w.id().equals(id));});
  if(p.getRandom().nextDouble()>=TnkBetaConfig.current.equipmentDropChance)return;
  int level=Math.max(1,Math.min(100,mob.getPersistentData().getInt("tnk_level")));
  var token=TnkLootRolls.roll(p,level,"tnk_entity_drop");if(!token.isEmpty())e.getDrops().add(new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),token));
 }
}
