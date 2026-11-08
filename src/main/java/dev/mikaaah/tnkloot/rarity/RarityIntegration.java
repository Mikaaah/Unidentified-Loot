package dev.mikaaah.tnkloot.rarity;

import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.menu.*;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.*;
import java.nio.file.*;

/** Opt-in checks in an actual NeoForge/Apotheosis runtime. */
public final class RarityIntegration {
    private static final java.util.concurrent.atomic.AtomicBoolean STARTED=new java.util.concurrent.atomic.AtomicBoolean();
    private RarityIntegration() {}
    public static void run(ServerStartedEvent event) {
        if ((!Boolean.getBoolean("tnkloot.raritySmokeTest")&&!Boolean.getBoolean("tnkloot.smokeTest")) || !STARTED.compareAndSet(false,true)) return;
        var server=event.getServer();
        try(var payment=new dev.mikaaah.tnkloot.workshop.LegacyPaymentScope()) {
            var player=new FakePlayer(server.overworld(),new GameProfile(UUID.randomUUID(),"TNKRaritySmoke")) {
                @Override public boolean hasDisconnected(){return false;}
                @Override public boolean hasPermissions(int level){return true;}
            };
            int checked=0;
            for(int tier=0;tier<8;tier++) for(var category:TnkConfig.Category.values()) {
                if(WeaponSelector.pool(tier,category).isEmpty()) continue;
                for(int seed=0;seed<3;seed++) {
                    Item expected=null;
                    for(var q:RarityPolicy.get().qualities()) {
                        ItemStack token=UnidentifiedWeaponItem.create(category,tier);
                        var tag=UnidentifiedWeaponItem.data(token);tag.putLong("roll_seed",seed);token.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
                        RaritySupport.force(token,q.id());
                        ItemStack base=UnidentifiedWeaponItem.baseResult(player.level(),token);
                        require(!base.isEmpty(),"Empty base");
                        if(expected==null)expected=base.getItem();else require(expected==base.getItem(),"Rarity changed equipment pool");
                        ItemStack result=ApotheosisCompat.apply(base,tier,player,seed);
                        require(UnidentifiedWeaponItem.data(result).getString("tnk_rarity").equals(q.id()),"Result rarity changed");
                        ItemAffixes affixes=result.getOrDefault(Apoth.Components.AFFIXES,ItemAffixes.EMPTY);
                        require(dev.shadowsoffire.apotheosis.loot.LootCategory.forItem(result).isNone()||affixes.size()>0,"No live affixes");
                        var band=RarityPolicy.get().bands().get(tier);
                        for(var key:affixes.keySet()) require(affixes.getLevel(key)>=band.min()-1e-6&&affixes.getLevel(key)<=band.max()+1e-6,"Affix level outside tier band");
                        if(tier==1&&q.id().equals("legendary")&&category==TnkConfig.Category.WEAPON&&!dev.shadowsoffire.apotheosis.loot.LootCategory.forItem(result).isNone())require(affixes.size()>=3,"Legendary early weapon lacks additional affixes");
                        String locked=UnidentifiedWeaponItem.data(token).getString("tnk_rarity");
                        UnidentifiedWeaponItem.bindRollSeed(token);require(locked.equals(UnidentifiedWeaponItem.data(token).getString("tnk_rarity")),"Rebinding rerolled rarity");
                        checked++;
                    }
                }
            }
            if (net.neoforged.fml.ModList.get().isLoaded("irons_jewelry")) {
                Class<?> data=Class.forName("io.redspace.ironsjewelry.core.data.JewelryData");
                for(int tier:new int[]{0,1,3,5,7})for(var q:RarityPolicy.get().qualities()) {
                    var token=UnidentifiedWeaponItem.create(TnkConfig.Category.ACCESSORY,tier);RaritySupport.force(token,q.id());
                    var base=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("irons_jewelry:ring")));
                    RaritySupport.base(base,token);var result=ApotheosisCompat.apply(base,tier,player,81L);
                    Object jewelry=data.getMethod("get",ItemStack.class).invoke(null,result);
                    require((boolean)data.getMethod("isValid").invoke(jewelry),"Blank native jewelry");
                    require(UnidentifiedWeaponItem.data(result).getString("tnk_rarity").equals(q.id()),"Native jewelry rarity lost");
                }
            }
            // A boss-bound roll uses the boss distribution and keeps native reward data/materials.
            var bossPath=dev.mikaaah.tnkloot.compat.BossRewards.path();String bossPrevious=Files.readString(bossPath);
            try {
                Files.writeString(bossPath,"""
                    {"enabled":true,"require_player_kill":true,"pools":[{"id":"tnkloot/rarity_test","mod":"tnkloot","entity":"minecraft:zombie","name":"Test Boss","tier":1,"enabled":true,"chance":1,"additional_if_no_equipment":true,"items":[{"item":"minecraft:diamond_sword","category":"weapon","weight":1}]}]}
                    """);
                require(dev.mikaaah.tnkloot.compat.BossRewards.reload(),"Could not load test boss pool");
                var pool=dev.mikaaah.tnkloot.compat.BossRewards.find("tnkloot/rarity_test").orElseThrow();
                var original=new ItemStack(Items.DIAMOND_SWORD);original.setDamageValue(23);original.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Native Trophy"));
                var sealed=dev.mikaaah.tnkloot.compat.BossRewards.token(pool,TnkConfig.Category.WEAPON,player.level(),original);
                var bossTag=UnidentifiedWeaponItem.data(sealed);
                require(bossTag.getString("tnk_rarity").equals(RarityPolicy.get().roll(1,true,bossTag.getLong("roll_seed")).id()),"Boss used normal rarity weights");
                var restored=UnidentifiedWeaponItem.baseResult(player.level(),sealed);
                require(restored.is(Items.DIAMOND_SWORD)&&!bossTag.contains("boss_result")&&bossTag.getBoolean("boss_universal"),"Boss reward was preselected");
                var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(player.level());
                java.util.Collection<net.minecraft.world.entity.item.ItemEntity> drops=new java.util.ArrayList<>();
                drops.add(new net.minecraft.world.entity.item.ItemEntity(player.level(),0,0,0,new ItemStack(Items.ROTTEN_FLESH)));
                var dropEvent=new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(zombie,player.damageSources().playerAttack(player),drops,true);
                dev.mikaaah.tnkloot.compat.BossRewards.drops(dropEvent);require(drops.size()==2,"Boss additional reward missing");
                dev.mikaaah.tnkloot.compat.BossRewards.drops(dropEvent);require(drops.size()==2,"Boss reward duplicated");
                require(drops.stream().anyMatch(d->d.getItem().is(Items.ROTTEN_FLESH)),"Boss materials removed");
            } finally {Files.writeString(bossPath,bossPrevious);require(dev.mikaaah.tnkloot.compat.BossRewards.reload(),"Could not restore boss definitions");}
            // Optional Obscure Tooltips filters must match the actual serialized component layout.
            for(var q:RarityPolicy.get().qualities()) {
                var token=UnidentifiedWeaponItem.create(1);RaritySupport.force(token,q.id());
                try(var stream=RarityIntegration.class.getResourceAsStream("/assets/tnkloot/tooltips/definition/"+q.id()+".json")) {
                    var definition=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject();
                    var filter=net.minecraft.nbt.TagParser.parseTag(definition.getAsJsonObject("filter").get("nbt").getAsString());
                    var saved=(net.minecraft.nbt.CompoundTag)token.save(server.registryAccess());
                    require(net.minecraft.nbt.NbtUtils.compareNbt(filter,saved.getCompound("components"),true),"Tooltip rarity filter does not match component layout");
                }
            }
            var template=UnidentifiedWeaponItem.creativeSample(TnkConfig.Category.WEAPON,1);
            require(!UnidentifiedWeaponItem.data(template).contains("tnk_rarity"),"Creative template has fixed rarity");
            Set<Long> seeds=new HashSet<>();Set<String> rarities=new HashSet<>();
            for(int i=0;i<100;i++) {
                var copy=template.copy();require(UnidentifiedWeaponItem.bindRollSeed(copy),"Template did not bind");
                var tag=UnidentifiedWeaponItem.data(copy);require(seeds.add(tag.getLong("roll_seed")),"Duplicate creative seeds");rarities.add(tag.getString("tnk_rarity"));
                require(!UnidentifiedWeaponItem.bindRollSeed(copy),"Bound template changed again");
            }
            require(rarities.size()>1,"Creative templates all share rarity");
            ItemStack old=UnidentifiedWeaponItem.create(1);var oldTag=UnidentifiedWeaponItem.data(old);oldTag.remove("tnk_rarity");long oldSeed=oldTag.getLong("roll_seed");old.set(DataComponents.CUSTOM_DATA,CustomData.of(oldTag));
            require(UnidentifiedWeaponItem.bindRollSeed(old),"Legacy rarity not migrated");require(UnidentifiedWeaponItem.data(old).getLong("roll_seed")==oldSeed,"Legacy seed changed");
            var commandToken=UnidentifiedWeaponItem.create(1);
            player.getInventory().setItem(player.getInventory().selected,commandToken);
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2),"tnkloot rarity legendary");
            require(UnidentifiedWeaponItem.data(commandToken).getString("tnk_rarity").equals("legendary"),"Operator rarity command failed");
            var editor=new TnkMenu(TnkMenus.ADMIN.get(),78,player.getInventory(),true);
            require(editor.clickMenuButton(player,100),"Could not choose weapon group");
            require(editor.clickMenuButton(player,1001),"Could not choose tier");
            require(editor.clickMenuButton(player,3),"Rarity information button failed");
            editor.removed(player);
            // Seven differently rare tokens: verify quote, one payment, and all finished items on early close.
            player.getInventory().clearContent();var menu=new TnkMenu(TnkMenus.IDENTIFY.get(),77,player.getInventory(),false);
            int total=0;Map<String,Integer> wanted=new HashMap<>();
            for(int i=0;i<7;i++) {
                ItemStack token=UnidentifiedWeaponItem.create(1);String id=RarityPolicy.get().qualities().get(i%5).id();RaritySupport.force(token,id);
                total+=UnidentifiedWeaponItem.cost(token,UnidentifiedWeaponItem.baseResult(player.level(),token));wanted.merge(id,1,Integer::sum);menu.getSlot(i).set(token);
            }
            player.getInventory().setItem(0,new ItemStack(Items.EMERALD,total));menu.broadcastChanges();
            require(menu.quotedCost()==total,"Rarity quote is wrong");require(menu.clickMenuButton(player,0)&&menu.busy(),"Rarity batch failed to start");
            require(player.getInventory().countItem(Items.EMERALD)==0,"Incorrect rarity payment");menu.removed(player);
            Map<String,Integer> found=new HashMap<>();int count=0;
            for(var stack:player.getInventory().items)if(!stack.isEmpty()){
                require(!(stack.getItem() instanceof UnidentifiedWeaponItem),"Token returned after paying");
                found.merge(UnidentifiedWeaponItem.data(stack).getString("tnk_rarity"),stack.getCount(),Integer::sum);count+=stack.getCount();
            }
            require(count==7&&found.equals(wanted),"Lost rarity or results on close");menu.removed(player);
            require(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum()==7,"Repeated close duplicated results");
            // Weight reload must affect new drops while an existing Legendary remains Legendary.
            Path path=TnkConfig.path().resolveSibling("rarities.json");String previous=Files.readString(path);
            try {
                var config=com.google.gson.JsonParser.parseString(previous).getAsJsonObject();
                for(var value:config.getAsJsonArray("qualities"))value.getAsJsonObject().addProperty("weight",value.getAsJsonObject().get("id").getAsString().equals("common")?1:0);
                Files.writeString(path,config.toString());require(RaritySupport.reload(),"Valid rarity reload failed");
                ItemStack legendary=UnidentifiedWeaponItem.create(1);RaritySupport.force(legendary,"legendary");
                for(int i=0;i<20;i++)require(UnidentifiedWeaponItem.data(UnidentifiedWeaponItem.create(1)).getString("tnk_rarity").equals("common"),"New weights not applied");
                require(UnidentifiedWeaponItem.data(legendary).getString("tnk_rarity").equals("legendary"),"Existing rarity changed after reload");
            } finally {Files.writeString(path,previous);require(RaritySupport.reload(),"Could not restore rarity config");}
            dev.mikaaah.tnkloot.boss.BossIntegration.verify(player);
            TnkLoot.LOG.info("TNKLOOT_RARITY_SMOKE_OK: {} tier/category/rarity result checks, locked pools/seeds, creative independence, legacy migration, weighted reload and seven-item payment/close",checked);
            server.halt(false);
        } catch(Throwable ex){TnkLoot.LOG.error("TNKLOOT_RARITY_SMOKE_FAILED",ex);server.halt(false);}
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
