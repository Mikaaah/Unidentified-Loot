package dev.mikaaah.tnkloot.compat;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class CompatPresets {
   private CompatPresets() {
   }

   public static JsonObject bundled(String name) {
      try {
         JsonObject var2;
         try (InputStream in = CompatPresets.class.getResourceAsStream("/" + name)) {
            if (in == null) {
               throw new IllegalStateException("Missing " + name);
            }

            var2 = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
         }

         return var2;
      } catch (IOException var6) {
         throw new UncheckedIOException(var6);
      }
   }

   public static void started(ServerStartedEvent event) {
      PackItemPolicy.reload();
      RaritySupport.reload();
      install();
      BossRewards.reload();
   }

   public static boolean install() {
      Path path = TnkConfig.path();

      try {
         JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
         if (root.getAsJsonArray("weapons").asList().stream().anyMatch(value -> {
            String id = value.getAsJsonObject().get("item").getAsString();
            return id.startsWith("efn:") || PresetCleanup.requiresNativePayload(id);
         })) {
            Path backup = path.resolveSibling("tiers.json.before-1.1.3.bak");
            if (!Files.exists(backup)) Files.copy(path, backup);
         }
         JsonArray weapons = root.getAsJsonArray("weapons");
         boolean changed = PresetCleanup.sanitize(root);
         if (!root.has("preset_revision") || root.get("preset_revision").getAsInt() < 2) {
            weapons = migrateVanilla(root, weapons);
            root.add("weapons", weapons);
            root.addProperty("preset_revision", 2);
            changed = true;
         }

         JsonArray installed = root.has("compat_profiles") ? root.getAsJsonArray("compat_profiles") : new JsonArray();
         Set<String> done = new HashSet<>();
         installed.forEach(v -> done.add(v.getAsString()));

         for (JsonElement element : bundled("compat-presets.json").getAsJsonArray("profiles")) {
            JsonObject profile = element.getAsJsonObject();
            String key = profile.get("id").getAsString() + "@2";
            if (!done.contains(key)) {
               boolean loaded = true;

               for (JsonElement mod : profile.getAsJsonArray("requires")) {
                  loaded &= ModList.get().isLoaded(mod.getAsString());
               }

               if (loaded) {
                  int added = 0;
                  Set<String> existing = new HashSet<>();
                  weapons.forEach(wx -> existing.add(wx.getAsJsonObject().get("item").getAsString()));

                  for (JsonElement item : profile.getAsJsonArray("items")) {
                     JsonObject w = item.getAsJsonObject();
                     String id = w.get("item").getAsString();
                     if (BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)) && existing.add(id)) {
                        weapons.add(w.deepCopy());
                        added++;
                     }
                  }

                  installed.add(key);
                  changed = true;
                  TnkLoot.LOG.info("Installed compatibility {}: {} registered equipment types", key, added);
               }
            }
         }

         root.add("compat_profiles", installed);
         if (!changed) {
            return true;
         } else {
            TnkConfig.parse(root.toString());
            Path backup = path.resolveSibling("tiers.json.before-0.8.1.bak");
            if (!Files.exists(backup)) {
               Files.copy(path, backup);
            }

            Path tmp = path.resolveSibling("tiers.json.compat.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return TnkConfig.reload();
         }
      } catch (Exception var17) {
         TnkLoot.LOG.error("Could not install compatibility presets; existing config retained", var17);
         return false;
      }
   }

   public static JsonArray migrateVanilla(JsonObject root, JsonArray source) {
      Map<String, JsonObject> policy = new HashMap<>();
      bundled("default-tiers.json").getAsJsonArray("weapons").forEach(v -> policy.put(v.getAsJsonObject().get("item").getAsString(), v.getAsJsonObject()));
      int highest = root.getAsJsonArray("tiers").asList().stream().mapToInt(v -> v.getAsJsonObject().get("rank").getAsInt()).max().orElse(7);
      JsonArray result = new JsonArray();

      for (JsonElement element : source) {
         JsonObject old = element.getAsJsonObject();
         JsonObject rule = policy.get(old.get("item").getAsString());
         if (rule == null) {
            result.add(old.deepCopy());
         } else {
            int min = Math.max(old.get("min_tier").getAsInt(), rule.get("min_tier").getAsInt());
            int cap = old.get("item").getAsString().startsWith("minecraft:netherite_") ? Math.max(7, highest) : rule.get("max_tier").getAsInt();
            int max = Math.min(old.has("max_tier") ? old.get("max_tier").getAsInt() : Integer.MAX_VALUE, cap);
            if (min <= max) {
               JsonObject copy = old.deepCopy();
               copy.addProperty("min_tier", min);
               copy.addProperty("max_tier", max);
               result.add(copy);
            }
         }
      }

      return result;
   }
}
