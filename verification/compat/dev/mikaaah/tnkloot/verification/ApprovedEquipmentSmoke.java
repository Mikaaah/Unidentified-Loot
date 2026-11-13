package dev.mikaaah.tnkloot.verification;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.compat.*;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.*;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.injection.*;
import dev.shadowsoffire.apotheosis.affix.*;
import dev.shadowsoffire.apotheosis.loot.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class ApprovedEquipmentSmoke {
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static JsonObject verify(MinecraftServer server) {
        var level=server.overworld();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"TNKApproved"));
        int rules=0,endChecks=0,nativeOnly=0;
        for(var profile:CompatPresets.bundled("approved-pack-compat.json").getAsJsonArray("profiles")) {
            var definition=profile.getAsJsonObject();if(!ModList.get().isLoaded(definition.getAsJsonArray("requires").get(0).getAsString()))continue;
            for(var value:definition.getAsJsonArray("items")) {
                var expected=value.getAsJsonObject();String id=expected.get("item").getAsString();var key=ResourceLocation.parse(id);
                require(BuiltInRegistries.ITEM.containsKey(key),"Approved ID not registered: "+id);
                var rule=TnkConfig.get().weapons().stream().filter(w->w.id().equals(id)).findFirst().orElseThrow(()->new AssertionError("Missing approved rule "+id));
                require(rule.minTier()==expected.get("min_tier").getAsInt()&&rule.maxTier()==expected.get("max_tier").getAsInt()&&WeaponSelector.available(rule),"Wrong/unavailable approved band "+id);
                require(BossRewards.exclusive(id)||!DimensionLoot.pool(rule.minTier(),rule.category(),DimensionRules.infer(id).isBlank()?"minecraft:overworld":DimensionRules.infer(id)).isEmpty(),"Approved item has no origin pool "+id);
                require(!LootCategory.forItem(new ItemStack(BuiltInRegistries.ITEM.get(key))).isNone(),"Approved equipment has no affix category: "+id);
                rules++;
                if(key.getNamespace().equals("enderscape")||key.getNamespace().equals("the_beyond")) {
                    require(DimensionRules.allowed(id,"minecraft:the_end")&&!DimensionRules.allowed(id,"minecraft:overworld"),"End item escaped its dimension: "+id);
                    ItemStack input=new ItemStack(BuiltInRegistries.ITEM.get(key));if(input.isDamageableItem())input.setDamageValue(Math.min(17,input.getMaxDamage()-1));
                    var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("native_state_receipt",8127);input.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
                    var snapshot=input.copy();var prepared=ApotheosisCompat.apply(input,rule.maxTier(),player,118L+rules);
                    require(prepared.is(snapshot.getItem())&&prepared.getDamageValue()==snapshot.getDamageValue(),"Native End item or damage was replaced");
                    require(UnidentifiedWeaponItem.data(prepared).getInt("native_state_receipt")==8127,"Native End data was lost");
                    for(var component:snapshot.getComponents()) {
                        var componentId=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
                        if(componentId!=null&&!Set.of("minecraft","apotheosis","tnkloot").contains(componentId.getNamespace()))require(Objects.equals(prepared.get(component.type()),component.value()),"Native End component changed: "+componentId);
                    }
                    var restored=ItemStack.parseOptional(level.registryAccess(),(net.minecraft.nbt.CompoundTag)prepared.save(level.registryAccess()));
                    require(ItemStack.matches(restored,prepared),"End equipment failed native serialization");endChecks++;
                }
            }
        }
        for(String id:new String[]{"the_beyond:ring_remembrance","deeperdarker:sonorous_staff","deeperdarker:soul_elytra","ae2:charged_staff","immersiveengineering:railgun"}) {
            var key=ResourceLocation.parse(id);if(!BuiltInRegistries.ITEM.containsKey(key))continue;
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(key));require(LootProtection.keepNative(stack),"Excluded item lost its native handling "+id);
            require(FinalLoot.replacement(stack,level,player,"audit").isEmpty(),"Excluded item was converted to random loot "+id);
            require(TnkConfig.get().weapons().stream().noneMatch(w->w.id().equals(id)),"Excluded item remains in the generated pool "+id);nativeOnly++;
        }
        var pool=BossRewards.source("archaion:last_of_deepslate").orElseThrow();require(pool.items().size()==1&&pool.items().getFirst().item().equals("archaion:echo_mace"),"Last of Deepslate reward is not the Echo Mace");
        require(BossRewards.exclusive("archaion:echo_mace"),"Echo Mace escaped its boss pool");
        var bossType=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("archaion:last_of_deepslate"));var boss=(net.minecraft.world.entity.LivingEntity)bossType.create(level);
        require(boss!=null,"Cannot create Last of Deepslate");var drops=new ArrayList<ItemEntity>();
        for(String id:new String[]{"archaion:echo_charge","archaion:echo_mace_upgrade_smithing_template"})drops.add(new ItemEntity(level,0,180,0,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)),1)));
        BossRewards.drops(new LivingDropsEvent(boss,player.damageSources().playerAttack(player),drops,true));
        require(drops.size()==3,"Boss reward replaced native charges/template or duplicated its crate");
        var token=drops.stream().map(ItemEntity::getItem).filter(s->s.getItem() instanceof UnidentifiedWeaponItem).findFirst().orElseThrow();
        var mace=BossRewards.baseResult(level,token);
        require(mace.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("archaion:echo_mace"))),"Boss crate cannot identify into the Echo Mace");
        require(!LootCategory.forItem(mace).isNone(),"Echo Mace has no native affix category");
        JsonObject result=new JsonObject();result.addProperty("approved_registered_rules",rules);result.addProperty("end_native_state_checks",endChecks);result.addProperty("excluded_items_preserved",nativeOnly);result.addProperty("echo_mace_boss_and_native_materials",true);return result;
    }
}
