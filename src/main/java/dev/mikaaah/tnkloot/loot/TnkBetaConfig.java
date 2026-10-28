package dev.mikaaah.tnkloot.loot;
import com.google.gson.*;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.*;
public final class TnkBetaConfig {
 public double equipmentDropChance=0.02;
 public double chestEquipmentChance=0.02;
 public int[] rarityWeights={6000,2500,1000,400,90,10};
 public static TnkBetaConfig current=new TnkBetaConfig();
 public static void load(){try{var path=FMLPaths.CONFIGDIR.get().resolve("tnkloot/tnk-beta.json");Files.createDirectories(path.getParent());Gson g=new GsonBuilder().setPrettyPrinting().create();if(!Files.exists(path))Files.writeString(path,g.toJson(current));var c=g.fromJson(Files.readString(path),TnkBetaConfig.class);if(!Double.isFinite(c.equipmentDropChance)||!Double.isFinite(c.chestEquipmentChance)||c.equipmentDropChance<0||c.equipmentDropChance>1||c.chestEquipmentChance<0||c.chestEquipmentChance>1||c.rarityWeights.length!=6)throw new IllegalArgumentException("Invalid chance/weights");long sum=0;for(int weight:c.rarityWeights){if(weight<=0)throw new IllegalArgumentException("Every rarity needs a positive weight");sum+=weight;}if(sum>Integer.MAX_VALUE)throw new IllegalArgumentException("Weights too large");current=c;}catch(Exception e){dev.mikaaah.tnkloot.TnkLoot.LOG.error("Keeping previous TNK beta config",e);}}
}
