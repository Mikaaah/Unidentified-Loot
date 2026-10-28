package dev.mikaaah.tnkprogression;
import com.google.gson.*;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.*;
import java.util.*;
public final class ProgressionConfig {
 public int[] ceilings = {10,30,50,70,80,94,100};
 public int[] rarityCaps = {1,2,3,4,4,5,5};
 public boolean shareTeamUnlocks = true;
 public String skillCategory = "tnk:character";
 public boolean scaleMobs = true;
 public double blocksPerMobLevel = 256;
 public double healthPerLevel = 0.035, damagePerLevel = 0.015;
 public Map<String,Integer> dimensionBaseLevels = new HashMap<>(Map.of("minecraft:overworld",1,"minecraft:the_nether",31,"minecraft:the_end",71));
 public Map<String,Activity> activities = new HashMap<>(Map.of("mining",new Activity(100,25),"combat",new Activity(10,35),"fishing",new Activity(1,15),"exploration",new Activity(10,30)));
 public record Activity(long initialRequirement,long initialXp) {}
 public static ProgressionConfig current = new ProgressionConfig();
 public static boolean reload() {
  Path path=FMLPaths.CONFIGDIR.get().resolve("tnkprogression.json");
  try {
   Gson gson=new GsonBuilder().setPrettyPrinting().create();
   if (!Files.exists(path)) Files.writeString(path,gson.toJson(new ProgressionConfig()));
   ProgressionConfig c=gson.fromJson(Files.readString(path),ProgressionConfig.class);
   if(c.ceilings.length!=c.rarityCaps.length || c.ceilings.length==0 || c.ceilings[c.ceilings.length-1]!=100 || c.blocksPerMobLevel<1 || !Double.isFinite(c.blocksPerMobLevel) || !Double.isFinite(c.healthPerLevel) || !Double.isFinite(c.damagePerLevel) || c.healthPerLevel<0 || c.damagePerLevel<0) throw new IllegalArgumentException("Invalid balance settings");
   int prev=0;
   for(int i=0;i<c.ceilings.length;i++){ if(c.ceilings[i]<=prev || c.rarityCaps[i]<0 || c.rarityCaps[i]>5) throw new IllegalArgumentException("Invalid milestones"); prev=c.ceilings[i]; }
   for(String key:List.of("mining","combat","fishing","exploration")) { Activity a=c.activities.get(key); if(a==null || a.initialRequirement<1 || a.initialXp<1)throw new IllegalArgumentException("Invalid activity "+key); }
   if(c.skillCategory==null || net.minecraft.resources.ResourceLocation.tryParse(c.skillCategory)==null)throw new IllegalArgumentException("Invalid skill category");
   current=c; return true;
  } catch(Exception e){ TnkProgression.LOG.error("Keeping previous progression config",e); return false; }
 }
}
