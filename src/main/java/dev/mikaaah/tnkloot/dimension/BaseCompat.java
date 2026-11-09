package dev.mikaaah.tnkloot.dimension;
import com.google.gson.*;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.compat.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.fml.ModList;
import java.nio.file.*;
import java.util.*;
public final class BaseCompat {
 public static final Set<String> NAMESPACES=Set.of("aether","deep_aether","twilightforest","relics","artifacts","reliquified_artifacts","betterend","betternether","starcatcher","quark","ars_elemental","reliquified_irons_spells_and_spellbooks","cataclysm_spellbooks","knightquest","witchery","knavesneeds","farmersdelight","integrated_simply_swords","irons_jewelry","ars_nouveau","legendary_spellbooks","apothic_compats","epicfight","wom","simplyswords","celestisynth","legendary_monsters","cataclysm","mowziesmobs","block_factorys_bosses");
 public static TnkConfig.Category category(Item item){
  if(item instanceof ArmorItem a)return switch(a.getType()){case HELMET->TnkConfig.Category.HELMET;case CHESTPLATE->TnkConfig.Category.CHESTPLATE;case LEGGINGS->TnkConfig.Category.LEGGINGS;case BOOTS->TnkConfig.Category.BOOTS;default->null;};
  if(item instanceof BlockItem)return null;
  if(item instanceof Equipable e){var slot=e.getEquipmentSlot();if(slot==net.minecraft.world.entity.EquipmentSlot.HEAD)return TnkConfig.Category.HELMET;if(slot==net.minecraft.world.entity.EquipmentSlot.CHEST)return TnkConfig.Category.CHESTPLATE;if(slot==net.minecraft.world.entity.EquipmentSlot.LEGS)return TnkConfig.Category.LEGGINGS;if(slot==net.minecraft.world.entity.EquipmentSlot.FEET)return TnkConfig.Category.BOOTS;}
  if(item instanceof ProjectileWeaponItem)return TnkConfig.Category.BOW;
  if(item instanceof SwordItem||item instanceof TridentItem)return TnkConfig.Category.WEAPON;
  if(item instanceof DiggerItem||item instanceof FishingRodItem||item instanceof ShearsItem||item instanceof ShieldItem)return TnkConfig.Category.TOOL;
  String id=BuiltInRegistries.ITEM.getKey(item).toString();if(id.equals("irons_jewelry:ring")||id.equals("irons_jewelry:necklace"))return TnkConfig.Category.ACCESSORY;
  if(new ItemStack(item).getTags().anyMatch(t->t.location().getNamespace().equals("curios")))return TnkConfig.Category.ACCESSORY;
  for(Class<?> c=item.getClass();c!=null;c=c.getSuperclass()){
   String n=c.getSimpleName().toLowerCase(Locale.ROOT);
   if(n.contains("fishingrod")||n.contains("pickarang")||n.contains("trowelitem"))return TnkConfig.Category.TOOL;
   if(n.contains("relicitem")||n.contains("curio")||n.contains("accessory")||n.contains("pendantitem")||n.contains("ringitem")||n.contains("glovesitem")||n.contains("capeitem"))return TnkConfig.Category.ACCESSORY;
   if(n.contains("spellbook")||n.contains("staffitem")||n.contains("scepter")||n.contains("wanditem")||n.contains("spellhorn"))return TnkConfig.Category.WEAPON;
   for(var face:c.getInterfaces())if(face.getName().endsWith("ICurioItem"))return TnkConfig.Category.ACCESSORY;
  }
  return null;
 }
 public static int[] band(String id,Item item,TnkConfig.Category cat){
  String p=id.substring(id.indexOf(':')+1);
  String[] names={"skyroot","holystone","zanite","gravitite","skyjade","stratus","stormforged","ironwood","steeleaf","knightmetal","fiery","arctic","yeti","nether_ruby","cincinnasite","thallasium","terminite","aeternium","crystalite","netherite","diamond","leather","novice","apprentice","archmage","sorcerer","battlemage"};
  int[][] ranges={{0,1},{1,2},{2,4},{4,6},{2,4},{5,7},{6,7},{1,3},{2,4},{4,6},{5,7},{2,4},{5,7},{5,7},{3,5},{3,5},{4,6},{6,7},{5,7},{5,7},{3,6},{0,1},{0,2},{2,4},{5,7},{3,5},{4,7}};
  for(int i=0;i<names.length;i++)if(p.contains(names[i]))return ranges[i];
  if(id.startsWith("irons_jewelry:"))return new int[]{0,7};
  if(cat==TnkConfig.Category.ACCESSORY)return new int[]{2,7};
  if(item instanceof TieredItem tool){float bonus=tool.getTier().getAttackDamageBonus();int min=bonus>=5?6:bonus>=4?5:bonus>=3?3:bonus>=2?2:bonus>=1?1:0;return new int[]{min,Math.min(7,min+3)};}
  if(item instanceof ArmorItem a){int defense=a.getDefense();int min=defense>=7?3:defense>=3?2:1;return new int[]{min,Math.min(7,min+3)};}
  return new int[]{2,6};
 }
 private static boolean excluded(String id){return PresetCleanup.excluded(id)||id.matches(".*(?:^|[:_])gold(?:en)?_.*")||id.contains("creative")||id.contains("debug")||id.contains("spawn_egg")||id.contains("template")||id.contains("empty_");}
 public static synchronized boolean install(){try{
  Path path=TnkConfig.path();JsonObject root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();var weapons=root.getAsJsonArray("weapons");JsonArray kept=new JsonArray();boolean changed=false;
  for(var v:weapons){var w=v.getAsJsonObject();String id=w.get("item").getAsString();var key=ResourceLocation.tryParse(id);boolean invalidJewelry=id.startsWith("irons_jewelry:")&&!Set.of("irons_jewelry:ring","irons_jewelry:necklace").contains(id);
   boolean invalidLoaded=key!=null&&NAMESPACES.contains(key.getNamespace())&&ModList.get().isLoaded(key.getNamespace())&&!BuiltInRegistries.ITEM.containsKey(key);
   if(invalidJewelry||invalidLoaded||excluded(id)){changed=true;continue;}kept.add(v);
  }
  Set<String> existing=new HashSet<>();kept.forEach(v->existing.add(v.getAsJsonObject().get("item").getAsString()));Set<String> seen=new HashSet<>();if(root.has("compat_profiles"))root.getAsJsonArray("compat_profiles").forEach(v->{String key=v.getAsString();if(key.startsWith("live_item@3:"))seen.add(key.substring(12));});int added=0;
  for(Item item:BuiltInRegistries.ITEM){var key=BuiltInRegistries.ITEM.getKey(item);String id=key.toString();if(!NAMESPACES.contains(key.getNamespace())||excluded(id)||(item instanceof BlockItem&&!(item instanceof Equipable))||seen.contains(id))continue;var category=dev.mikaaah.tnkloot.injection.FinalLoot.category(new ItemStack(item));if(category==null)continue;
   seen.add(id);changed=true;if(existing.contains(id)||BossRewards.exclusive(id))continue;int[] band=band(id,item,category);JsonObject w=new JsonObject();w.addProperty("item",id);w.addProperty("category",category.name().toLowerCase(Locale.ROOT));w.addProperty("min_tier",band[0]);w.addProperty("max_tier",band[1]);w.addProperty("weight",1);w.addProperty("enabled",true);kept.add(w);existing.add(id);added++;
  }
  if(!changed)return true;root.add("weapons",kept);var seenJson=root.has("compat_profiles")?root.getAsJsonArray("compat_profiles"):new JsonArray();Set<String> keys=new HashSet<>();seenJson.forEach(v->keys.add(v.getAsString()));seen.stream().sorted().filter(id->keys.add("live_item@3:"+id)).forEach(id->seenJson.add("live_item@3:"+id));root.add("compat_profiles",seenJson);TnkConfig.parse(root.toString());
  Path backup=path.resolveSibling("tiers.json.before-0.12.0.bak");if(!Files.exists(backup))Files.copy(path,backup);Path temp=path.resolveSibling("tiers.json.basecompat.tmp");Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(root));Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);TnkLoot.LOG.info("Installed audited base compatibility: {} new registered equipment types",added);return TnkConfig.reload();
 }catch(Exception ex){TnkLoot.LOG.error("Base compatibility migration failed; previous config retained",ex);return false;}}
}
