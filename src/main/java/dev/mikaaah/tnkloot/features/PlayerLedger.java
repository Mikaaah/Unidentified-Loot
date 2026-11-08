package dev.mikaaah.tnkloot.features;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;
public final class PlayerLedger {
 private static final String KEY="tnkloot_journal";
 public static CompoundTag read(Player p){return p.getPersistentData().getCompound(KEY).copy();}
 public static void write(Player p,CompoundTag data){p.getPersistentData().put(KEY,data.copy());}
 public static int credit(Player p){return Math.max(0,read(p).getInt("credit"));}
 public static void addCredit(Player p,int amount){if(amount<0)throw new IllegalArgumentException("Negative credit");var d=read(p);long total=(long)credit(p)+amount;if(total>FeatureConfig.get().maxCredit())throw new IllegalArgumentException("Salvage credit limit reached; spend credit first");d.putInt("credit",(int)total);write(p,d);}
 public static void spend(Player p,int amount){if(amount<0||amount>credit(p))throw new IllegalStateException("Insufficient salvage credit");var d=read(p);d.putInt("credit",credit(p)-amount);write(p,d);}
 public static void discovered(ServerPlayer p,ItemStack stack){
  if(!FeatureConfig.get().collection()||stack.isEmpty()||stack.getItem() instanceof UnidentifiedWeaponItem)return;
  var tag=UnidentifiedWeaponItem.data(stack);if(!tag.contains("tnk_rarity"))return;
  var data=read(p);var items=data.getCompound("discoveries");String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();var entry=items.getCompound(id);
  if(!entry.contains("first"))entry.putLong("first",p.serverLevel().getGameTime());
  entry.putInt("count",Math.min(Integer.MAX_VALUE-1,entry.getInt("count"))+1);
  int quality=rarityIndex(tag.getString("tnk_rarity"));if(!entry.contains("rarity")||quality>entry.getInt("rarity"))entry.putInt("rarity",quality);
  entry.putInt("best_tier",Math.max(entry.getInt("best_tier"),tag.getInt("tnk_tier")));
  if(!tag.getString("boss_pool").isBlank()){var bosses=entry.getCompound("bosses");bosses.putBoolean(tag.getString("boss_pool"),true);entry.put("bosses",bosses);}
  items.put(id,entry);data.put("discoveries",items);write(p,data);
 }
 public static int rarityIndex(String id){return Math.max(0,List.of("common","uncommon","rare","epic","legendary","mythic").indexOf(id.startsWith("tnkloot:")?id.substring(8):id));}
 /** Discover automated or traded results without counting the same item on every scan. */
 public static void observe(ServerPlayer player){
  if(!FeatureConfig.get().collection())return;
  var data=read(player);var items=data.getCompound("discoveries");boolean changed=false;
  for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
   var stack=player.getInventory().getItem(slot);
   if(stack.isEmpty()||stack.getItem() instanceof UnidentifiedWeaponItem)continue;
   var tag=UnidentifiedWeaponItem.data(stack);if(!tag.contains("tnk_rarity")||!tag.contains("tnk_tier"))continue;
   String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();var entry=items.getCompound(id);boolean fresh=!entry.contains("first");
   int rarity=rarityIndex(tag.getString("tnk_rarity")),tier=Math.max(0,tag.getInt("tnk_tier"));
   if(fresh){entry.putLong("first",player.serverLevel().getGameTime());changed=true;}
   if(fresh||rarity>entry.getInt("rarity")){entry.putInt("rarity",rarity);changed=true;}
   if(tier>entry.getInt("best_tier")){entry.putInt("best_tier",tier);changed=true;}
   String boss=tag.getString("boss_pool");if(!boss.isBlank()&&!entry.getCompound("bosses").getBoolean(boss)){var bosses=entry.getCompound("bosses");bosses.putBoolean(boss,true);entry.put("bosses",bosses);changed=true;}
   items.put(id,entry);
  }
  if(changed){data.put("discoveries",items);write(player,data);}
 }
 public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer player&&player.tickCount%40==0)observe(player);}
 public static int salvageValue(ItemStack stack){var tag=UnidentifiedWeaponItem.data(stack);if(stack.isEmpty()||stack.getItem() instanceof UnidentifiedWeaponItem||!tag.contains("tnk_rarity")||!tag.contains("tnk_tier"))return 0;int tier=Math.min(100,Math.max(0,tag.getInt("tnk_tier")));int raw=FeatureConfig.get().salvageValues().get(rarityIndex(tag.getString("tnk_rarity")))*(tier+1);
  int price=dev.mikaaah.tnkloot.config.TnkConfig.get().costs().amount(tier,BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
  int cost=(dev.shadowsoffire.apotheosis.loot.LootCategory.forItem(stack).isNone()&&!dev.mikaaah.tnkloot.rarity.NativeQuality.jewelry(stack))?price:dev.mikaaah.tnkloot.rarity.RarityPolicy.cost(price,dev.mikaaah.tnkloot.rarity.RarityPolicy.get().quality(tag.getString("tnk_rarity")).cost());
  cost=dev.mikaaah.tnkloot.workshop.ExperienceCosts.scale(cost);int cap=cost<=0?0:Math.max(1,(int)Math.floor(cost*FeatureConfig.get().refundCap()));return Math.min(raw,cap);}
 public static void clonePlayer(PlayerEvent.Clone e){write(e.getEntity(),read(e.getOriginal()));}
}
