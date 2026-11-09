package dev.mikaaah.tnkloot.apotheosis;

import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.compat.*;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.mikaaah.tnkloot.loot.*;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingMenu;
import dev.shadowsoffire.apotheosis.affix.trades.*;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.loot.modifiers.AffixLootModifier;
import dev.shadowsoffire.apotheosis.loot.modifiers.AffixConvertLootModifier;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.tiers.*;
import dev.shadowsoffire.apotheosis.util.LootPatternMatcher;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in actual-server coverage; no test fixture is installed during normal play. */
public final class LootPoolSmoke {
    private LootPoolSmoke() {}
    private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
    private static CompoundTag profile(ServerPlayer player) throws Exception {
        return (CompoundTag)Class.forName("dev.mikaaah.tnkprogression.ProgressionAPI").getMethod("profile",ServerPlayer.class).invoke(null,player);
    }
    private static FakePlayer player(ServerLevel level) throws Exception {
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"TNKPools"));
        profile(player).putInt("level",100);profile(player).putInt("ceiling",100);ApotheosisIntegration.force(player);return player;
    }
    private static LootParams params(ServerLevel level,ServerPlayer player,LootContextParamSet type) {
        var builder=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO);
        if(player!=null)builder.withParameter(LootContextParams.THIS_ENTITY,player);
        if(type==LootContextParamSets.FISHING)builder.withParameter(LootContextParams.TOOL,new ItemStack(Items.FISHING_ROD));
        return builder.create(type);
    }
    private static LootContext context(LootParams params,String id,long seed) {
        var context=new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty());context.setQueriedLootTableId(ResourceLocation.parse(id));return context;
    }
    private static long chestTokens(List<ItemStack> stacks) {
        return stacks.stream().filter(stack->stack.getItem() instanceof UnidentifiedWeaponItem&&UnidentifiedWeaponItem.data(stack).getString("source_type").equals("tnk_chest")).count();
    }
    private static void bounded(ItemStack stack) {
        require(!stack.isEmpty(),"Empty native equipment");
        require(AffixHelper.getRarity(stack).isBound()&&AffixHelper.getRarity(stack).getId().getNamespace().equals("tnkloot"),"Foreign native rarity");
        require(AffixHelper.hasAffixes(stack),"Missing affixes on "+stack.getItem());
        for(var affix:AffixHelper.getAffixes(stack).values()) {
            require(affix.affix().getId().getNamespace().equals("tnkloot"),"Foreign affix");
            require(affix.level()<=UnifiedApotheosis.maximum(stack)+0.00001f,"Native affix exceeded UL band");
        }
    }
    private static void materials(List<ItemStack> before,List<ItemStack> after,String id,boolean independentRolls) {
        for(var material:before) {
            if(material.getItem() instanceof UnidentifiedWeaponItem||FinalLoot.category(material)!=null||BossRewards.exclusive(BuiltInRegistries.ITEM.getKey(material.getItem()).toString())||!LootCategory.forItem(material).isNone()||AffixHelper.hasAffixes(material))continue;
            // Exploration maps allocate saved-data IDs and can locate a different
            // unreferenced structure on a second roll, even with the same loot seed.
            boolean generatedMap=independentRolls&&material.is(Items.FILLED_MAP);
            java.util.function.Predicate<ItemStack> same=stack->generatedMap?stack.is(material.getItem()):ItemStack.isSameItemSameComponents(stack,material);
            int expected=before.stream().filter(same).mapToInt(ItemStack::getCount).sum();
            int actual=after.stream().filter(same).mapToInt(ItemStack::getCount).sum();
            require(actual>=expected,"Material/key/trophy changed in "+id+": "+material);
        }
    }
    public static void run(ServerStartedEvent event) {
        if(!Boolean.getBoolean("tnkloot.lootPoolSmokeTest"))return;
        var server=event.getServer();var previous=TnkBetaConfig.current;
        try {
            var world=server.overworld();var player=player(world);
            UnifiedApotheosisSmoke.verify(player);
            int salvage=0,legacy=0,nativeRolls=0,explicit=0,chests=0,advancements=0;
            var nativeEntries=new com.google.gson.JsonArray();var chestIds=new com.google.gson.JsonArray();var rewardIds=new com.google.gson.JsonArray();
            var generation=GenContext.forPlayer(RandomSource.create(17),player);
            for(var rarity:RarityRegistry.getSortedRarities()) {
                var stack=LootController.createLootItem(new ItemStack(Items.DIAMOND_SWORD),rarity,generation);
                var matches=SalvagingMenu.findMatch(world,stack).stream().filter(match->match.value().getOutputs().stream().anyMatch(output->output.stack().is(rarity.getMaterial()))).toList();
                require(matches.size()==1,"Expected exactly one rarity salvage recipe for "+rarity+", got "+matches.stream().map(match->match.id().toString()).toList());
                for(int repeat=0;repeat<8;repeat++) {
                    var outputs=SalvagingMenu.getSalvageResults(world,stack);
                    int count=outputs.stream().filter(output->output.is(rarity.getMaterial())).mapToInt(ItemStack::getCount).sum();
                    require(count>=1&&count<=4,"Wrong salvage material/count for "+rarity);
                    require(outputs.stream().noneMatch(output->RarityRegistry.getSortedRarities().stream().anyMatch(other->other!=rarity&&output.is(other.getMaterial()))),"Another rarity's salvage material leaked");
                }
                String name=RarityRegistry.INSTANCE.getKey(rarity).getPath();
                if(!name.equals("legendary")) {
                    var old=stack.copy();old.set(Apoth.Components.RARITY,RarityRegistry.INSTANCE.holder(ResourceLocation.parse("apotheosis:"+name)));
                    require(SalvagingMenu.findMatch(world,old).stream().filter(match->match.value().getOutputs().stream().anyMatch(output->output.stack().is(rarity.getMaterial()))).count()==1,"Legacy saved rarity cannot be salvaged: "+name);legacy++;
                }
                salvage++;
            }
            require(SalvagingMenu.getSalvageResults(world,new ItemStack(Items.DIAMOND_SWORD)).stream().noneMatch(output->RarityRegistry.getSortedRarities().stream().anyMatch(rarity->output.is(rarity.getMaterial()))),"Unaffixed equipment yielded rarity material");
            for(var level:server.getAllLevels()) {
                var owner=player(level);
                for(int source:new int[]{1,10,11,30,31,50,51,70,71,80,81,94,95,100}) {
                    profile(owner).putInt("level",source);
                    var ctx=GenContext.forPlayer(RandomSource.create(112+source),owner);
                    for(int repeat=0;repeat<16;repeat++) {
                        var entry=AffixLootRegistry.INSTANCE.getRandomItem(ctx);require(entry!=null,"Empty native random pool at "+source+" in "+level.dimension().location());
                        require(NativeLootPools.allowed(entry.stack(),ctx),"Native registry bypassed UL item eligibility");
                        var stack=LootController.createRandomLootItem(ctx,null);bounded(stack);
                        require(NativeLootPools.allowed(stack,ctx),"Native random generator bypassed eligibility");
                        int tier=UnidentifiedWeaponItem.data(stack).getInt("tnk_tier");
                        require(tier<=LevelTierRoll.roll(source,99),"Native random item bypassed source tier");nativeRolls++;
                    }
                }
                profile(owner).putInt("level",100);
                var ctx=GenContext.forPlayer(RandomSource.create(19),owner);
                for(var entry:AffixLootRegistry.INSTANCE.getValues()) {
                    var holder=AffixLootRegistry.INSTANCE.holder(AffixLootRegistry.INSTANCE.getKey(entry));
                    boolean eligible=NativeLootPools.allowed(entry.stack(),ctx)&&Constraints.eval(ctx).test(entry)&&entry.weights().getWeight(ctx.tier(),ctx.luck())>0;
                    var output=LootController.createAffixItemFromPools(Set.of(),Set.of(holder),ctx);
                    require(output.isEmpty()!=eligible,"Explicit native pool policy mismatch for "+holder.getId()+" in "+ctx.dimension().location());
                    var row=new com.google.gson.JsonObject();row.addProperty("entry",holder.getId().toString());row.addProperty("item",BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString());row.addProperty("dimension",ctx.dimension().location().toString());row.addProperty("eligible",eligible);nativeEntries.add(row);
                    if(eligible)bounded(output);explicit++;
                }
            }
            var settings=new TnkBetaConfig();TnkBetaConfig.current=settings;
            for(boolean hasPlayer:new boolean[]{false,true})for(String id:List.of("tnkloot:audit/cache","minecraft:chests/simple_dungeon")) {
                settings.chestEquipmentChance=1;
                var loot=new ObjectArrayList<ItemStack>(List.of(new ItemStack(Items.IRON_SWORD),new ItemStack(Items.DIAMOND,3)));
                FinalLoot.table(loot,context(params(world,hasPlayer?player:null,LootContextParamSets.CHEST),id,31));
                require(chestTokens(loot)==1&&loot.stream().anyMatch(stack->stack.is(Items.DIAMOND)&&stack.getCount()==3),"Chest origin/no-player conversion failed");
                var token=loot.stream().filter(stack->UnidentifiedWeaponItem.data(stack).getString("source_type").equals("tnk_chest")).findFirst().orElseThrow();
                require(!UnidentifiedWeaponItem.baseResult(world,token).isEmpty(),"Chest reward is unidentifiable");
                if(!hasPlayer) {
                    var tag=UnidentifiedWeaponItem.data(token);require(tag.getInt("tnk_source_level")==1&&tag.getString("tnk_rarity").equals("common")&&tag.getBoolean("tnk_progression_sealed"),"Automation borrowed character progression");
                    var snapshot=token.copy();TnkLootRolls.cap(token,player,100);require(ItemStack.matches(snapshot,token),"Automation reward improved after transfer");
                }
                settings.chestEquipmentChance=0;loot=new ObjectArrayList<ItemStack>(List.of(new ItemStack(Items.IRON_SWORD),new ItemStack(Items.DIAMOND,3)));
                FinalLoot.table(loot,context(params(world,hasPlayer?player:null,LootContextParamSets.CHEST),id,32));
                require(chestTokens(loot)==0&&loot.stream().noneMatch(stack->stack.is(Items.IRON_SWORD)),"Zero chest frequency ignored");
            }
            settings.chestEquipmentChance=1;
            var fixed=new ItemStack(Items.DIAMOND_SWORD);fixed.set(DataComponents.CUSTOM_NAME,Component.literal("Earned fixed sword"));
            var convert=new AffixConvertLootModifier(new LootItemCondition[0],List.of(new AffixConvertLootModifier.AffixConversionEntry(LootPatternMatcher.of(".*"),1,Set.of())));
            var conversionContext=context(params(world,player,LootContextParamSets.CHEST),"tnkloot:audit/fixed_reward",330);
            var converted=convert.apply(new ObjectArrayList<ItemStack>(List.of(fixed.copy())),conversionContext);bounded(converted.getFirst());FinalLoot.table(converted,conversionContext);
            require(converted.stream().anyMatch(stack->stack.is(Items.DIAMOND_SWORD)&&Objects.equals(stack.get(DataComponents.CUSTOM_NAME),fixed.get(DataComponents.CUSTOM_NAME))),"Native affix conversion erased a fixed named reward");
            var randomFixed=fixed.copy();var randomData=UnidentifiedWeaponItem.data(randomFixed);randomData.putBoolean("tnk_random_pool",true);randomFixed.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(randomData));
            var randomContext=context(params(world,player,LootContextParamSets.CHEST),"tnkloot:audit/random_named_reward",331);
            var randomNamed=convert.apply(new ObjectArrayList<ItemStack>(List.of(randomFixed)),randomContext);FinalLoot.table(randomNamed,randomContext);
            require(randomNamed.stream().noneMatch(stack->stack.is(Items.DIAMOND_SWORD)),"Named random pool entry bypassed chest sealing");
            var fishing=new ObjectArrayList<ItemStack>(List.of(new ItemStack(Items.DIAMOND_SWORD),new ItemStack(Items.COD)));
            FinalLoot.table(fishing,context(params(world,player,LootContextParamSets.FISHING),"betterend:gameplay/fishing/treasure",33));require(chestTokens(fishing)==0,"Fishing treated as a chest");
            profile(player).putInt("level",1);
            for(boolean hasPlayer:new boolean[]{false,true}) {
                var ordinary=new ObjectArrayList<ItemStack>(List.of(new ItemStack(Items.NETHERITE_SWORD),new ItemStack(Items.COD,2)));
                FinalLoot.table(ordinary,context(params(world,hasPlayer?player:null,LootContextParamSets.FISHING),"tnkloot:audit/generic",34));
                var token=ordinary.stream().filter(stack->stack.getItem() instanceof UnidentifiedWeaponItem).findFirst().orElseThrow();
                require(UnidentifiedWeaponItem.tier(token)<=1&&UnidentifiedWeaponItem.data(token).getBoolean("tnk_progression_sealed"),"Generic replacement raised ordinary source tier or remained unsealed");
                require(ordinary.stream().anyMatch(stack->stack.is(Items.COD)&&stack.getCount()==2),"Generic replacement changed fish/materials");
            }
            profile(player).putInt("level",100);
            var registry=server.reloadableRegistries().get().registryOrThrow(Registries.LOOT_TABLE);
            var chestParams=params(world,player,LootContextParamSets.CHEST);
            for(var id:registry.keySet()) {
                var table=registry.get(id);if(table.getParamSet()!=LootContextParamSets.CHEST)continue;
                var raw=new ObjectArrayList<ItemStack>();
                table.getRandomItemsRaw(context(chestParams,id.toString(),43001),raw::add);
                var coreOnly=new ObjectArrayList<ItemStack>(raw.stream().map(ItemStack::copy).toList());FinalLoot.table(coreOnly,context(chestParams,id.toString(),43001));materials(raw,coreOnly,id.toString(),false);
                var output=table.getRandomItems(chestParams,43001L);materials(raw,output,id.toString(),true);
                require(chestTokens(output)<=1,"Duplicate normal chest rolls in "+id);
                for(var stack:output) {
                    if(stack.getItem() instanceof UnidentifiedWeaponItem)require(!UnidentifiedWeaponItem.baseResult(world,stack).isEmpty(),"Unidentifiable native-table token in "+id);
                    require(!(UnidentifiedWeaponItem.data(stack).getBoolean("tnk_random_pool")&&UnidentifiedWeaponItem.data(stack).getBoolean("tnk_external_generated")&&!(stack.getItem() instanceof UnidentifiedWeaponItem)),"Random identified gear escaped chest sealing in "+id);
                }
                chestIds.add(id.toString());chests++;
            }
            var rewardParams=params(world,player,LootContextParamSets.ADVANCEMENT_REWARD);
            for(var advancement:server.getAdvancements().getAllAdvancements())for(var key:advancement.value().rewards().loot()) {
                var table=registry.get(key);if(table==null)continue;
                var raw=new ObjectArrayList<ItemStack>();table.getRandomItemsRaw(context(rewardParams,key.location().toString(),43002),raw::add);
                var coreOnly=new ObjectArrayList<ItemStack>(raw.stream().map(ItemStack::copy).toList());FinalLoot.table(coreOnly,context(rewardParams,key.location().toString(),43002));materials(raw,coreOnly,key.location().toString(),false);
                var output=table.getRandomItems(rewardParams,43002L);materials(raw,output,key.location().toString(),true);
                require(output.stream().noneMatch(stack->stack.getItem() instanceof UnidentifiedWeaponItem),"Advancement loot sealed: "+key.location());rewardIds.add(key.location().toString());advancements++;
            }
            String bosses=Files.readString(BossRewards.path());
            try {
                var root=com.google.gson.JsonParser.parseString(bosses).getAsJsonObject();var fixture=new com.google.gson.JsonObject();
                fixture.addProperty("id","tnkloot/audit_reserved");fixture.addProperty("mod","tnkloot");fixture.addProperty("entity","minecraft:skeleton");fixture.addProperty("name","Reserved Pool Test");fixture.addProperty("tier",5);fixture.addProperty("enabled",true);fixture.addProperty("chance",1);fixture.addProperty("additional_if_no_equipment",false);
                var prize=new com.google.gson.JsonObject();prize.addProperty("item","minecraft:diamond_sword");prize.addProperty("category","weapon");prize.addProperty("weight",1);var prizes=new com.google.gson.JsonArray();prizes.add(prize);fixture.add("items",prizes);root.getAsJsonArray("pools").add(fixture);Files.writeString(BossRewards.path(),root.toString());require(BossRewards.reload(),"Fixture boss reload failed");
                var invalid=AffixLootRegistry.INSTANCE.holder(ResourceLocation.parse("apotheosis:diamond_sword"));
                var valid=AffixLootRegistry.INSTANCE.holder(ResourceLocation.parse("apotheosis:netherite_sword"));
                require(invalid.isBound()&&valid.isBound(),"Native pool fixtures missing");
                for(int i=0;i<128;i++)require(!LootController.createRandomLootItem(generation,null).is(Items.DIAMOND_SWORD),"Boss-only item escaped default native pool");
                require(LootController.createAffixItemFromPools(Set.of(),Set.of(invalid),generation).isEmpty(),"Boss-only explicit pool escaped");
                var modifier=new AffixLootModifier(new LootItemCondition[0],List.of(new AffixLootModifier.AffixTableEntry(LootPatternMatcher.of(".*"),1,Set.of(invalid),Set.of())));
                var drops=modifier.apply(new ObjectArrayList<ItemStack>(List.of(new ItemStack(Items.DIAMOND,4))),context(chestParams,"tnkloot:audit/explicit",46));
                require(drops.size()==1&&drops.getFirst().is(Items.DIAMOND),"Empty/forbidden explicit GLM changed material loot");
                var invader=Invader.modifyBossItem(new ItemStack(Items.DIAMOND_SWORD),Component.literal("Pool Boss"),generation,RarityRegistry.getSortedRarities().getLast(),0,world.registryAccess(),false);
                bounded(invader);require(!invader.is(Items.DIAMOND_SWORD)&&NativeLootPools.allowed(invader,generation),"Invader gear-set prize escaped boss exclusion");
                var owned=LootController.createLootItem(new ItemStack(Items.DIAMOND_SWORD),RarityRegistry.getSortedRarities().getLast(),generation);require(owned.is(Items.DIAMOND_SWORD),"Owned-item reforging was blocked by random-pool rules");
                var merchant=EntityType.WANDERING_TRADER.create(world);player.setPos(0,0,0);merchant.setPos(0,0,0);world.players().add(player);
                try {
                    var forbidden=new AffixTrade(new ItemCost(Items.EMERALD,5),Optional.empty(),Set.of(),List.of(invalid),false);require(forbidden.getOffer(merchant,RandomSource.create(2))==null,"Forbidden explicit trade offered");
                    var mixed=new AffixTrade(new ItemCost(Items.EMERALD,5),Optional.empty(),Set.of(),List.of(invalid,valid),false);
                    for(int i=0;i<16;i++) { var offer=mixed.getOffer(merchant,RandomSource.create(i));require(offer!=null&&offer.getResult().is(Items.NETHERITE_SWORD)&&offer.getCostA().getCount()==5,"Mixed explicit trade eligibility/payment changed");bounded(offer.getResult()); }
                    var automatic=new AutomaticAffixTrade(Set.of(),List.of(invalid,valid),false);var offer=automatic.getOffer(merchant,RandomSource.create(3));require(offer!=null&&offer.getResult().is(Items.NETHERITE_SWORD),"Automatic trade ignored filtered pool");
                } finally { world.players().remove(player); }
            } finally { Files.writeString(BossRewards.path(),bosses);require(BossRewards.reload(),"Boss fixtures not restored"); }
            var receipt=new com.google.gson.JsonObject();var mods=new com.google.gson.JsonObject();
            for(var mod:net.neoforged.fml.ModList.get().getMods())mods.addProperty(mod.getModId(),mod.getVersion().toString());
            receipt.add("runtime_mods",mods);receipt.addProperty("salvage_groups",salvage);receipt.addProperty("legacy_salvage_aliases",legacy);receipt.addProperty("native_random_rolls",nativeRolls);receipt.addProperty("explicit_entry_checks",explicit);
            receipt.add("native_entries",nativeEntries);receipt.add("chest_table_ids",chestIds);receipt.add("advancement_reward_table_ids",rewardIds);
            receipt.addProperty("fixed_named_native_payloads",true);receipt.addProperty("result","passed");receipt.addProperty("coverage","Focused runtime; all listed chest tables sampled with a fixed seed, not exhaustive random or full-pack certification.");
            Files.writeString(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("tnkloot-loot-pool-verification.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(receipt));
            TnkLoot.LOG.info("TNKLOOT_LOOT_POOLS_OK: {} salvage groups, {} legacy salvage aliases, {} native random rolls, {} explicit native entry/context checks, {} actual chest tables, {} actual advancement reward tables, playerless/nonstandard chests, fishing isolation, materials, empty GLMs, reserved invader prizes, paid explicit/automatic trades and owned reforging",salvage,legacy,nativeRolls,explicit,chests,advancements);
        } catch(Throwable error) { TnkLoot.LOG.error("TNKLOOT_LOOT_POOLS_FAILED",error); }
        finally { TnkBetaConfig.current=previous;server.halt(false); }
    }
}
