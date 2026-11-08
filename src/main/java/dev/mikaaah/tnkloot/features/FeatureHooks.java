package dev.mikaaah.tnkloot.features;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.menu.TnkMenu;
import dev.mikaaah.tnkloot.progression.ProgressionService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;
public final class FeatureHooks {
 private static final java.lang.reflect.Field DATA=field("data"),RESULTS=field("results");
 private static java.lang.reflect.Field field(String name){try{var f=TnkMenu.class.getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException ex){throw new ExceptionInInitializerError(ex);}}
 public static void register(){
  dev.mikaaah.tnkloot.injection.FinalLoot.register();dev.mikaaah.tnkloot.workshop.ExperienceCosts.reload();FeatureConfig.reload();dev.mikaaah.tnkloot.dimension.DimensionRules.reload();NeoForge.EVENT_BUS.addListener(FeatureMenus::commands);NeoForge.EVENT_BUS.addListener(PlayerLedger::clonePlayer);NeoForge.EVENT_BUS.addListener(PlayerLedger::tick);NeoForge.EVENT_BUS.addListener(BossSignatures::combat);
  NeoForge.EVENT_BUS.addListener(dev.mikaaah.tnkloot.dimension.DimensionSupport::started);
  NeoForge.EVENT_BUS.addListener(dev.mikaaah.tnkloot.dimension.DimensionIntegration::run);
  NeoForge.EVENT_BUS.addListener(dev.mikaaah.tnkloot.workshop.MarketIntegration::run);NeoForge.EVENT_BUS.addListener(FeatureIntegration::run);NeoForge.EVENT_BUS.addListener(dev.mikaaah.tnkloot.workshop.WorkshopIntegration::run);
 }
 public static long total(TnkMenu menu,ServerPlayer player){long cost=0;for(int i=0;i<7;i++){var t=menu.getSlot(i).getItem();if(t.getItem() instanceof UnidentifiedWeaponItem){var base=UnidentifiedWeaponItem.baseResult(player.level(),t);if(base.isEmpty())throw new IllegalArgumentException("Empty reward pool");cost+=dev.mikaaah.tnkloot.workshop.ExperienceCosts.identification(t,base);}}return cost;}
 public static void quote(TnkMenu menu,ServerPlayer player){
  dev.mikaaah.tnkloot.workshop.ExperienceCosts.wallet(menu,player);ContainerData data=featureData(menu);long total=0;int count=0;boolean valid=TnkConfig.get().enableIdentification();
  for(int i=0;i<7;i++){var token=menu.getSlot(i).getItem();if(!(token.getItem() instanceof UnidentifiedWeaponItem))continue;UnidentifiedWeaponItem.bindRollSeed(token);count++;var base=UnidentifiedWeaponItem.baseResult(player.level(),token);if(base.isEmpty()){valid=false;continue;}total+=dev.mikaaah.tnkloot.workshop.ExperienceCosts.identification(token,base);}
  long net=Math.max(0,total-(FeatureConfig.get().salvage()?PlayerLedger.credit(player):0));int payable=(int)Math.min(Integer.MAX_VALUE,net);
  Item currency=BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(TnkConfig.get().costs().item())).orElse(Items.AIR);
  data.set(1,payable&65535);data.set(2,payable>>>16);data.set(4,count);data.set(5,Item.getId(currency));data.set(6,valid&&total<=Integer.MAX_VALUE&&count>0&&(payable==0||(dev.mikaaah.tnkloot.workshop.ExperienceCosts.get().identifyXp()?dev.mikaaah.tnkloot.workshop.ExperienceCosts.points(player)>=payable:UnidentifiedWeaponItem.has(player,currency,payable)))?1:0);
 }
 private static ContainerData featureData(TnkMenu menu){try{return (ContainerData)DATA.get(menu);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
 public static void committed(TnkMenu menu,ServerPlayer player){
  if(FeatureConfig.get().salvage()){int credit=(int)Math.min(total(menu,player),PlayerLedger.credit(player));if(credit>0){PlayerLedger.spend(player,credit);player.sendSystemMessage(Component.literal("Used "+credit+" salvage fragments · "+PlayerLedger.credit(player)+" remaining"));}}
  dev.mikaaah.tnkloot.workshop.ExperienceCosts.wallet(menu,player);try{for(var result:(ItemStack[])RESULTS.get(menu))PlayerLedger.discovered(player,result);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
 }
 public static void setReveal(SimpleContainer container,int slot,ItemStack stack,ServerPlayer player){container.setItem(slot,stack);if(!(stack.getItem() instanceof UnidentifiedWeaponItem)&&UnidentifiedWeaponItem.data(stack).contains("tnk_rarity"))reveal(player,stack);}
 public static void reveal(ServerPlayer player,ItemStack stack){
  if(!FeatureConfig.get().reveals())return;var tag=UnidentifiedWeaponItem.data(stack);int index=PlayerLedger.rarityIndex(tag.getString("tnk_rarity"));
  int color=tag.getInt("tnk_rarity_color");var dust=new DustParticleOptions(new org.joml.Vector3f(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f),.7f+index*.15f);
  player.serverLevel().sendParticles(player,dust,true,player.getX(),player.getY()+1.2,player.getZ(),6+index*7,.45,.35,.45,0);
  if(index>=3)player.serverLevel().sendParticles(player,ParticleTypes.ENCHANT,true,player.getX(),player.getY()+1,player.getZ(),index*8,.6,.4,.6,.04);
  SoundEvent sound=index>=5?SoundEvents.UI_TOAST_CHALLENGE_COMPLETE:index>=4?SoundEvents.PLAYER_LEVELUP:SoundEvents.EXPERIENCE_ORB_PICKUP;
  player.playNotifySound(sound,SoundSource.PLAYERS,.3f+index*.06f,.85f+index*.08f);
 }
 public static boolean matches(String table){for(String prefix:FeatureConfig.get().tablePrefixes())if(prefix.startsWith("*:")){int colon=table.indexOf(':');if(colon>=0&&table.substring(colon+1).startsWith(prefix.substring(2)))return true;}else if(table.startsWith(prefix))return true;return false;}
 public static ObjectArrayList<ItemStack> dungeon(ObjectArrayList<ItemStack> loot,LootContext ctx){
  if(dev.mikaaah.tnkloot.dimension.DimensionLoot.rewardPool(ctx).isPresent())return loot;
  if(!FeatureConfig.get().dungeon()||!TnkConfig.get().enableIdentification()||!TnkConfig.get().replaceWeaponLoot())return loot;
  var table=ctx.getQueriedLootTableId();if(table==null||!matches(table.toString()))return loot;
  // Only chest table namespaces/paths are whitelisted. Never inject into mob, fishing or player drops.
  var opener=ctx.getParamOrNull(LootContextParams.THIS_ENTITY);if(opener!=null&&!(opener instanceof ServerPlayer))return loot;
  if(loot.stream().anyMatch(s->UnidentifiedWeaponItem.data(s).getString("source_type").equals("dungeon_accessory")))return loot;
  if(ctx.getRandom().nextDouble()>=FeatureConfig.get().chestChance())return loot;
  var entity=ctx.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);int tier=opener instanceof ServerPlayer p?ProgressionService.getTier(p):entity instanceof ServerPlayer p?ProgressionService.getTier(p):0;
  // Vanilla chest contexts can carry the opener in THIS_ENTITY; automation without a player uses starter tier.
  var luck=ctx.getParamOrNull(LootContextParams.ATTACKING_ENTITY);if(luck instanceof ServerPlayer p)tier=ProgressionService.getTier(p);
  tier=dev.mikaaah.tnkloot.dimension.DimensionLoot.eligibleTier(tier,TnkConfig.Category.ACCESSORY,ctx.getLevel().dimension().location().toString());if(tier<0)return loot;
  loot.add(UnidentifiedWeaponItem.create(TnkConfig.Category.ACCESSORY,tier,"","dungeon_accessory"));return loot;
 }
 public static int eligibleTier(int requested){return TnkConfig.get().tiers().stream().mapToInt(TnkConfig.Tier::rank).filter(t->t<=requested).boxed().sorted(java.util.Comparator.reverseOrder()).filter(t->!WeaponSelector.pool(t,TnkConfig.Category.ACCESSORY).isEmpty()).findFirst().orElse(-1);}
}
