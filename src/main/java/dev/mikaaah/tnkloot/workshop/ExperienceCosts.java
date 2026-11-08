package dev.mikaaah.tnkloot.workshop;
import com.google.gson.*;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.features.PlayerLedger;
import dev.mikaaah.tnkloot.menu.TnkMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.*;
import java.nio.file.*;
public final class ExperienceCosts {
 public record Settings(boolean identifyXp,double multiplier,boolean salvageXp,int base,int tier,int rarity){}
 private static volatile Settings current;private static final java.lang.reflect.Field DATA=field();
 private static java.lang.reflect.Field field(){try{var f=TnkMenu.class.getDeclaredField("data");f.setAccessible(true);return f;}catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}}
 public static ContainerData data(TnkMenu menu){try{return (ContainerData)DATA.get(menu);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
 public static Path path(){return TnkConfig.path().resolveSibling("experience.json");}
 public static Settings get(){if(current==null)reload();return current;}
 public static synchronized boolean reload(){try{
  if(!Files.exists(path())){Files.createDirectories(path().getParent());try(var in=ExperienceCosts.class.getResourceAsStream("/default-experience.json")){Files.copy(in,path());}}
  current=parse(JsonParser.parseString(Files.readString(path())).getAsJsonObject());return true;
 }catch(Exception e){dev.mikaaah.tnkloot.TnkLoot.LOG.error("Invalid experience.json; retaining working XP settings",e);if(current==null)current=new Settings(true,1,true,1,1,1);return false;}}
 public static Settings parse(JsonObject j){String mode=j.get("identification_payment").getAsString();if(!mode.equals("xp_points")&&!mode.equals("items"))throw new IllegalArgumentException("identification_payment must be xp_points or items");double scale=j.get("identification_xp_multiplier").getAsDouble();if(!Double.isFinite(scale)||scale<0||scale>100)throw new IllegalArgumentException("Invalid XP multiplier");int b=number(j,"salvage_xp_base"),t=number(j,"salvage_xp_per_tier"),r=number(j,"salvage_xp_per_rarity");return new Settings(mode.equals("xp_points"),scale,j.get("salvage_costs_xp").getAsBoolean(),b,t,r);}
 private static int number(JsonObject j,String key){int n=j.get(key).getAsBigDecimal().intValueExact();if(n<0||n>10000)throw new IllegalArgumentException("Invalid "+key);return n;}
 public static long xpAtLevel(int level){if(level<0)return 0;double l=level;double points=level<=16?l*l+6*l:level<=31?2.5*l*l-40.5*l+360:4.5*l*l-162.5*l+2220;return Math.min(Integer.MAX_VALUE,Math.round(points));}
 public static int points(Player p){return (int)Math.min(Integer.MAX_VALUE,xpAtLevel(p.experienceLevel)+Math.round(p.experienceProgress*p.getXpNeededForNextLevel()));}
 public static boolean spendPoints(Player p,int amount){if(amount<0||points(p)<amount)return false;if(amount>0)p.giveExperiencePoints(-amount);return true;}
 public static int scale(int original){return get().identifyXp()?(int)Math.min(Integer.MAX_VALUE,Math.ceil(original*get().multiplier())):original;}
 public static int identification(ItemStack token,ItemStack base){return scale(UnidentifiedWeaponItem.cost(token,base));}
 public static int salvage(ItemStack stack){if(!get().salvageXp()||stack.isEmpty())return 0;var tag=UnidentifiedWeaponItem.data(stack);return get().base()+Math.clamp(tag.getInt("tnk_tier"),0,100)*get().tier()+PlayerLedger.rarityIndex(tag.getString("tnk_rarity"))*get().rarity();}
 public static boolean pay(ServerPlayer p,Item item,int amount){if(get().identifyXp())return spendPoints(p,amount);if(!UnidentifiedWeaponItem.has(p,item,amount))return false;UnidentifiedWeaponItem.remove(p,item,amount);return true;}
 public static void wallet(TnkMenu menu,Player p){var d=data(menu);int xp=points(p),credit=PlayerLedger.credit(p);d.set(15,get().identifyXp()?1:0);d.set(16,xp&65535);d.set(17,xp>>>16);d.set(18,credit&65535);d.set(19,credit>>>16);}
 public static boolean xpMode(TnkMenu menu){return data(menu).get(15)==1;}
 public static int xp(TnkMenu menu){var d=data(menu);return (d.get(16)&65535)|(d.get(17)<<16);}
 public static int credit(TnkMenu menu){var d=data(menu);return (d.get(18)&65535)|(d.get(19)<<16);}
}
