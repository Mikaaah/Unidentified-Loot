package dev.mikaaah.tnkloot.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.loot.PoolWeights;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

public final class TnkConfig {
   private static volatile TnkConfig.Settings current = new TnkConfig.Settings(
      TnkConfig.Mode.MANUAL,
      false,
      false,
      false,
      true,
      true,
      List.of(new TnkConfig.Tier(0, "novice", "Novice", List.of(), ResourceLocation.withDefaultNamespace("common"))),
      List.of(),
      new TnkConfig.Costs(false, "minecraft:emerald", 1, Map.of(), Map.of())
   );
   private static Path loadedPath;

   private TnkConfig() {
   }

   public static TnkConfig.Settings get() {
      return current;
   }

   public static Path path() {
      return FMLPaths.CONFIGDIR.get().resolve("tnkloot/tiers.json");
   }

   public static boolean reload() {
      return reload(path());
   }

   public static synchronized boolean reload(Path path) {
      try {
         Files.createDirectories(path.toAbsolutePath().getParent());
         if (Files.notExists(path)) {
            Files.writeString(path, defaultJson(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
         }

         TnkConfig.Settings candidate = parse(Files.readString(path, StandardCharsets.UTF_8));
         current = candidate;
         loadedPath = path;
         TnkLoot.LOG.info("Loaded {} tiers, {} weapons; mode {}", new Object[]{candidate.tiers().size(), candidate.weapons().size(), candidate.mode()});
         return true;
      } catch (RuntimeException | IOException var2) {
         TnkLoot.LOG.error("Invalid config {}; keeping last known-good settings: {}", path, var2.getMessage());
         return false;
      }
   }

   public static synchronized boolean editTier(int rank, String name, ResourceLocation rarity) {
      TnkConfig.Settings s = current;
      List<TnkConfig.Tier> tiers = new ArrayList<>(s.tiers());
      int index = -1;

      for (int i = 0; i < tiers.size(); i++) {
         if (tiers.get(i).rank() == rank) {
            index = i;
         }
      }

      if (index < 0) {
         return false;
      } else {
         TnkConfig.Tier old = tiers.get(index);
         tiers.set(
            index,
            new TnkConfig.Tier(old.rank(), old.id(), name == null ? old.name() : name, old.advancements(), rarity == null ? old.apotheosisRarity() : rarity)
         );
         return save(replace(s, tiers, s.weapons()));
      }
   }

   public static synchronized boolean addTier(String id, String name, ResourceLocation rarity) {
      TnkConfig.Settings s = current;
      int rank = s.highestTier() + 1;
      List<TnkConfig.Tier> tiers = new ArrayList<>(s.tiers());
      tiers.add(new TnkConfig.Tier(rank, id, name, List.of(), rarity));
      List<TnkConfig.Weapon> weapons = new ArrayList<>(s.weapons());

      for (TnkConfig.Weapon template : tierWeapons(s, s.highestTier())) {
         weapons.add(new TnkConfig.Weapon(template.id(), template.category(), rank, rank, template.weight(), template.enabled()));
      }

      return save(replace(s, tiers, weapons));
   }

   public static List<TnkConfig.Weapon> tierWeapons(int rank) {
      return tierWeapons(current, rank);
   }

   public static List<TnkConfig.Weapon> tierWeapons(TnkConfig.Settings settings, int rank) {
      Map<String, TnkConfig.Weapon> result = new LinkedHashMap<>();

      for (TnkConfig.Weapon w : settings.weapons()) {
         if (rank >= w.minTier() && rank <= w.maxTier()) {
            result.put(w.id(), w);
         }
      }

      return List.copyOf(result.values());
   }

   public static synchronized boolean addWeaponToTier(String item, TnkConfig.Category category, int rank) {
      List<TnkConfig.Weapon> existing = tierWeapons(rank);
      if (existing.stream().anyMatch(w -> w.id().equals(item))) {
         return true;
      } else {
         List<TnkConfig.Weapon> peers = existing.stream().filter(w -> w.category() == category && w.enabled()).toList();
         int weight = peers.isEmpty() ? 1 : (int)Math.max(1L, peers.stream().mapToLong(TnkConfig.Weapon::weight).sum() / (long)peers.size());
         return save(replace(current, current.tiers(), putAtRank(current.weapons(), new TnkConfig.Weapon(item, category, rank, rank, weight), rank, false)));
      }
   }

   public static synchronized boolean removeWeaponFromTier(String item, TnkConfig.Category category, int rank) {
      Optional<TnkConfig.Weapon> old = tierWeapons(rank).stream().filter(w -> w.id().equals(item) && w.category() == category).findFirst();
      return old.isPresent() && save(replace(current, current.tiers(), putAtRank(current.weapons(), old.get(), rank, true)));
   }

   public static synchronized boolean enableWeapon(String item, int rank, boolean enabled) {
      Optional<TnkConfig.Weapon> old = tierWeapons(rank).stream().filter(wx -> wx.id().equals(item)).findFirst();
      if (old.isEmpty()) {
         return false;
      } else {
         TnkConfig.Weapon w = old.get();
         return save(
            replace(
               current, current.tiers(), putAtRank(current.weapons(), new TnkConfig.Weapon(w.id(), w.category(), rank, rank, w.weight(), enabled), rank, false)
            )
         );
      }
   }

   static List<TnkConfig.Weapon> putAtRank(List<TnkConfig.Weapon> source, TnkConfig.Weapon replacement, int rank, boolean remove) {
      List<TnkConfig.Weapon> result = new ArrayList<>();

      for (TnkConfig.Weapon w : source) {
         if (w.id().equals(replacement.id()) && w.category() == replacement.category() && rank >= w.minTier() && rank <= w.maxTier()) {
            if (w.minTier() < rank) {
               result.add(w.at(w.minTier(), rank - 1));
            }

            if (w.maxTier() > rank) {
               result.add(w.at(rank + 1, w.maxTier()));
            }
         } else {
            result.add(w);
         }
      }

      if (!remove) {
         result.add(replacement.at(rank, rank));
      }

      return result;
   }

   public static synchronized boolean equalChances(int rank, TnkConfig.Category category) {
      List<TnkConfig.Weapon> result = new ArrayList<>(current.weapons());

      for (TnkConfig.Weapon w : tierWeapons(rank)) {
         if (category == null || w.category() == category) {
            result = putAtRank(result, new TnkConfig.Weapon(w.id(), w.category(), rank, rank, 1, w.enabled()), rank, false);
         }
      }

      return save(replace(current, current.tiers(), result));
   }

   public static synchronized boolean setChance(String item, int rank, int basisPoints, Predicate<TnkConfig.Weapon> available) {
      if (basisPoints >= 0 && basisPoints <= 10000) {
         Optional<TnkConfig.Weapon> selected = tierWeapons(rank).stream().filter(wx -> wx.id().equals(item)).findFirst();
         if (selected.isEmpty()) {
            return false;
         } else if (basisPoints == 0) {
            return enableWeapon(item, rank, false);
         } else {
            List<TnkConfig.Weapon> peers = tierWeapons(rank)
               .stream()
               .filter(wx -> wx.category() == selected.get().category() && (wx.id().equals(item) || wx.enabled()) && available.test(wx))
               .toList();
            int index = -1;

            for (int i = 0; i < peers.size(); i++) {
               if (peers.get(i).id().equals(item)) {
                  index = i;
               }
            }

            if (index < 0) {
               return false;
            } else {
               int[] weights;
               if (basisPoints == 10000) {
                  weights = new int[peers.size()];
                  weights[index] = 1;
               } else {
                  try {
                     weights = PoolWeights.setPercent(peers.stream().map(TnkConfig.Weapon::weight).toList(), index, basisPoints);
                  } catch (IllegalArgumentException var11) {
                     return false;
                  }
               }

               List<TnkConfig.Weapon> result = new ArrayList<>(current.weapons());

               for (int ix = 0; ix < peers.size(); ix++) {
                  TnkConfig.Weapon w = peers.get(ix);
                  result = putAtRank(result, new TnkConfig.Weapon(w.id(), w.category(), rank, rank, Math.max(1, weights[ix]), weights[ix] > 0), rank, false);
               }

               return save(replace(current, current.tiers(), result));
            }
         }
      } else {
         return false;
      }
   }

   private static TnkConfig.Settings replace(TnkConfig.Settings s, List<TnkConfig.Tier> tiers, List<TnkConfig.Weapon> weapons) {
      return new TnkConfig.Settings(
         s.mode(),
         s.shareFtbTeams(),
         s.enableIdentification(),
         s.replaceWeaponLoot(),
         s.handheldIdentification(),
         s.stationIdentification(),
         List.copyOf(tiers),
         List.copyOf(weapons),
         s.costs()
      );
   }

   private static boolean save(TnkConfig.Settings candidate) {
      try {
         JsonObject root = new JsonObject();
         Path active = loadedPath == null ? path() : loadedPath;
         if (Files.exists(active)) {
            JsonObject prior = JsonParser.parseString(Files.readString(active)).getAsJsonObject();

            for (String key : List.of("preset_revision", "compat_profiles", "tnk_release_revision", "tnk_pack_compat_revision")) {
               if (prior.has(key)) {
                  root.add(key, prior.get(key).deepCopy());
               }
            }
         }

         root.addProperty("progression_mode", candidate.mode().name());
         root.addProperty("share_ftb_teams", candidate.shareFtbTeams());
         root.addProperty("identification_enabled", candidate.enableIdentification());
         root.addProperty("replace_weapon_loot", candidate.replaceWeaponLoot());
         root.addProperty("handheld_identification", candidate.handheldIdentification());
         root.addProperty("station_identification", candidate.stationIdentification());
         JsonObject costs = new JsonObject();
         costs.addProperty("enabled", candidate.costs().enabled());
         costs.addProperty("item", candidate.costs().item());
         costs.addProperty("default", candidate.costs().defaultAmount());
         JsonObject perTier = new JsonObject();
         candidate.costs().tierAmounts().forEach((k, v) -> perTier.addProperty(k.toString(), v));
         JsonObject perItem = new JsonObject();
         candidate.costs().itemAmounts().forEach((k, v) -> perItem.addProperty(k, v));
         costs.add("per_tier", perTier);
         costs.add("per_item", perItem);
         root.add("identification_costs", costs);
         JsonArray tierArray = new JsonArray();
         candidate.tiers().forEach(t -> {
            JsonObject o = new JsonObject();
            o.addProperty("rank", t.rank());
            o.addProperty("id", t.id());
            o.addProperty("name", t.name());
            JsonArray adv = new JsonArray();
            t.advancements().forEach(a -> adv.add(a.toString()));
            o.add("advancements", adv);
            o.addProperty("apotheosis_rarity", t.apotheosisRarity().toString());
            tierArray.add(o);
         });
         root.add("tiers", tierArray);
         JsonArray weaponArray = new JsonArray();
         candidate.weapons().forEach(w -> {
            JsonObject o = new JsonObject();
            o.addProperty("item", w.id());
            o.addProperty("category", w.category().name().toLowerCase(Locale.ROOT));
            o.addProperty("min_tier", w.minTier());
            o.addProperty("max_tier", w.maxTier());
            o.addProperty("weight", w.weight());
            o.addProperty("enabled", w.enabled());
            weaponArray.add(o);
         });
         root.add("weapons", weaponArray);
         TnkConfig.Settings validated = parse(new GsonBuilder().setPrettyPrinting().create().toJson(root));
         Path target = loadedPath == null ? path() : loadedPath;
         Files.createDirectories(target.toAbsolutePath().getParent());
         Path tmp = target.resolveSibling("tiers.json.tmp");
         Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
         Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         current = validated;
         return true;
      } catch (Exception var11) {
         TnkLoot.LOG.error("Could not save tier editor changes: {}", var11.getMessage());
         return false;
      }
   }

   public static String defaultJson() throws IOException {
      String var1;
      try (InputStream stream = TnkConfig.class.getResourceAsStream("/default-tiers.json")) {
         if (stream == null) {
            throw new IOException("Bundled default-tiers.json missing");
         }

         var1 = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      }

      return var1;
   }

   public static String tierSnapshot() {
      JsonArray array = new JsonArray();
      current.tiers().forEach(t -> {
         JsonObject o = new JsonObject();
         o.addProperty("rank", t.rank());
         o.addProperty("id", t.id());
         o.addProperty("name", t.name());
         o.addProperty("apotheosis_rarity", t.apotheosisRarity().toString());
         array.add(o);
      });
      return array.toString();
   }

   public static List<TnkConfig.Tier> parseTierSnapshot(String json) {
      List<TnkConfig.Tier> result = new ArrayList<>();

      for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) {
         JsonObject o = e.getAsJsonObject();
         result.add(
            new TnkConfig.Tier(
               o.get("rank").getAsInt(),
               o.get("id").getAsString(),
               o.get("name").getAsString(),
               List.of(),
               ResourceLocation.parse(o.get("apotheosis_rarity").getAsString())
            )
         );
      }

      return List.copyOf(result);
   }

   public static TnkConfig.Settings parse(String json) {
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      keys(
         root,
         "progression_mode",
         "share_ftb_teams",
         "identification_enabled",
         "replace_weapon_loot",
         "handheld_identification",
         "station_identification",
         "identification_costs",
         "tiers",
         "weapons",
         "preset_revision",
         "compat_profiles",
         "tnk_release_revision",
         "tnk_pack_compat_revision"
      );
      TnkConfig.Mode mode = TnkConfig.Mode.valueOf(string(root, "progression_mode", "ADVANCEMENTS").toUpperCase(Locale.ROOT));
      boolean share = bool(root, "share_ftb_teams", true);
      boolean identification = bool(root, "identification_enabled", true);
      boolean replaceWeaponLoot = bool(root, "replace_weapon_loot", true);
      boolean handheld = bool(root, "handheld_identification", true);
      boolean station = bool(root, "station_identification", true);
      TnkConfig.Costs costs = parseCosts(root.has("identification_costs") ? root.getAsJsonObject("identification_costs") : new JsonObject());
      List<TnkConfig.Tier> tiers = new ArrayList<>();
      Set<Integer> ranks = new HashSet<>();
      Set<String> ids = new HashSet<>();

      for (JsonElement element : root.getAsJsonArray("tiers")) {
         JsonObject e = element.getAsJsonObject();
         keys(e, "rank", "id", "name", "advancements", "apotheosis_rarity");
         int rank = integer(e, "rank", null);
         String id = string(e, "id", null).toLowerCase(Locale.ROOT);
         if (rank < 0 || rank == Integer.MAX_VALUE || !id.matches("[a-z0-9_]+") || !ranks.add(rank) || !ids.add(id)) {
            throw new IllegalArgumentException("Invalid or duplicate tier rank/id: " + e);
         }

         List<ResourceLocation> advancements = new ArrayList<>();
         if (e.has("advancements")) {
            for (JsonElement adv : e.getAsJsonArray("advancements")) {
               if (!adv.isJsonPrimitive() || !adv.getAsJsonPrimitive().isString()) {
                  throw new IllegalArgumentException("Advancement ID must be a string");
               }

               ResourceLocation key = ResourceLocation.tryParse(adv.getAsString());
               if (key == null) {
                  throw new IllegalArgumentException("Invalid advancement ID: " + adv);
               }

               advancements.add(key);
            }
         }

         String name = string(e, "name", id);
         if (name.isBlank()) {
            throw new IllegalArgumentException("Tier name cannot be blank");
         }

         String rarityValue = string(e, "apotheosis_rarity", defaultApotheosisRarity(rank));
         ResourceLocation apotheosisRarity = ResourceLocation.tryParse(rarityValue);
         if (apotheosisRarity == null) {
            throw new IllegalArgumentException("Invalid Apotheosis rarity ID: " + rarityValue);
         }

         tiers.add(new TnkConfig.Tier(rank, id, name, List.copyOf(advancements), apotheosisRarity));
      }

      tiers.sort(Comparator.comparingInt(TnkConfig.Tier::rank));
      if (!tiers.isEmpty() && tiers.getFirst().rank() == 0) {
         for (int rankx : costs.tierAmounts().keySet()) {
            if (!ranks.contains(rankx)) {
               throw new IllegalArgumentException("Identification cost references unknown tier rank " + rankx);
            }
         }

         List<TnkConfig.Weapon> weapons = new ArrayList<>();
         Map<String, TnkConfig.Category> itemCategories = new HashMap<>();

         for (JsonElement element : root.getAsJsonArray("weapons")) {
            JsonObject ex = element.getAsJsonObject();
            keys(ex, "item", "category", "min_tier", "max_tier", "weight", "enabled");
            String idx = string(ex, "item", null);
            if (ResourceLocation.tryParse(idx) == null) {
               throw new IllegalArgumentException("Invalid item ID: " + idx);
            }

            TnkConfig.Category category = TnkConfig.Category.parse(string(ex, "category", "weapon"));
            TnkConfig.Category prior = itemCategories.putIfAbsent(idx, category);
            if (prior != null && prior != category) {
               throw new IllegalArgumentException("Item is assigned to multiple categories: " + idx);
            }

            int min = integer(ex, "min_tier", null);
            int max = integer(ex, "max_tier", Integer.MAX_VALUE);
            int weight = integer(ex, "weight", 1);
            if (min < 0 || max < min || weight < 1) {
               throw new IllegalArgumentException("Invalid tier range or weight: " + ex);
            }

            weapons.add(new TnkConfig.Weapon(idx, category, min, max, weight, bool(ex, "enabled", true)));
         }

         return new TnkConfig.Settings(mode, share, identification, replaceWeaponLoot, handheld, station, List.copyOf(tiers), List.copyOf(weapons), costs);
      } else {
         throw new IllegalArgumentException("A rank-0 tier is required");
      }
   }

   private static TnkConfig.Costs parseCosts(JsonObject obj) {
      keys(obj, "enabled", "item", "default", "per_tier", "per_item");
      boolean enabled = bool(obj, "enabled", true);
      String item = string(obj, "item", "minecraft:emerald");
      if (ResourceLocation.tryParse(item) == null) {
         throw new IllegalArgumentException("Invalid identification cost item: " + item);
      } else {
         int fallback = integer(obj, "default", 1);
         if (fallback < 0) {
            throw new IllegalArgumentException("Identification cost cannot be negative");
         } else {
            Map<Integer, Integer> tiers = new HashMap<>();
            if (obj.has("per_tier")) {
               for (Entry<String, JsonElement> e : obj.getAsJsonObject("per_tier").entrySet()) {
                  int rank = Integer.parseInt(e.getKey());
                  int amount = e.getValue().getAsBigDecimal().intValueExact();
                  if (rank < 0 || amount < 0) {
                     throw new IllegalArgumentException("Invalid per-tier identification cost");
                  }

                  tiers.put(rank, amount);
               }
            }

            Map<String, Integer> items = new HashMap<>();
            if (obj.has("per_item")) {
               for (Entry<String, JsonElement> e : obj.getAsJsonObject("per_item").entrySet()) {
                  if (ResourceLocation.tryParse(e.getKey()) == null || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isNumber()) {
                     throw new IllegalArgumentException("Invalid per-item identification cost: " + e.getKey());
                  }

                  int amount = e.getValue().getAsBigDecimal().intValueExact();
                  if (amount < 0) {
                     throw new IllegalArgumentException("Identification cost cannot be negative");
                  }

                  items.put(e.getKey(), amount);
               }
            }

            return new TnkConfig.Costs(enabled, item, fallback, Map.copyOf(tiers), Map.copyOf(items));
         }
      }
   }

   private static String defaultApotheosisRarity(int rank) {
      return switch (rank) {
         case 0 -> "tnkloot:common";
         case 1 -> "tnkloot:uncommon";
         case 2 -> "tnkloot:rare";
         case 3 -> "tnkloot:epic";
         case 4 -> "tnkloot:legendary";
         case 5, 6 -> "tnkloot:mythic";
         default -> "tnkloot:ancient";
      };
   }

   private static void keys(JsonObject object, String... allowed) {
      Set<String> accepted = Set.of(allowed);

      for (String key : object.keySet()) {
         if (!accepted.contains(key)) {
            throw new IllegalArgumentException("Unknown config key: " + key);
         }
      }
   }

   private static JsonPrimitive primitive(JsonObject object, String key) {
      JsonElement value = object.get(key);
      if (value != null && value.isJsonPrimitive()) {
         return value.getAsJsonPrimitive();
      } else {
         throw new IllegalArgumentException("Missing or invalid " + key);
      }
   }

   private static boolean bool(JsonObject object, String key, boolean fallback) {
      if (!object.has(key)) {
         return fallback;
      } else {
         JsonPrimitive value = primitive(object, key);
         if (!value.isBoolean()) {
            throw new IllegalArgumentException(key + " must be a boolean");
         } else {
            return value.getAsBoolean();
         }
      }
   }

   private static String string(JsonObject object, String key, String fallback) {
      if (!object.has(key) && fallback != null) {
         return fallback;
      } else {
         JsonPrimitive value = primitive(object, key);
         if (!value.isString()) {
            throw new IllegalArgumentException(key + " must be a string");
         } else {
            return value.getAsString();
         }
      }
   }

   private static int integer(JsonObject object, String key, Integer fallback) {
      if (!object.has(key) && fallback != null) {
         return fallback;
      } else {
         JsonPrimitive value = primitive(object, key);
         if (!value.isNumber()) {
            throw new IllegalArgumentException(key + " must be an integer");
         } else {
            return value.getAsBigDecimal().intValueExact();
         }
      }
   }

   public static enum Category {
      WEAPON("unidentified_weapon"),
      BOW("unidentified_bow"),
      HELMET("unidentified_helmet"),
      CHESTPLATE("unidentified_chestplate"),
      LEGGINGS("unidentified_leggings"),
      BOOTS("unidentified_boots"),
      TOOL("unidentified_tool"),
      ACCESSORY("unidentified_accessory");

      private final String token;

      private Category(String token) {
         this.token = token;
      }

      public String token() {
         return this.token;
      }

      public static TnkConfig.Category parse(String value) {
         return valueOf(value.toUpperCase(Locale.ROOT));
      }
   }

   public static record Costs(boolean enabled, String item, int defaultAmount, Map<Integer, Integer> tierAmounts, Map<String, Integer> itemAmounts) {
      public int amount(int tier, String itemId) {
         return this.itemAmounts.getOrDefault(itemId, this.tierAmounts.getOrDefault(tier, this.defaultAmount));
      }
   }

   public static enum Mode {
      ADVANCEMENTS,
      FTB_QUESTS,
      MANUAL,
      OFF;
   }

   public static record Settings(
      TnkConfig.Mode mode,
      boolean shareFtbTeams,
      boolean enableIdentification,
      boolean replaceWeaponLoot,
      boolean handheldIdentification,
      boolean stationIdentification,
      List<TnkConfig.Tier> tiers,
      List<TnkConfig.Weapon> weapons,
      TnkConfig.Costs costs
   ) {
      public int highestTier() {
         return this.tiers.getLast().rank();
      }

      public Optional<TnkConfig.Tier> tierForName(String id) {
         return this.tiers.stream().filter(t -> t.id().equalsIgnoreCase(id)).findFirst();
      }

      public TnkConfig.Tier tierForRank(int rank) {
         return this.tiers.stream().filter(t -> t.rank() <= rank).max(Comparator.comparingInt(TnkConfig.Tier::rank)).orElse(this.tiers.getFirst());
      }
   }

   public static record Tier(int rank, String id, String name, List<ResourceLocation> advancements, ResourceLocation apotheosisRarity) {
   }

   public static record Weapon(String id, TnkConfig.Category category, int minTier, int maxTier, int weight, boolean enabled) {
      public Weapon(String id, TnkConfig.Category category, int minTier, int maxTier, int weight) {
         this(id, category, minTier, maxTier, weight, true);
      }

      public TnkConfig.Weapon at(int min, int max) {
         return new TnkConfig.Weapon(this.id, this.category, min, max, this.weight, this.enabled);
      }
   }
}
