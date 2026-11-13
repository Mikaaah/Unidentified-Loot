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
  var root=JsonParser.parseString(Files.readString(path())).getAsJsonObject();
  if(migrate(root)) {
   parse(root);Path backup=path().resolveSibling("dimensions.json.before-1.1.3.bak");if(!Files.exists(backup))Files.copy(path(),backup);
   Path temporary=path().resolveSibling("dimensions.json.compat.tmp");Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(root));Files.move(temporary,path(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
  }
  current=parse(root);return true;
 }catch(Exception ex){System.err.println("[TNK Loot] Invalid dimensions.json; retaining settings: "+ex.getMessage());if(current==null)try(var in=DimensionRules.class.getResourceAsStream("/default-dimensions.json")){current=parse(JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject());}catch(Exception bad){throw new IllegalStateException(bad);}return false;}}
 public static boolean migrate(JsonObject root){
  if(root.has("tnk_pack_routes_revision")&&root.get("tnk_pack_routes_revision").getAsInt()>=1)return false;
  var routes=root.getAsJsonArray("routes");
  for(var value:routes){var route=value.getAsJsonObject();String dimension=route.get("dimension").getAsString();String[] additions=dimension.equals("minecraft:the_end")?new String[]{"enderscape","the_beyond"}:dimension.equals("minecraft:the_nether")?new String[]{"netherexp"}:new String[0];
   var namespaces=route.getAsJsonArray("item_namespaces");Set<String> existing=new HashSet<>();namespaces.forEach(n->existing.add(n.getAsString()));for(String addition:additions)if(existing.add(addition))namespaces.add(addition);
   for(int i=namespaces.size()-1;i>=0;i--){String namespace=namespaces.get(i).getAsString();if(Set.of("betterend","betternether").contains(namespace)&&!net.neoforged.fml.ModList.get().isLoaded(namespace))namespaces.remove(i);}
  }
  if(routes.asList().stream().noneMatch(v->v.getAsJsonObject().get("dimension").getAsString().equals("deeperdarker:otherside"))){
   var defaults=dev.mikaaah.tnkloot.compat.CompatPresets.bundled("default-dimensions.json");
   defaults.getAsJsonArray("routes").asList().stream().map(JsonElement::getAsJsonObject).filter(v->v.get("dimension").getAsString().equals("deeperdarker:otherside")).findFirst().ifPresent(v->routes.add(v.deepCopy()));
  }
  root.addProperty("tnk_pack_routes_revision",1);return true;
 }
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
