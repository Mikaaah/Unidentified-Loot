package dev.mikaaah.tnkloot.boss;
import dev.mikaaah.tnkloot.compat.*;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.rarity.*;
import com.google.gson.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import java.util.*;
import java.nio.file.*;

/** Runs only through the opt-in development smoke test. */
public final class BossIntegration {
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void verify(ServerPlayer player)throws Exception{
  Path path=BossRewards.path();String prior=Files.readString(path);
  try{
   Files.writeString(path,"""
    {"enabled":true,"require_player_kill":true,"pools":[{"id":"tnkloot/mixed","mod":"tnkloot","entity":"minecraft:zombie","name":"Mixed Boss","tier":2,"enabled":true,"chance":1,"additional_if_no_equipment":true,"items":[{"item":"minecraft:diamond_sword","category":"weapon","weight":1},{"item":"minecraft:diamond_helmet","category":"helmet","weight":1},{"item":"minecraft:bow","category":"bow","weight":1}]}]}
    """);
   require(BossRewards.reload(),"Mixed pool reload failed");var pool=BossRewards.find("tnkloot/mixed").orElseThrow();Set<Item> seen=new HashSet<>();
   for(int seed=0;seed<160;seed++){
    var token=BossRewards.token(pool,TnkConfig.Category.HELMET,player.level(),new ItemStack(Items.DIAMOND_HELMET));
    require(token.is(BossItems.DROP.get()),"Not the universal registered item");var tag=UnidentifiedWeaponItem.data(token);
    tag.putLong("roll_seed",seed);token.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));RaritySupport.force(token,"mythic");
    require(!tag.contains("boss_result"),"Reward was chosen at drop time");
    require(token.get(DataComponents.CUSTOM_MODEL_DATA).value()==6,"Mythic texture property missing");
    var base=UnidentifiedWeaponItem.baseResult(player.level(),token);seen.add(base.getItem());
    require(base.equals(UnidentifiedWeaponItem.baseResult(player.level(),token))||ItemStack.isSameItemSameComponents(base,UnidentifiedWeaponItem.baseResult(player.level(),token)),"Reopen rerolled boss result");
    var result=ApotheosisCompat.apply(base,2,player,seed);require(!result.isEmpty(),"Mixed category affix generation failed");
   }
   require(seen.size()==3,"Universal pool did not roll all categories");
   var zombie=EntityType.ZOMBIE.create(player.level());List<ItemEntity> drops=new ArrayList<>();
   for(Item item:new Item[]{Items.DIAMOND_SWORD,Items.DIAMOND_HELMET,Items.ROTTEN_FLESH})drops.add(new ItemEntity(player.level(),0,0,0,new ItemStack(item)));
   var event=new LivingDropsEvent(zombie,player.damageSources().playerAttack(player),drops,true);BossRewards.drops(event);BossRewards.drops(event);
   require(drops.size()==2&&drops.stream().filter(d->d.getItem().is(BossItems.DROP.get())).count()==1,"Multiple boss crates per death");
   require(drops.stream().anyMatch(d->d.getItem().is(Items.ROTTEN_FLESH)),"Boss material lost");
   var noPlayer=new LivingDropsEvent(zombie,player.damageSources().generic(),new ArrayList<>(),false);BossRewards.drops(noPlayer);require(noPlayer.getDrops().isEmpty(),"Player-only rule ignored");
   // Vanilla menu packet attacks must not extract or duplicate server display stacks.
   player.getInventory().clearContent();var ui=new BossEditor(99,player.getInventory(),player,new SimpleContainer(54));
   ui.clicked(24,0,ClickType.PICKUP,player);ui.clicked(0,0,ClickType.PICKUP,player);ui.clicked(0,0,ClickType.PICKUP,player);
   require(ui.getCarried().isEmpty(),"Ghost card extracted");
   player.getInventory().setItem(9,new ItemStack(Items.DIAMOND_BOOTS));ui.clicked(54,0,ClickType.QUICK_MOVE,player);
   require(player.getInventory().getItem(9).is(Items.DIAMOND_BOOTS),"Import consumed equipment");
   require(BossStore.pool("tnkloot/mixed").getAsJsonArray("items").size()==4,"GUI import failed");
   for(var type:ClickType.values())ui.clicked(0,0,type,player);
   require(ui.getCarried().isEmpty(),"Ghost slot click duplicated equipment");
   ui.removed(player);
   // Force a second reward page and exercise both page controls.
   BossStore.edit("tnkloot/mixed",p->{var arr=p.getAsJsonArray("items");
    for(var id:net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet().stream().filter(k->k.getNamespace().equals("minecraft")).limit(60).toList()){
     if(arr.asList().stream().anyMatch(v->v.getAsJsonObject().get("item").getAsString().equals(id.toString())))continue;
     var w=new JsonObject();w.addProperty("item",id.toString());w.addProperty("category","weapon");w.addProperty("weight",1);arr.add(w);
    }
   });
   var paged=new BossEditor(100,player.getInventory(),player,new SimpleContainer(54));
   paged.clicked(24,0,ClickType.PICKUP,player);paged.clicked(0,0,ClickType.PICKUP,player);paged.clicked(0,0,ClickType.PICKUP,player);
   var first=paged.slots.get(0).getItem().getHoverName().getString();paged.clicked(53,0,ClickType.PICKUP,player);
   require(!first.equals(paged.slots.get(0).getItem().getHoverName().getString()),"Boss vault did not page");paged.clicked(45,0,ClickType.PICKUP,player);
   require(first.equals(paged.slots.get(0).getItem().getHoverName().getString()),"Boss vault did not page back");paged.removed(player);
   BossStore.edit("tnkloot/mixed",p->{var arr=p.getAsJsonArray("items");while(arr.size()>4)arr.remove(arr.size()-1);});
   // The manual entry command accepts an unquoted namespaced entity ID.
   require(player.getServer().getCommands().getDispatcher().execute("tnkloot boss add minecraft:skeleton",player.createCommandSourceStack().withPermission(2))==1,"Manual boss command failed");
   String added="minecraft/skeleton";BossStore.importItem(added,new ItemStack(Items.BOW));
   require(BossStore.pool(added).getAsJsonArray("items").size()==1&&!BossStore.pool(added).get("enabled").getAsBoolean(),"Manual boss setup failed");
   BossStore.edit("tnkloot/mixed",p->{var arr=p.getAsJsonArray("items");BossEditor.setPercent(p,arr.get(0).getAsJsonObject(),25);});
   var json=BossStore.pool("tnkloot/mixed");long total=json.getAsJsonArray("items").asList().stream().map(JsonElement::getAsJsonObject).filter(e->!e.has("enabled")||e.get("enabled").getAsBoolean()).mapToLong(e->e.get("weight").getAsLong()).sum();
   require(json.getAsJsonArray("items").get(0).getAsJsonObject().get("weight").getAsInt()*4==total,"Boss percentage was not exact");
   BossStore.edit("tnkloot/mixed",p->p.getAsJsonArray("items").get(0).getAsJsonObject().addProperty("enabled",false));
   require(BossRewards.find("tnkloot/mixed").orElseThrow().items().size()==3,"Disabled reward still active");
   require(BossRewards.reload(),"Boss edits not persistent");
   System.out.println("TNKLOOT_BOSS_SMOKE_OK: 160 mixed-category Mythic affix rolls, single-drop replacement, material/player protection, GUI imports/ghost guards, manual bosses, percentages and persistence");
  }finally{Files.writeString(path,prior);require(BossRewards.reload(),"Failed to restore boss definitions");}
 }
}
