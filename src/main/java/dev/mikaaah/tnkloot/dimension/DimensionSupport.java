package dev.mikaaah.tnkloot.dimension;
import com.google.gson.*;
import dev.mikaaah.tnkloot.compat.*;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;
import java.util.*;
public final class DimensionSupport {
 public static void started(ServerStartedEvent event){DimensionRules.reload();installBosses();BaseCompat.install();}
 public static boolean reload(){return DimensionRules.reload()&&installBosses()&&BaseCompat.install();}
 public static boolean installBosses(){try{
  Path path=BossRewards.path();var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();var pools=root.getAsJsonArray("pools");Set<String> ids=new HashSet<>();pools.forEach(p->ids.add(p.getAsJsonObject().get("id").getAsString()));boolean changed=false;
  for(var p:CompatPresets.bundled("base-boss-presets.json").getAsJsonArray("pools"))if(ids.add(p.getAsJsonObject().get("id").getAsString())){pools.add(p.deepCopy());changed=true;}
  // Optional Cataclysm Spellbooks prizes belong to their boss rather than ordinary rolls.
  String[][] extras={{"cataclysm/ignis","cataclysm_spellbooks:ignis_spell_book"},{"cataclysm/the_leviathan","cataclysm_spellbooks:abyss_spell_book"},{"cataclysm/ancient_remnant","cataclysm_spellbooks:desert_spell_book"},{"cataclysm/maledictus","cataclysm_spellbooks:codex_of_malice_spell_book"}};
  var seen=root.has("base_compat_extras")?root.getAsJsonArray("base_compat_extras"):new JsonArray();Set<String> handled=new HashSet<>();seen.forEach(v->handled.add(v.getAsString()));
  for(var extra:extras){String token=extra[0]+"|"+extra[1];if(handled.contains(token)||!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.parse(extra[1])))continue;
   var target=pools.asList().stream().map(JsonElement::getAsJsonObject).filter(p->p.get("id").getAsString().equals(extra[0])).findFirst();if(target.isEmpty())continue;
   var items=target.get().getAsJsonArray("items");if(items.asList().stream().noneMatch(v->v.getAsJsonObject().get("item").getAsString().equals(extra[1]))){var item=new JsonObject();item.addProperty("item",extra[1]);item.addProperty("category","accessory");item.addProperty("weight",1);items.add(item);}seen.add(token);handled.add(token);changed=true;
  }root.add("base_compat_extras",seen);
  // Only wearable ring/necklace IDs exist in Iron's Jewelry 2.0.2; remove invalid older reward entries too.
  for(var p:pools){var list=p.getAsJsonObject().getAsJsonArray("items");for(int i=list.size()-1;i>=0;i--){String id=list.get(i).getAsJsonObject().get("item").getAsString();if(id.startsWith("irons_jewelry:")&&!Set.of("irons_jewelry:ring","irons_jewelry:necklace").contains(id)){list.remove(i);changed=true;}}}
  if(!changed)return true;BossRewards.parse(root);Path backup=path.resolveSibling("boss-rewards.json.before-1.1.3.bak");if(!Files.exists(backup))Files.copy(path,backup);Path temp=path.resolveSibling("boss-rewards.json.basecompat.tmp");Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(root));Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);return BossRewards.reload();
 }catch(Exception ex){dev.mikaaah.tnkloot.TnkLoot.LOG.error("New boss preset migration failed",ex);return false;}}
}
