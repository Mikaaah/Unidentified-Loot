package dev.mikaaah.tnkloot.rarity;
import dev.mikaaah.tnkloot.compat.NativeEquipment;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class NativeQuality {
 private NativeQuality(){}
 public static boolean jewelry(ItemStack stack){String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();return id.equals("irons_jewelry:ring")||id.equals("irons_jewelry:necklace");}
 private static Object call(Object target,String method)throws ReflectiveOperationException{return target.getClass().getMethod(method).invoke(target);}
 private static double number(Object target,String method){try{return ((Number)call(target,method)).doubleValue();}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
 public static int choose(int size,int tier,RarityPolicy.Quality q,Random random){if(size==0)throw new IllegalStateException("No native jewelry choices");int start=(int)Math.floor(Math.clamp(tier,0,7)/7.0*(size-1));int count=Math.min(2,size-start);return start+Math.min(count-1,(int)((q.floor()+random.nextDouble()*(1-q.floor()))*count));}
 private static boolean usable(Object pattern,Registry<?> materials)throws ReflectiveOperationException {
  for(Object ingredient:(List<?>)call(pattern,"partTemplate")) {Holder<?> part=(Holder<?>)call(ingredient,"part");boolean found=false;
   for(var material:materials.holders().toList())if(!material.key().location().getPath().contains("gold")&&(boolean)part.value().getClass().getMethod("canUseMaterial",Holder.class).invoke(part.value(),material)){found=true;break;}
   if(!found)return false;
  }return true;
 }
 public static ItemStack prepare(ItemStack stack,int tier,Player player,long seed){
  if(!jewelry(stack))return NativeEquipment.prepare(stack,tier,player,seed);
  try{
   Class<?> data=Class.forName("io.redspace.ironsjewelry.core.data.JewelryData");Object existing=data.getMethod("get",ItemStack.class).invoke(null,stack);
   if((boolean)data.getMethod("isValid").invoke(existing))return stack;
   var tag=UnidentifiedWeaponItem.data(stack);var q=tag.contains("tnk_rarity")?RarityPolicy.get().quality(tag.getString("tnk_rarity")):RarityPolicy.get().roll(tier,false,seed);
   Class<?> registries=Class.forName("io.redspace.ironsjewelry.registry.IronsJewelryRegistries");Registry<?> patterns=(Registry<?>)registries.getMethod("patternRegistry",RegistryAccess.class).invoke(null,player.registryAccess());Registry<?> materials=(Registry<?>)registries.getMethod("materialRegistry",RegistryAccess.class).invoke(null,player.registryAccess());
   List<Holder<?>> candidates=new ArrayList<>();
   for(var h:patterns.holders().toList())if(((Holder<?>)call(call(h.value(),"jewelryType"),"item")).value()==stack.getItem()&&usable(h.value(),materials))candidates.add(h);
   candidates.sort(Comparator.comparingDouble(h->number(h.value(),"qualityMultiplier")));Random random=new Random(seed^0x4A4557454C5259L);Holder<?> pattern=candidates.get(choose(candidates.size(),tier,q,random));Map<Holder<?>,Holder<?>> parts=new HashMap<>();
   for(Object ingredient:(List<?>)call(pattern.value(),"partTemplate")){
    Holder<?> part=(Holder<?>)call(ingredient,"part");List<Holder<?>> eligible=new ArrayList<>();
    for(var material:materials.holders().toList())if(!material.key().location().getPath().contains("gold")&&(boolean)part.value().getClass().getMethod("canUseMaterial",Holder.class).invoke(part.value(),material))eligible.add(material);
    eligible.sort(Comparator.comparingDouble(h->number(h.value(),"quality")));parts.put(part,eligible.get(choose(eligible.size(),tier,q,random)));
   }
   Object generated=data.getConstructor(Holder.class,Map.class).newInstance(pattern,parts);if(!(boolean)data.getMethod("isValid").invoke(generated))throw new IllegalStateException("Native jewelry validation failed");data.getMethod("set",ItemStack.class,data).invoke(null,stack,generated);return stack;
  }catch(ReflectiveOperationException ex){throw new IllegalStateException("Could not generate native jewelry",ex);}
 }
}
