package dev.mikaaah.tnkloot.features;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.rarity.RarityPolicy;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.*;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import java.util.*;
public final class BossSignatures {
 public static String theme(String pool){
  String configured=FeatureConfig.get().themes().get(pool);if(configured!=null)return configured;
  String s=pool.toLowerCase(Locale.ROOT);
  if(s.matches(".*(ignis|fire|infernal|netherite|dragon|flame|sun|umvuthi).*"))return "ember";
  if(s.matches(".*(frost|ice|yeti|leviathan|kraken).*"))return "frost";
  if(s.matches(".*(lightning|storm|harbinger|thunder).*"))return "storm";
  if(s.matches(".*(poison|venom|arach|spider|wither|maledictus).*"))return "venom";
  if(s.matches(".*(ender|void|dead_king|underworld|soul).*"))return "void";
  return "earth";
 }
 public static ItemStack apply(ItemStack stack,int tier,long seed){
  var tag=UnidentifiedWeaponItem.data(stack);String pool=tag.getString("boss_pool");
  if(pool.isBlank()||!FeatureConfig.get().bossAffixes()||LootCategory.forItem(stack).isNone())return stack;
  String theme=theme(pool);if(theme.equals("none"))return stack;
  var holder=AffixRegistry.INSTANCE.holder(ResourceLocation.parse("tnkloot:boss/"+theme));
  if(!holder.isBound())throw new IllegalStateException("Missing boss signature affix "+theme);
  var rarity=AffixHelper.getRarity(stack);if(!rarity.isBound()||!holder.get().canApplyTo(stack,LootCategory.forItem(stack),rarity.get()))return stack;
  float level=RarityPolicy.get().level(tier,RarityPolicy.get().quality(tag.getString("tnk_rarity")),new Random(seed^0x424F5353));
  AffixHelper.setAffixes(stack,stack.getOrDefault(Apoth.Components.AFFIXES,ItemAffixes.EMPTY).toBuilder().put(holder,level).build());
  tag=UnidentifiedWeaponItem.data(stack);tag.putString("tnk_signature",theme);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
 }
 public static String describe(String theme){return switch(theme){case "ember"->"Emberbrand · ignite foes";case "frost"->"Frostbound · slow foes";case "storm"->"Stormheart · gain speed and mark foes";case "venom"->"Venomfang · poison foes";case "void"->"Voidmarked · weaken foes";case "earth"->"Stoneguard · stagger foes and gain resistance";default->"";};}
 private static boolean ready(LivingEntity owner){
  var d=owner.getPersistentData();long now=owner.level().getGameTime();String key="tnk_signature_cooldown";
  if(d.contains(key)&&now<d.getLong(key))return false;d.putLong(key,now+FeatureConfig.get().cooldown());return true;
 }
 public static void combat(LivingDamageEvent.Post event){
  if(!FeatureConfig.get().bossAffixes()||event.getNewDamage()<=0||event.getEntity().level().isClientSide)return;
  LivingEntity target=event.getEntity();
  if(event.getSource().getEntity() instanceof LivingEntity source&&source!=target&&!source.isAlliedTo(target)){
   String theme=UnidentifiedWeaponItem.data(source.getMainHandItem()).getString("tnk_signature");
   if(!theme.isBlank()&&ready(source))trigger(theme,source,target);
  }
  for(ItemStack armour:target.getArmorSlots())if(!UnidentifiedWeaponItem.data(armour).getString("tnk_signature").isBlank()){
   if(ready(target))target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,FeatureConfig.get().duration(),0));break;
  }
 }
 public static void trigger(String theme,LivingEntity source,LivingEntity target){
  int ticks=FeatureConfig.get().duration();
  switch(theme){
   case "ember"->target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),ticks));
   case "frost"->target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks,1));
   case "storm"->{source.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,ticks,0));target.addEffect(new MobEffectInstance(MobEffects.GLOWING,ticks,0));}
   case "venom"->target.addEffect(new MobEffectInstance(MobEffects.POISON,ticks,0));
   case "void"->target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,ticks,0));
   case "earth"->{target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks,0));source.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,ticks,0));}
  }
 }
}
