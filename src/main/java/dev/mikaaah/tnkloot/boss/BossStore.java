package dev.mikaaah.tnkloot.boss;
import com.google.gson.*;
import dev.mikaaah.tnkloot.compat.*;
import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

public final class BossStore {
    private BossStore(){}
    public static List<BossRewards.Pool> parse(JsonObject root){
        List<BossRewards.Pool> pools=new ArrayList<>();Set<String> ids=new HashSet<>();
        if(!root.has("enabled")||!root.has("require_player_kill"))throw new IllegalArgumentException("Missing boss settings");
        root.get("enabled").getAsBoolean();root.get("require_player_kill").getAsBoolean();
        for(var value:root.getAsJsonArray("pools")){
            var p=value.getAsJsonObject();String id=p.get("id").getAsString(),entity=p.get("entity").getAsString();
            int tier=p.get("tier").getAsInt();double chance=p.get("chance").getAsDouble();
            if(!ids.add(id)||!id.matches("[a-z0-9_/.-]+")||ResourceLocation.tryParse(entity)==null||tier<0||!Double.isFinite(chance)||chance<0||chance>1)throw new IllegalArgumentException("Invalid boss "+id);
            List<BossRewards.Reward> items=new ArrayList<>();Set<String> entries=new HashSet<>();
            for(var item:p.getAsJsonArray("items")){
                var w=item.getAsJsonObject();String key=w.get("item").getAsString();int weight=w.get("weight").getAsInt();
                if(ResourceLocation.tryParse(key)==null||weight<1||weight>1_000_000||!entries.add(key))throw new IllegalArgumentException("Invalid boss item "+key);
                var category=TnkConfig.Category.parse(w.get("category").getAsString());
                if(!w.has("enabled")||w.get("enabled").getAsBoolean())items.add(new BossRewards.Reward(key,category,weight));
            }
            pools.add(new BossRewards.Pool(id,p.get("mod").getAsString(),entity,p.get("name").getAsString(),tier,p.get("enabled").getAsBoolean(),chance,p.get("additional_if_no_equipment").getAsBoolean(),List.copyOf(items)));
        }
        return List.copyOf(pools);
    }
    public static JsonObject root(){try{return JsonParser.parseString(Files.readString(BossRewards.path())).getAsJsonObject();}catch(Exception ex){throw new IllegalStateException("Could not read boss pools",ex);}}
    public static JsonObject pool(String id){return root().getAsJsonArray("pools").asList().stream().map(JsonElement::getAsJsonObject).filter(p->p.get("id").getAsString().equals(id)).findFirst().orElseThrow();}
    public static synchronized void edit(String id,Consumer<JsonObject> change){
        JsonObject root=root();JsonObject p=root.getAsJsonArray("pools").asList().stream().map(JsonElement::getAsJsonObject).filter(v->v.get("id").getAsString().equals(id)).findFirst().orElseThrow();change.accept(p);save(root);
    }
    private static void save(JsonObject root){
        parse(root);Path path=BossRewards.path();
        try{
            Path backup=path.resolveSibling("boss-rewards.json.before-editor.bak");if(!Files.exists(backup))Files.copy(path,backup);
            Path tmp=path.resolveSibling("boss-rewards.json.editor.tmp");Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(root));Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            if(!BossRewards.reload())throw new IllegalStateException("Boss reload failed");
        }catch(java.io.IOException ex){throw new IllegalStateException("Could not save boss pools",ex);}
    }
    public static String add(String entity){
        ResourceLocation key=ResourceLocation.tryParse(entity);
        if(key==null||!BuiltInRegistries.ENTITY_TYPE.containsKey(key)||entity.equals("minecraft:player"))throw new IllegalArgumentException("Enter a registered mob ID: modid:entity");
        JsonObject root=root();String id=key.getNamespace()+"/"+key.getPath();
        if(root.getAsJsonArray("pools").asList().stream().anyMatch(v->v.getAsJsonObject().get("entity").getAsString().equals(entity)))throw new IllegalArgumentException("That entity already has a boss pool");
        JsonObject p=new JsonObject();p.addProperty("id",id);p.addProperty("mod",key.getNamespace());p.addProperty("entity",entity);p.addProperty("name",Arrays.stream(key.getPath().split("_")).map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).reduce((a,b)->a+" "+b).orElse(entity));p.addProperty("tier",0);p.addProperty("enabled",false);p.addProperty("chance",1);p.addProperty("additional_if_no_equipment",true);p.add("items",new JsonArray());root.getAsJsonArray("pools").add(p);save(root);return id;
    }
    public static void importItem(String id,ItemStack stack){
        if(stack.isEmpty()||stack.getItem() instanceof dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem)return;
        String key=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if(dev.mikaaah.tnkloot.compat.PresetCleanup.excluded(key))throw new IllegalArgumentException("Gold equipment and wom:claw are excluded");
        edit(id,p->{var items=p.getAsJsonArray("items");if(items.asList().stream().anyMatch(v->v.getAsJsonObject().get("item").getAsString().equals(key)))return;JsonObject w=new JsonObject();w.addProperty("item",key);w.addProperty("category",category(stack).name().toLowerCase(Locale.ROOT));w.addProperty("weight",1);w.addProperty("enabled",true);items.add(w);});
    }
    private static TnkConfig.Category category(ItemStack stack){
        var item=stack.getItem();if(item instanceof ArmorItem armor)return switch(armor.getType()){case HELMET->TnkConfig.Category.HELMET;case CHESTPLATE->TnkConfig.Category.CHESTPLATE;case LEGGINGS->TnkConfig.Category.LEGGINGS;case BOOTS->TnkConfig.Category.BOOTS;default->TnkConfig.Category.ACCESSORY;};
        if(item instanceof BowItem||item instanceof CrossbowItem)return TnkConfig.Category.BOW;if(item instanceof DiggerItem)return TnkConfig.Category.TOOL;return TnkConfig.Category.WEAPON;
    }
}
