package dev.mikaaah.tnkloot.rarity;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Rarity selection is independent of equipment tier; only affix levels depend on tier. */
public final class RarityPolicy {
    public record Quality(String id, String name, int color, long weight, long bossWeight, double cost, String apotheosis, double floor) {}
    public record Band(double min, double max) {}
    public record Settings(List<Quality> qualities, Map<Integer,Map<String,Long>> weights, List<Band> bands) {
        public Quality quality(String id) { String normalized=id.startsWith("tnkloot:")?id.substring(8):id; String key=normalized.equals("ancient")?"mythic":normalized; return qualities.stream().filter(q -> q.id().equals(key)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown rarity " + id)); }
        public Quality roll(int tier, boolean boss, long seed) {
            Map<String,Long> overrides = weights.getOrDefault(tier, Map.of());
            long total = 0;
            for (Quality q : qualities) total = Math.addExact(total, boss ? q.bossWeight() : overrides.getOrDefault(q.id(), q.weight()));
            if (total <= 0) throw new IllegalStateException("Empty rarity distribution");
            long ticket = new Random(seed ^ 0x524152495459L).nextLong(total);
            for (Quality q : qualities) {
                long weight = boss ? q.bossWeight() : overrides.getOrDefault(q.id(), q.weight());
                if (ticket < weight) return q;
                ticket -= weight;
            }
            throw new AssertionError();
        }
        public float level(int tier, Quality q, Random random) {
            Band b = bands.get(Math.max(0, Math.min(bands.size()-1, tier)));
            return (float)(b.min() + (b.max()-b.min()) * (q.floor() + random.nextDouble()*(1-q.floor())));
        }
    }
    private static volatile Settings current = defaults();
    private RarityPolicy() {}
    public static Settings get() { return current; }
    public static Settings defaults() {
        try (var stream = RarityPolicy.class.getResourceAsStream("/default-rarities.json")) {
            if (stream == null) throw new IllegalStateException("Missing rarity defaults");
            return parse(JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
    public static boolean reload(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                try (var stream = RarityPolicy.class.getResourceAsStream("/default-rarities.json")) { Files.copy(stream, path); }
            }
            JsonObject root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            boolean migrated=false;
            if(root.getAsJsonArray("qualities").asList().stream().noneMatch(v->v.getAsJsonObject().get("id").getAsString().equals("mythic"))) {
                migrated=true;
                try(var stream=RarityPolicy.class.getResourceAsStream("/default-rarities.json")) {
                    var defaults=JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject();
                    root.getAsJsonArray("qualities").add(defaults.getAsJsonArray("qualities").asList().stream().filter(v->v.getAsJsonObject().get("id").getAsString().equals("mythic")).findFirst().orElseThrow());
                }
                // Update only the former stock colors; preserve custom colors and weights.
                for(var value:root.getAsJsonArray("qualities")) {
                    var q=value.getAsJsonObject();String id=q.get("id").getAsString(),old=q.get("color").getAsString();
                    if(id.equals("common")&&old.equalsIgnoreCase("#AAAAAA"))q.addProperty("color","#FFFFFF");
                    if(id.equals("rare")&&old.equalsIgnoreCase("#5555FF"))q.addProperty("color","#FF77CC");
                    if(id.equals("legendary")&&old.equalsIgnoreCase("#00FFFF"))q.addProperty("color","#55DDFF");
                }

            }
            Settings next = parse(root);
            if(migrated){
                Path backup=path.resolveSibling("rarities.json.before-mythic.bak");
                if(!Files.exists(backup))Files.copy(path,backup);
                Path temp=path.resolveSibling("rarities.json.migration.tmp");
                Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(root));
                Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            }
            current = next; return true;
        } catch (Exception ex) { System.err.println("[TNK Loot] Invalid rarities.json; previous rarity settings retained: " + ex.getMessage()); return false; }
    }
    private static long weight(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Weight must be an integer");
        long result = value.getAsBigDecimal().longValueExact();
        if (result < 0 || result > 1_000_000_000L) throw new IllegalArgumentException("Rarity weight out of range"); return result;
    }
    public static Settings parse(JsonObject root) {
        List<Quality> qualities = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (JsonElement value : root.getAsJsonArray("qualities")) {
            JsonObject q = value.getAsJsonObject(); String id = q.get("id").getAsString();
            if (!Set.of("common","uncommon","rare","epic","legendary","mythic").contains(id) || !ids.add(id)) throw new IllegalArgumentException("Invalid or duplicate rarity ID");
            String color = q.get("color").getAsString(), apo = q.get("apotheosis_rarity").getAsString(), name = q.get("name").getAsString();
            double cost = q.get("cost_multiplier").getAsDouble(), floor = q.get("roll_floor").getAsDouble();
            if (!color.matches("#[0-9a-fA-F]{6}") || !apo.matches("[a-z0-9_.-]+:[a-z0-9/_.-]+") || name.isBlank() || !Double.isFinite(cost) || cost < 0 || cost > 100 || !Double.isFinite(floor) || floor < 0 || floor > 1) throw new IllegalArgumentException("Invalid rarity properties " + id);
            qualities.add(new Quality(id,name,Integer.parseInt(color.substring(1),16),weight(q.get("weight")),weight(q.get("boss_weight")),cost,apo,floor));
        }
        if (ids.size() != 6 || qualities.stream().mapToLong(Quality::weight).sum()==0 || qualities.stream().mapToLong(Quality::bossWeight).sum()==0) throw new IllegalArgumentException("All six rarities and nonempty distributions are required");
        List<Band> bands = new ArrayList<>();
        for (var value : root.getAsJsonArray("tier_affix_bands")) {
            var b = value.getAsJsonObject();double min=b.get("min").getAsDouble(), max=b.get("max").getAsDouble();
            if (!Double.isFinite(min)||!Double.isFinite(max)||min<0||max>1||min>max) throw new IllegalArgumentException("Invalid affix band");
            bands.add(new Band(min,max));
        }
        if (bands.isEmpty()) throw new IllegalArgumentException("No tier bands");
        Map<Integer,Map<String,Long>> weights = new HashMap<>();
        for (var entry : root.getAsJsonObject("per_tier_weights").entrySet()) {
            int tier = Integer.parseInt(entry.getKey()); if(tier<0) throw new IllegalArgumentException("Invalid tier");
            Map<String,Long> map=new HashMap<>();
            for (var e : entry.getValue().getAsJsonObject().entrySet()) { if(!ids.contains(e.getKey())) throw new IllegalArgumentException("Unknown rarity override"); map.put(e.getKey(),weight(e.getValue())); }
            if (qualities.stream().mapToLong(q -> map.getOrDefault(q.id(),q.weight())).sum()==0) throw new IllegalArgumentException("Empty tier weights");
            weights.put(tier,Map.copyOf(map));
        }
        return new Settings(List.copyOf(qualities),Map.copyOf(weights),List.copyOf(bands));
    }
    public static int cost(int base, double multiplier) {
        if (base == 0) return 0;
        return (int)Math.min(Integer.MAX_VALUE,Math.ceil(base*multiplier));
    }
}
