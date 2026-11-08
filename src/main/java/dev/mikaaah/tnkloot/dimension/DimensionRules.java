package dev.mikaaah.tnkloot.dimension;
import com.google.gson.*;
import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.util.*;
public final class DimensionRules {
 public record Route(String dimension,boolean includeGlobal,double chestChance,double mobChance,List<String> namespaces,List<String> prefixes){}
 public record Settings(boolean enabled,Map<String,Route> routes){}
 private static volatile Settings current;
 public static Path path(){return TnkConfig.path().resolveSibling("dimensions.json");}
 public static Settings get(){if(current==null)reload();return current;}
 public static synchronized boolean reload(){try{
  if(!Files.exists(path())){Files.createDirectories(path().getParent());try(var in=DimensionRules.class.getResourceAsStream("/default-dimensions.json")){Files.copy(in,path());}}
  current=parse(JsonParser.parseString(Files.readString(path())).getAsJsonObject());return true;
 }catch(Exception ex){System.err.println("[TNK Loot] Invalid dimensions.json; retaining settings: "+ex.getMessage());if(current==null)try(var in=DimensionRules.class.getResourceAsStream("/default-dimensions.json")){current=parse(JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject());}catch(Exception bad){throw new IllegalStateException(bad);}return false;}}
 public static Settings parse(JsonObject root){Map<String,Route> routes=new LinkedHashMap<>();for(var value:root.getAsJsonArray("routes")){
  var p=value.getAsJsonObject();String dimension=p.get("dimension").getAsString();double chest=p.get("bonus_chest_chance").getAsDouble(),mob=p.get("bonus_hostile_mob_chance").getAsDouble();
  if(ResourceLocation.tryParse(dimension)==null||routes.containsKey(dimension)||!Double.isFinite(chest)||!Double.isFinite(mob)||chest<0||chest>1||mob<0||mob>1)throw new IllegalArgumentException("Invalid dimension route");
  List<String> namespaces=new ArrayList<>(),prefixes=new ArrayList<>();for(var n:p.getAsJsonArray("item_namespaces")){String s=n.getAsString();if(!s.matches("[a-z0-9_.-]+"))throw new IllegalArgumentException("Invalid item namespace");namespaces.add(s);}for(var n:p.getAsJsonArray("item_prefixes")){String s=n.getAsString();if(ResourceLocation.tryParse(s)==null)throw new IllegalArgumentException("Invalid item prefix");prefixes.add(s);}
  routes.put(dimension,new Route(dimension,p.get("include_global_equipment").getAsBoolean(),chest,mob,List.copyOf(namespaces),List.copyOf(prefixes)));
 }return new Settings(root.get("enabled").getAsBoolean(),Map.copyOf(routes));}
 public static boolean owned(Route r,String item){int colon=item.indexOf(':');if(colon<1)return false;String ns=item.substring(0,colon);return r.namespaces().contains(ns)||r.prefixes().stream().anyMatch(item::startsWith);}
 public static boolean allowed(String item,String dimension){return allowed(get(),item,dimension);}
 public static boolean allowed(Settings s,String item,String dimension){if(!s.enabled())return true;boolean restricted=s.routes().values().stream().anyMatch(r->owned(r,item));Route here=s.routes().get(dimension);if(restricted)return here!=null&&owned(here,item);return here==null||here.includeGlobal();}
 public static String infer(String item){var matches=get().routes().values().stream().filter(r->owned(r,item)).map(Route::dimension).toList();return matches.size()==1?matches.getFirst():"";}
}
