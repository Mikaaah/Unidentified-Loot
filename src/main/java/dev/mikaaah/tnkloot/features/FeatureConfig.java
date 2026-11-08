package dev.mikaaah.tnkloot.features;
import com.google.gson.*;
import dev.mikaaah.tnkloot.config.TnkConfig;
import java.nio.file.*;
import java.util.*;

public final class FeatureConfig {
 public record Settings(boolean dungeon,double chestChance,List<String> tablePrefixes,boolean collection,boolean salvage,boolean previews,boolean reveals,boolean bossAffixes,int cooldown,int duration,int maxCredit,double refundCap,List<Integer> salvageValues,Map<String,String> themes){}
 private static volatile Settings current;
 public static Settings get(){if(current==null)reload();return current;}
 public static Path path(){return TnkConfig.path().resolveSibling("features.json");}
 public static synchronized boolean reload(){
  try{
   if(!Files.exists(path())){Files.createDirectories(path().getParent());try(var in=FeatureConfig.class.getResourceAsStream("/default-features.json")){Files.copy(in,path());}}
   current=parse(JsonParser.parseString(Files.readString(path())).getAsJsonObject());return true;
  }catch(Exception e){System.err.println("[TNK Loot] features.json rejected: "+e.getMessage());if(current==null)try(var in=FeatureConfig.class.getResourceAsStream("/default-features.json")){current=parse(JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject());}catch(Exception bad){throw new IllegalStateException(bad);}return false;}
 }
 public static Settings parse(JsonObject j){
  double chance=j.get("dungeon_accessory_chance").getAsDouble(),refund=j.get("max_salvage_refund_fraction").getAsDouble();int cooldown=j.get("boss_proc_cooldown_ticks").getAsInt(),duration=j.get("boss_proc_duration_ticks").getAsInt(),max=j.get("max_salvage_credit").getAsInt();
  if(!Double.isFinite(refund)||refund<=0||refund>1||!Double.isFinite(chance)||chance<0||chance>1||cooldown<20||cooldown>72000||duration<1||duration>1200||max<1||max>100000000)throw new IllegalArgumentException("Feature limits out of range");
  List<Integer> values=new ArrayList<>();for(var v:j.getAsJsonArray("salvage_values")){int n=v.getAsBigDecimal().intValueExact();if(n<0||n>10000)throw new IllegalArgumentException("Invalid salvage value");values.add(n);}if(values.size()!=6)throw new IllegalArgumentException("Six salvage rarity values required");
  List<String> prefixes=new ArrayList<>();for(var v:j.getAsJsonArray("dungeon_table_prefixes")){String s=v.getAsString();if(s.isBlank()||s.length()>256)throw new IllegalArgumentException("Invalid loot prefix");prefixes.add(s);}
  Map<String,String> themes=new LinkedHashMap<>();for(var e:j.getAsJsonObject("boss_theme_overrides").entrySet()){String theme=e.getValue().getAsString();if(!Set.of("ember","frost","storm","venom","void","earth","none").contains(theme))throw new IllegalArgumentException("Unknown boss theme");themes.put(e.getKey(),theme);}
  return new Settings(j.get("dungeon_accessories").getAsBoolean(),chance,List.copyOf(prefixes),j.get("discovery_collection").getAsBoolean(),j.get("salvaging").getAsBoolean(),j.get("pool_previews").getAsBoolean(),j.get("rarity_reveals").getAsBoolean(),j.get("boss_signature_affixes").getAsBoolean(),cooldown,duration,max,refund,List.copyOf(values),Map.copyOf(themes));
 }
}
