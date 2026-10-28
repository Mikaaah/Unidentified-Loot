package dev.mikaaah.tnkprogression;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.nbt.CompoundTag;
public final class ProgressionSmoke {
 public static void run(ServerStartedEvent e){
  if(!Boolean.getBoolean("tnkprogression.smoke"))return;
  try{
   var p=FakePlayerFactory.get(e.getServer().overworld(),new com.mojang.authlib.GameProfile(java.util.UUID.fromString("9520ec62-6399-410a-885c-581e04022180"),"ProgressionTest"));
   CompoundTag n=ProgressionAPI.profile(p);for(String key:java.util.Set.copyOf(n.getAllKeys()))n.remove(key);
   check(ProgressionAPI.level(p)==1,"starts at one");
   for(int badgeLevel:new int[]{0,1,9,10,42,99,100}){
    var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
    try{var payload=new LevelBadgePayload(p.getUUID(),badgeLevel);LevelBadgePayload.CODEC.encode(buffer,payload);check(payload.equals(LevelBadgePayload.CODEC.decode(buffer)),"badge payload roundtrip");check(buffer.readableBytes()==0,"badge payload consumes buffer");}finally{buffer.release();}
   }
   check(p.getDisplayName().getString().startsWith(LevelBadges.glyph(1)+" "),"initial name badge");
   check(ProgressionAPI.ceiling(p)==10,"initial ceiling");
   ProgressionAPI.awardXp(p,10000000);check(ProgressionAPI.level(p)==10,"cap blocks powerleveling");
   check(p.getDisplayName().getString().startsWith(LevelBadges.glyph(10)+" "),"level-up refreshes badge");
   p.refreshDisplayName();check(p.getDisplayName().getString().equals(LevelBadges.glyph(10)+" ProgressionTest"),"badge refresh does not duplicate prefix or change username");
   check(n.getLong("xp")==0,"no banked XP");
   check(ProgressionAPI.awardXp(p,1000)==0,"capped XP rejected");
   ProgressionAPI.unlock(p,30);ProgressionAPI.awardXp(p,ProgressionMath.levelCost(10));check(ProgressionAPI.level(p)==11,"quest opens next bracket");
   long before=n.getLong("xp");check(ProgressionAPI.rewardOnce(p,"test:reward",25),"first quest claim");check(!ProgressionAPI.rewardOnce(p,"test:reward",25),"duplicate quest rejected");check(n.getLong("xp")==before+25,"reward paid once");
   ProgressionAPI.unlock(p,100);ProgressionAPI.awardXp(p,1000000000);check(ProgressionAPI.level(p)==100,"max level");check(n.getLong("xp")==0,"max level XP zero");
   check(ProgressionAPI.rarityCap(p)==5,"endgame rarity access");
   check(p.getDisplayName().getString().startsWith(LevelBadges.glyph(100)+" "),"three digit badge");
   var tag=ProgressionData.get(p.server).save(new CompoundTag(),p.registryAccess());
   var loaded=ProgressionData.load(tag,p.registryAccess());check(loaded.entry("player:"+p.getUUID()).getInt("level")==100,"save reload");
   TnkProgression.LOG.info("TNK_PROGRESSION_SMOKE_OK");
  }catch(Throwable t){TnkProgression.LOG.error("TNK_PROGRESSION_SMOKE_FAILED",t);}
  e.getServer().halt(false);
 }
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
