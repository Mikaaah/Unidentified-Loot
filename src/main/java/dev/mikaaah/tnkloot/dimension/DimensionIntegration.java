package dev.mikaaah.tnkloot.dimension;
import com.mojang.authlib.GameProfile;
import com.google.gson.*;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.rarity.*;
import dev.mikaaah.tnkloot.menu.*;
import dev.mikaaah.tnkloot.features.*;
import dev.mikaaah.tnkloot.compat.BossRewards;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.*;
import java.nio.file.*;
public final class DimensionIntegration {
 static final java.util.concurrent.atomic.AtomicBoolean ONCE=new java.util.concurrent.atomic.AtomicBoolean();static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void run(ServerStartedEvent event){if(!Boolean.getBoolean("tnkloot.dimensionsSmokeTest")||!ONCE.compareAndSet(false,true))return;var server=event.getServer();String cfg=null;
  try{
   cfg=Files.readString(TnkConfig.path());var p=new FakePlayer(server.overworld(),new GameProfile(UUID.randomUUID(),"TNKDimensions")){@Override public boolean hasPermissions(int n){return true;}@Override public boolean hasDisconnected(){return false;}};
   require(DimensionRules.allowed("aether:skyroot_sword","aether:the_aether"),"Aether own item blocked");require(!DimensionRules.allowed("aether:skyroot_sword","minecraft:overworld"),"Aether leaked to Overworld");require(!DimensionRules.allowed("twilightforest:ironwood_sword","aether:the_aether"),"Twilight leaked to Aether");require(DimensionRules.allowed("minecraft:iron_sword","aether:the_aether"),"Global fallback blocked");
   require(!DimensionRules.allowed("knavesneeds:fiery_longsword","minecraft:overworld"),"Addon dimension material leaked");
   var fresh=UnidentifiedWeaponItem.create(1);require(!UnidentifiedWeaponItem.baseResult(p.level(),fresh).isEmpty(),"Fresh manual token failed");require(DimensionLoot.origin(fresh).equals("minecraft:overworld"),"Fresh manual origin missing");
   var aetherLevel=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,ResourceLocation.parse("aether:the_aether")));
   if(aetherLevel!=null){var travel=UnidentifiedWeaponItem.create(0);var first=UnidentifiedWeaponItem.baseResult(aetherLevel,travel);var packed=travel.save(p.registryAccess());var restored=ItemStack.parseOptional(p.registryAccess(),(net.minecraft.nbt.CompoundTag)packed);require(DimensionLoot.origin(restored).equals("aether:the_aether"),"Saved origin lost");require(ItemStack.isSameItemSameComponents(first,UnidentifiedWeaponItem.baseResult(p.level(),restored)),"Travelling rerolled origin reward");}
   int rolls=0;boolean sawAether=false;
   for(String origin:List.of("minecraft:overworld","aether:the_aether","twilightforest:twilight_forest","minecraft:the_nether","minecraft:the_end"))for(int seed=0;seed<80;seed++){
    var token=UnidentifiedWeaponItem.create(0);var tag=UnidentifiedWeaponItem.data(token);tag.putString("origin_dimension",origin);tag.putLong("roll_seed",seed);token.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    var base=UnidentifiedWeaponItem.baseResult(p.level(),token);require(!base.isEmpty(),"Empty origin result");String id=BuiltInRegistries.ITEM.getKey(base.getItem()).toString();require(DimensionRules.allowed(id,origin),"Wrong origin equipment "+id);
    require(UnidentifiedWeaponItem.data(token).getString("origin_dimension").equals(origin),"Identification changed origin");var options=FeatureMenus.options(token);require(options.stream().anyMatch(o->o.id().equals(id)),"Preview disagrees with roll");if(origin.equals("aether:the_aether")&&id.startsWith("aether:"))sawAether=true;rolls++;
   }
   if(net.neoforged.fml.ModList.get().isLoaded("aether"))require(sawAether,"Registered Aether gear was not imported");
   // Jewelry NONE is present-but-invalid; it must be replaced, and genuine native results must survive.
   if(net.neoforged.fml.ModList.get().isLoaded("irons_jewelry")){
    Class<?> data=Class.forName("io.redspace.ironsjewelry.core.data.JewelryData");int jewelry=0;
    for(String item:List.of("ring","necklace"))for(int tier=0;tier<8;tier++)for(var q:RarityPolicy.get().qualities()){
     var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("irons_jewelry:"+item)));data.getMethod("set",ItemStack.class,data).invoke(null,stack,data.getField("NONE").get(null));var token=UnidentifiedWeaponItem.create(tier);RaritySupport.force(token,q.id());RaritySupport.base(stack,token);NativeQuality.prepare(stack,tier,p,1234);
     var generated=data.getMethod("get",ItemStack.class).invoke(null,stack);require((boolean)data.getMethod("isValid").invoke(generated),"Invalid native jewelry after preparation");
     for(Object material:((java.util.Map<?,?>)data.getMethod("parts").invoke(generated)).values())require(!((net.minecraft.core.Holder<?>)material).unwrapKey().orElseThrow().location().getPath().contains("gold"),"Gold material generated");var copy=stack.copy();NativeQuality.prepare(stack,tier,p,9999);require(ItemStack.isSameItemSameComponents(copy,stack),"Existing valid native jewelry rerolled");jewelry++;
    }
    var menu=new TnkMenu(TnkMenus.ADMIN.get(),123,p.getInventory(),true);menu.clickMenuButton(p,100+TnkConfig.Category.ACCESSORY.ordinal());menu.clickMenuButton(p,1000);menu.broadcastChanges();
    int icons=0;for(int i=0;i<37;i++){var s=menu.getSlot(i).getItem();if(NativeQuality.jewelry(s)){icons++;require((boolean)data.getMethod("isValid").invoke(data.getMethod("get",ItemStack.class).invoke(null,s)),"Editor shows invalid jewelry");}}require(icons>0,"Jewelry vault check had no rings");menu.removed(p);
    TnkLoot.LOG.info("TNKLOOT_JEWELRY_OK: {} native tier/rarity combinations, NONE repair, preservation and vault previews",jewelry);
   }
   // Invalid older IDs are removed with a backup; editing/removing imported items is respected.
   var root=JsonParser.parseString(Files.readString(TnkConfig.path())).getAsJsonObject();var w=new JsonObject();w.addProperty("item","irons_jewelry:bracelet");w.addProperty("category","accessory");w.addProperty("min_tier",0);w.addProperty("max_tier",7);w.addProperty("weight",1);w.addProperty("enabled",true);root.getAsJsonArray("weapons").add(w);Files.writeString(TnkConfig.path(),root.toString());require(BaseCompat.install(),"Migration failed");require(!Files.readString(TnkConfig.path()).contains("irons_jewelry:bracelet"),"Invalid jewelry ID retained");
   for(var weapon:TnkConfig.get().weapons()){var key=ResourceLocation.parse(weapon.id());if(net.neoforged.fml.ModList.get().isLoaded(key.getNamespace())&&BaseCompat.NAMESPACES.contains(key.getNamespace()))require(BuiltInRegistries.ITEM.containsKey(key),"Imported missing item "+key);}
   if(net.neoforged.fml.ModList.get().isLoaded("aether")){
    var params=new LootParams.Builder(p.serverLevel()).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withOptionalParameter(LootContextParams.THIS_ENTITY,p).create(LootContextParamSets.CHEST);var ctx=new LootContext.Builder(params).create(Optional.empty());ctx.setQueriedLootTableId(ResourceLocation.parse("aether:chests/dungeon/bronze/bronze_dungeon_treasure"));
    var loot=new ObjectArrayList<ItemStack>();loot.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("aether:valkyrie_lance"))));loot.add(new ItemStack(Items.DIAMOND));DimensionLoot.route(loot,ctx);DimensionLoot.route(loot,ctx);require(loot.size()==2&&loot.stream().filter(s->s.getItem() instanceof UnidentifiedWeaponItem).count()==1,"Boss cache conversion duplicated or lost loot");require(loot.stream().anyMatch(s->s.is(Items.DIAMOND)),"Boss cache material removed");
   }
   TnkLoot.LOG.info("TNKLOOT_DIMENSIONS_OK: {} origin-locked rolls, preview agreement, live registry import, invalid-ID migration, Aether cache conversion",rolls);
  }catch(Throwable e){TnkLoot.LOG.error("TNKLOOT_DIMENSIONS_FAILED",e);}finally{try{if(cfg!=null){Files.writeString(TnkConfig.path(),cfg);TnkConfig.reload();}}catch(Exception e){TnkLoot.LOG.error("Test config restore failed",e);}server.halt(false);}
 }
}
