package dev.mikaaah.tnkprogression;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
public final class ProgressionEvents {
 @SubscribeEvent public static void join(EntityJoinLevelEvent e){
  if(!(e.getLevel() instanceof ServerLevel world) || !(e.getEntity() instanceof Mob mob) || !(mob instanceof Enemy) || !ProgressionConfig.current.scaleMobs)return;
  var c=ProgressionConfig.current;CompoundTag n=mob.getPersistentData();boolean fresh=!n.contains("tnk_level");
  if(fresh){double dx=mob.getX()-world.getSharedSpawnPos().getX(),dz=mob.getZ()-world.getSharedSpawnPos().getZ();int base=c.dimensionBaseLevels.getOrDefault(world.dimension().location().toString(),1);int level=(int)Math.min(100,base+Math.hypot(dx,dz)/c.blocksPerMobLevel+Math.max(0,48-mob.getY())/16);n.putInt("tnk_level",Math.max(1,level));}
  int level=ProgressionAPI.mobLevel(mob);
  scale(mob.getAttribute(Attributes.MAX_HEALTH),"health",c.healthPerLevel*(level-1));
  scale(mob.getAttribute(Attributes.ATTACK_DAMAGE),"damage",c.damagePerLevel*(level-1));
  if(fresh)mob.setHealth(mob.getMaxHealth());
  else if(mob.getHealth()>mob.getMaxHealth())mob.setHealth(mob.getMaxHealth());
  // Persistent custom name: server-only installations can still see the level.
  if(!mob.hasCustomName()){mob.setCustomName(net.minecraft.network.chat.Component.literal("Lv. "+level+" ").append(mob.getType().getDescription()));}
 }
 private static void scale(AttributeInstance a,String key,double value){if(a==null)return;var id=ResourceLocation.parse("tnkprogression:"+key);a.removeModifier(id);a.addPermanentModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent e){if(e.getEntity() instanceof Enemy && e.getSource().getEntity() instanceof ServerPlayer p)ProgressionAPI.activity(p,"combat",1);}
 private static String location(net.minecraft.world.level.Level l,net.minecraft.core.BlockPos pos){return l.dimension().location()+"/"+pos.asLong();}
 @SubscribeEvent public static void place(BlockEvent.EntityPlaceEvent e){if(!(e.getLevel() instanceof ServerLevel l))return;ProgressionData d=ProgressionData.get(l.getServer());d.entry("placed").putBoolean(location(l,e.getPos()),true);d.setDirty();}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void mine(BlockEvent.BreakEvent e){
  if(!(e.getPlayer() instanceof ServerPlayer p) || p.isCreative())return;
  ProgressionData d=ProgressionData.get(p.server);CompoundTag placed=d.entry("placed");String key=location(p.level(),e.getPos());
  if(placed.contains(key)){placed.remove(key);d.setDirty();return;}
  if(e.getState().is(BlockTags.BASE_STONE_OVERWORLD) || e.getState().is(BlockTags.BASE_STONE_NETHER) || e.getState().is(BlockTags.COAL_ORES) || e.getState().is(BlockTags.IRON_ORES) || e.getState().is(BlockTags.COPPER_ORES) || e.getState().is(BlockTags.GOLD_ORES) || e.getState().is(BlockTags.DIAMOND_ORES) || e.getState().is(BlockTags.REDSTONE_ORES) || e.getState().is(BlockTags.LAPIS_ORES) || e.getState().is(BlockTags.EMERALD_ORES))ProgressionAPI.activity(p,"mining",1);
 }
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){ProgressionAPI.profile(p).putInt("fish_baseline",p.getStats().getValue(Stats.CUSTOM.get(Stats.FISH_CAUGHT)));ProgressionData.get(p.server).setDirty();SkillBridge.sync(p);}}
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p) || p.tickCount%20!=0)return;
  CompoundTag n=ProgressionAPI.profile(p);
  int fish=p.getStats().getValue(Stats.CUSTOM.get(Stats.FISH_CAUGHT)),old=n.getInt("fish_baseline");
  if(fish!=old){n.putInt("fish_baseline",fish);ProgressionAPI.activity(p,"fishing",Math.max(0,fish-old));ProgressionData.get(p.server).setDirty();}
  String chunk=p.level().dimension().location()+"/"+ChunkPos.asLong(p.chunkPosition().x,p.chunkPosition().z);
  CompoundTag seen=n.getCompound("explored");if(!seen.contains(chunk)){seen.putBoolean(chunk,true);n.put("explored",seen);ProgressionAPI.activity(p,"exploration",1);ProgressionData.get(p.server).setDirty();}
 }
}
