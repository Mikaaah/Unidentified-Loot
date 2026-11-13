package dev.mikaaah.tnkloot.verification;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkloot.dungeon.*;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.*;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.tiers.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Checks the real optional mixins, registry-backed templates and saved identification results. */
public final class DungeonRewardSmoke {
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static Map<ResourceLocation, JsonObject> read(MinecraftServer server, String prefix) throws Exception {
        Map<ResourceLocation, JsonObject> result = new LinkedHashMap<>();
        for (var resource : server.getResourceManager().listResources(prefix, id -> id.getPath().endsWith(".json")).entrySet()) {
            try (var reader = resource.getValue().openAsReader()) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                if (!DungeonData.modsPresent(json)) continue;
                var source = resource.getKey(); String path = source.getPath();
                result.put(ResourceLocation.fromNamespaceAndPath(source.getNamespace(), path.substring(prefix.length() + 1, path.length() - 5)), json);
            }
        }
        return result;
    }
    public static JsonObject verify(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld(); var initial = DungeonData.catalog(); JsonObject results = new JsonObject();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "TNKDungeon"));
        var rawPresets = read(server, DungeonData.PRESETS); var rawTables = read(server, DungeonData.TABLES);
        ResourceLocation id = ResourceLocation.parse("tnk_spiral:chests/floor_1");
        require(initial.tables().size() == 6, "Six example floor definitions did not load");
        Object floorsSnapshot = SpiralCompat.statics("encounter.data.FloorCatalog", "snapshot");
        Map<?,?> floors = (Map<?,?>)SpiralCompat.get(floorsSnapshot,"floors");
        Class<?> rewardsType = Class.forName("com.pla.spiral_abyss.encounter.data.FloorDefinition$Rewards");
        var nativeRoll = Class.forName("com.pla.spiral_abyss.encounter.FloorRewards").getMethod("roll", ServerLevel.class, Vec3.class, rewardsType, int.class, int.class);
        var previewType = Class.forName("com.pla.spiral_abyss.encounter.RewardPreviews");
        var previewMethod = previewType.getMethod("floor",rewardsType,Set.class);
        Object previewService = previewType.getConstructor(MinecraftServer.class).newInstance(server);
        int floorCases=0;
        try {
            var currency=initial.tables().get(id).normal().stream().filter(e->e.template().is(Items.EMERALD)).findFirst().orElseThrow();
            var resources=DungeonEquipment.reward(currency,level,new Random(123));
            require(ItemStack.isSameItemSameComponents(resources,new ItemStack(Items.EMERALD)),"Ordinary resources received unique equipment data and stopped stacking");
            results.addProperty("ordinary_resources_stack_normally",true);
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,Vec3.ZERO).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(Optional.empty());
            var ops=RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
            var explicit=SealRewardFunction.CODEC.codec().parse(ops,JsonParser.parseString("{\"tier\":4,\"source_level\":80,\"rarity\":\"epic\",\"preset\":\"tnk_spiral:rare_crown\"}")).getOrThrow();
            var exactFunctionToken=explicit.apply(new ItemStack(Items.DIAMOND_HELMET),context);
            require(DungeonEquipment.sealed(exactFunctionToken) && UnidentifiedWeaponItem.baseResult(level,exactFunctionToken).is(Items.DIAMOND_HELMET),"Vanilla seal_reward lost its selected actual item");
            require(ItemStack.matches(exactFunctionToken,explicit.apply(exactFunctionToken.copy(),context)),"Repeating seal_reward rewrote a completed token");
            var wrapper=server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,id));
            require(!wrapper.getRandomItems(params).isEmpty(),"Thin vanilla dungeon reward wrapper did not generate loot");
            results.addProperty("vanilla_loot_functions",true);
            // Real six-floor example through Spiral's actual roll and preview methods.
            for(int floor=1;floor<=6;floor++) {
                Object offered=SpiralCompat.get(floors.get(floor),"rewards");
                var table=DungeonData.table(ResourceLocation.parse("tnk_spiral:chests/floor_"+floor)).orElseThrow();
                var previews=(List<?>)previewMethod.invoke(previewService,offered,Set.of());
                require(previews.size()==table.availableNormal().size()+3,"Floor overview roster disagrees with actual pools");
                for(int chest=1;chest<=3;chest++) {
                    Object bundle=nativeRoll.invoke(null,level,Vec3.ZERO,offered,chest,1);
                    Set<?> rare=(Set<?>)SpiralCompat.get(bundle,"rareItems");
                    require(rare.size()<=1 && (chest==3 || rare.isEmpty()),"Rare prize escaped golden-only/one-prize rules");
                    for(Object value:(List<?>)SpiralCompat.get(bundle,"items")) {
                        var stack=(ItemStack)value; require(!stack.isEmpty(),"Empty floor reward");
                        if(DungeonEquipment.sealed(stack)) {
                            var result=UnidentifiedWeaponItem.baseResult(level,stack);
                            require(!result.isEmpty() && DungeonEquipment.authored(result),"Unidentified floor reward lost its saved item");
                            require(ItemStack.matches(result,ApotheosisCompat.apply(result.copy(),7,player,999)),"Identification rerolled an exact reward");
                        }
                    }
                    floorCases++;
                }
            }
            results.addProperty("floor_chest_cases",floorCases);

            JsonObject authored=rawTables.get(id).deepCopy(); authored.remove("rarities");authored.addProperty("rarity","epic");
            authored.addProperty("tier",4);authored.addProperty("source_level",80);
            authored.add("normal",JsonParser.parseString("[{\"id\":\"scholar\",\"item\":\"minecraft:iron_helmet\",\"name\":\"Scholar Hood\",\"weight\":20,\"preset\":\"tnk_spiral:adventurer\"},{\"id\":\"sentinel\",\"item\":\"minecraft:iron_helmet\",\"name\":\"Sentinel Hood\",\"weight\":30,\"preset\":\"tnk_spiral:rare_crown\"},{\"id\":\"potion\",\"item\":\"minecraft:potion\",\"weight\":50,\"components\":{\"minecraft:potion_contents\":{\"potion\":\"minecraft:strong_healing\"}}}]"));
            authored.getAsJsonObject("rare").addProperty("chance",1.0);
            rawTables.put(id,authored);
            DungeonData.replace(DungeonData.parse(rawPresets,rawTables,level.registryAccess()));
            var table=DungeonData.table(id).orElseThrow();Object offered=SpiralCompat.get(floors.get(1),"rewards");
            level.getRandom().setSeed(452918);
            var previews=(List<?>)previewMethod.invoke(previewService,offered,Set.of());
            require(level.getRandom().nextLong()==RandomSource.create(452918).nextLong(),"Preview consumed world RNG");
            require(previews.size()==6,"Variant or rare preview omitted");
            Set<ResourceLocation> identities=new HashSet<>();double normalTotal=0,rareTotal=0;
            int helmetVariants=0,potions=0;
            for(Object preview:previews) {
                require(identities.add((ResourceLocation)SpiralCompat.get(preview,"id")),"Two variants shared one preview identity");
                var icon=(ItemStack)SpiralCompat.get(preview,"icon"); require(!icon.isEmpty() && DungeonEquipment.preview(icon),"Preview icon lost its real stack/components");
                if((boolean)SpiralCompat.get(preview,"rare"))rareTotal+=(double)SpiralCompat.get(preview,"displayChance");
                else {
                    normalTotal+=(double)SpiralCompat.get(preview,"displayChance");
                    if(icon.is(Items.IRON_HELMET))helmetVariants++;
                    if(icon.is(Items.POTION)) { require(icon.get(DataComponents.POTION_CONTENTS).potion().orElseThrow().unwrapKey().orElseThrow().location().toString().equals("minecraft:strong_healing"),"Potion preview became a blank bottle");potions++; }
                }
            }
            require(helmetVariants==2 && potions==1 && Math.abs(normalTotal-1)<1e-12 && Math.abs(rareTotal-1)<1e-12,"Displayed odds/variants disagree with configured distributions");
            results.addProperty("preview_variants",previews.size());results.addProperty("preview_rng_unchanged",true);
            var floorPreviewType=Class.forName("com.pla.spiral_abyss.network.AbyssNetwork$FloorPreview");
            var floorPacket=floorPreviewType.getConstructor(int.class,List.class,List.class).newInstance(1,List.of(),previews);
            var network=Class.forName("com.pla.spiral_abyss.network.AbyssNetwork");
            var write=network.getDeclaredMethod("writePreviews",net.minecraft.network.RegistryFriendlyByteBuf.class,List.class);write.setAccessible(true);
            var read=network.getDeclaredMethod("readPreviews",net.minecraft.network.RegistryFriendlyByteBuf.class);read.setAccessible(true);
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            try {
                write.invoke(null,buffer,List.of(floorPacket));
                var restoredFloors=(List<?>)read.invoke(null,buffer);
                var restoredPreviews=(List<?>)SpiralCompat.get(restoredFloors.getFirst(),"rewards");
                require(restoredPreviews.size()==previews.size(),"Network codec lost preview variants");
                for(int i=0;i<previews.size();i++)require(ItemStack.matches((ItemStack)SpiralCompat.get(previews.get(i),"icon"),(ItemStack)SpiralCompat.get(restoredPreviews.get(i),"icon")),"Network codec discarded preset/potion components");
            } finally {buffer.release();}
            results.addProperty("native_preview_network_roundtrip",true);

            var crown=table.normal().get(1);Set<Long> seeds=new HashSet<>();Random random=new Random(871234L);
            ItemStack savedToken=ItemStack.EMPTY,savedResult=ItemStack.EMPTY;int exact=0;
            for(int i=0;i<96;i++) {
                ItemStack token=DungeonEquipment.reward(crown,level,random);require(DungeonEquipment.sealed(token),"Equipment was not wrapped");
                require(seeds.add(UnidentifiedWeaponItem.data(token).getLong("roll_seed")),"Independent equipment rewards reused a seed");
                var stack=UnidentifiedWeaponItem.baseResult(level,token);var expected=stack.copy();
                require(stack.is(Items.IRON_HELMET) && stack.getHoverName().getString().equals("Sentinel Hood"),"Specific equipment or custom name lost");
                var protection=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.parse("minecraft:protection")));
                require(EnchantmentHelper.getItemEnchantmentLevel(protection,stack)==4,"Fixed enchantment changed");
                require(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().anyMatch(e->e.attribute().equals(Attributes.ARMOR)),"Preset removed native armour");
                require(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().anyMatch(e->e.attribute().equals(Attributes.MAX_HEALTH)&&e.modifier().amount()==4),"Guaranteed native bonus was lost");
                var fixed=AffixRegistry.INSTANCE.holder(ResourceLocation.parse("tnkloot:armor/attribute/blessed"));
                require(Math.abs(stack.get(Apoth.Components.AFFIXES).getLevel(fixed)-.65)<1e-6,"Fixed affix strength changed");
                require(ItemStack.matches(expected,ApotheosisCompat.apply(stack,0,player,random.nextLong())),"Later owner cap or identification rerolled saved stats");
                var restored=ItemStack.parseOptional(level.registryAccess(),(net.minecraft.nbt.CompoundTag)token.save(level.registryAccess()));
                require(ItemStack.matches(expected,UnidentifiedWeaponItem.baseResult(level,restored)),"Serialized token lost its completed result");
                if(i==0){savedToken=token.copy();savedResult=expected.copy();}
                exact++;
            }
            // A completely new monthly catalog must not affect old tokens or their saved reforge plans.
            DungeonData.replace(new DungeonData.Catalog(Map.of(),Map.of()));
            require(ItemStack.matches(savedResult,UnidentifiedWeaponItem.baseResult(level,savedToken)),"Monthly rotation changed an earned unidentified result");
            var rarity=RarityRegistry.INSTANCE.getValue(ResourceLocation.parse("tnkloot:epic"));
            var reforged=LootController.createLootItem(savedResult.copy(),rarity,GenContext.forPlayer(RandomSource.create(4321),player));
            require(AffixHelper.getRarity(reforged).getId().equals(AffixHelper.getRarity(savedResult).getId()),"Reforging scaled fixed bonuses by changing rarity");
            require(reforged.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(savedResult.get(DataComponents.ATTRIBUTE_MODIFIERS)),"Reforging replaced fixed/native attributes");
            require(reforged.get(DataComponents.ENCHANTMENTS).equals(savedResult.get(DataComponents.ENCHANTMENTS)),"Reforging replaced fixed enchantments");
            var fixed=AffixRegistry.INSTANCE.holder(ResourceLocation.parse("tnkloot:armor/attribute/blessed"));
            require(Math.abs(reforged.get(Apoth.Components.AFFIXES).getLevel(fixed)-.65)<1e-6,"Reforging replaced fixed affix level");
            require(reforged.getHoverName().getString().equals("Sentinel Hood"),"Reforging renamed the prize");
            results.addProperty("exact_equipment_save_identify_checks",exact);results.addProperty("monthly_rotation_and_reforge_preserved",true);
            // Run the real paid NPC batch after removing every current reward definition.
            player.giveExperiencePoints(1_000_000);
            for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.EMERALD,64));
            var menu=new dev.mikaaah.tnkloot.menu.TnkMenu(dev.mikaaah.tnkloot.menu.TnkMenus.IDENTIFY.get(),789,player.getInventory(),false);
            menu.getSlot(0).set(savedToken.copy());
            require(menu.clickMenuButton(player,0) && menu.busy(),"NPC refused saved exact equipment");
            var previewField=menu.getClass().getDeclaredField("previews");previewField.setAccessible(true);
            var exactPreview=(List<?>)((List<?>)previewField.get(menu)).getFirst();
            require(exactPreview.size()==1 && ItemStack.matches((ItemStack)exactPreview.getFirst(),DungeonEquipment.hint(level,savedToken)),"NPC used a global equipment roulette for an exact reward");
            var animate=menu.getClass().getDeclaredMethod("animate",net.minecraft.server.level.ServerPlayer.class);animate.setAccessible(true);
            for(int tick=0;menu.busy()&&tick<200;tick++)animate.invoke(menu,player);
            require(!menu.busy() && ItemStack.matches(menu.getSlot(0).getItem(),savedResult),"NPC changed the sealed item during its paid reveal");
            results.addProperty("paid_npc_saved_result",true);
            var profile=(net.minecraft.nbt.CompoundTag)Class.forName("dev.mikaaah.tnkprogression.ProgressionAPI").getMethod("profile",net.minecraft.server.level.ServerPlayer.class).invoke(null,player);
            profile.putInt("level",100);profile.putInt("ceiling",100);
            var reforgingPosition=new net.minecraft.core.BlockPos(0,190,0);var oldState=level.getBlockState(reforgingPosition);
            try {
                level.setBlock(reforgingPosition,Apoth.Blocks.REFORGING_TABLE.value().defaultBlockState(),3);
                var reforgeMenu=new dev.shadowsoffire.apotheosis.affix.reforging.ReforgingMenu(790,player.getInventory(),reforgingPosition);
                reforgeMenu.getSlot(0).set(savedResult.copy());
                var wrong=RarityRegistry.INSTANCE.getValue(ResourceLocation.parse("tnkloot:legendary"));
                reforgeMenu.getSlot(1).set(new ItemStack(wrong.getMaterial(),64));
                reforgeMenu.getSlot(2).set(new ItemStack(Apoth.Items.SIGIL_OF_REBIRTH,64));
                require(reforgeMenu.getRarity()==null && reforgeMenu.getSlot(3).getItem().isEmpty(),"Wrong material can pay for a rarity-locked reward");
                reforgeMenu.getSlot(1).set(new ItemStack(rarity.getMaterial(),64));
                reforgeMenu.slotsChanged(player.getInventory());
                require(reforgeMenu.getRarity()==rarity && !reforgeMenu.getSlot(3).getItem().isEmpty(),"Matching dungeon rarity cannot be reforged");
            } finally { level.setBlock(reforgingPosition,oldState,3); }
            results.addProperty("reforge_material_and_rarity",true);
            DungeonData.replace(DungeonData.parse(rawPresets,rawTables,level.registryAccess()));

            var bosses=(List<?>)SpiralCompat.statics("encounter.JournalPreviews","bosses",player);
            require(!bosses.isEmpty(),"Actual Spiral journal returned no bosses");
            for(Object boss:bosses) {
                var journalRewards=(List<?>)SpiralCompat.get(boss,"rewards");
                require(journalRewards.size()==3 && journalRewards.stream().allMatch(p->(boolean)SpiralCompat.get(p,"rare")&&DungeonEquipment.preview((ItemStack)SpiralCompat.get(p,"icon"))),"Journal does not use the configured rare equipment variants");
            }
            results.addProperty("journal_bosses_checked",bosses.size());

            int golden=0;Map<String,Integer> rareCounts=new HashMap<>();int partyCases=0;
            for(int chest=1;chest<=3;chest++)for(int players:new int[]{1,2,4}) {
                Object bundle=nativeRoll.invoke(null,level,Vec3.ZERO,offered,chest,players);
                List<?> stacks=(List<?>)SpiralCompat.get(bundle,"items");Set<?> rareIndices=(Set<?>)SpiralCompat.get(bundle,"rareItems");
                require(rareIndices.size()==(chest==3?1:0),"Golden-only shared cap failed for party size "+players);
                require(stacks.size()==chest*players+rareIndices.size(),"Normal table roll count changed with party size");
                if(chest==3)for(Object position:rareIndices) {var prize=(ItemStack)stacks.get((int)position);require(!DungeonEquipment.sealed(prize)&&prize.getCount()==1,"Golden prize was unidentified or stacked");}
                partyCases++;
            }
            // All prizes remain possible, and weights work inside the one-prize cap.
            for(int i=0;i<900;i++) {
                Object bundle=nativeRoll.invoke(null,level,Vec3.ZERO,offered,3,1);List<?> items=(List<?>)SpiralCompat.get(bundle,"items");Set<?> rare=(Set<?>)SpiralCompat.get(bundle,"rareItems");
                require(rare.size()==1,"Guaranteed rare check failed");
                var stack=(ItemStack)items.get((int)rare.iterator().next());rareCounts.merge(stack.getHoverName().getString(),1,Integer::sum);golden++;
                var saved=SpiralCompat.call(bundle,"save",level.registryAccess());
                var restored=SpiralCompat.statics("encounter.FloorRewards$Bundle","load",saved,level.registryAccess());
                require(((Set<?>)SpiralCompat.get(restored,"rareItems")).size()==1,"Saved payout lost rare announcement marker");
            }
            require(rareCounts.size()==3,"Some weighted rare prizes never dropped");
            require(Math.abs(rareCounts.get("Arcane Champion's Blade")/900.0-.5)<.07,"Rare blade weight differs from preview");
            results.addProperty("golden_payouts_checked",golden);results.addProperty("party_chest_cases",partyCases);results.add("rare_prize_counts",new Gson().toJsonTree(rareCounts));

            // Exercise Spiral's real pickup announcer without modifying its event path.
            var prize=DungeonEquipment.reward(table.rare().entries().orElseThrow().getFirst(),level,new Random(22));
            var entity=(net.minecraft.world.entity.item.ItemEntity)Class.forName("com.pla.spiral_abyss.entity.ChestLootEntity")
                .getConstructor(net.minecraft.world.level.Level.class,double.class,double.class,double.class,ItemStack.class,boolean.class)
                .newInstance(level,0.0,190.0,0.0,prize.copy(),true);
            SpiralCompat.statics("event.RareRewardBroadcast","markDrop",entity,1);
            var postType=Class.forName("com.pla.spiral_abyss.platform.event.AbyssEvents$ItemEntityPickupEvent$Post");
            var postConstructor=postType.getConstructor(net.minecraft.world.entity.player.Player.class,net.minecraft.world.entity.item.ItemEntity.class,ItemStack.class,ItemStack.class);
            SpiralCompat.statics("event.RareRewardBroadcast","pickedUp",postConstructor.newInstance(player,entity,prize.copy(),prize.copy()));
            require(!entity.getPersistentData().getBoolean("spiral_abyss_reward_announced"),"Rare reward announced before pickup");
            SpiralCompat.statics("event.RareRewardBroadcast","pickedUp",postConstructor.newInstance(player,entity,prize.copy(),ItemStack.EMPTY));
            require(entity.getPersistentData().getBoolean("spiral_abyss_reward_announced"),"Completed rare prize lost native pickup announcement");
            results.addProperty("native_rare_pickup_announcement",true);

            // Failed authoring must leave the complete previous catalog intact.
            var good=DungeonData.catalog();var invalid=new LinkedHashMap<>(rawPresets);
            invalid.put(ResourceLocation.parse("tnk_spiral:bad"),JsonParser.parseString("{\"attributes\":[{\"attribute\":\"tnkloot:does_not_exist\",\"amount\":1}]}").getAsJsonObject());
            boolean rejected=false;try{DungeonData.replace(DungeonData.parse(invalid,rawTables,level.registryAccess()));}catch(IllegalArgumentException expected){rejected=true;}
            require(rejected && DungeonData.catalog()==good,"Invalid monthly data replaced the working catalog");
            results.addProperty("invalid_reload_retained_catalog",true);

            if(ModList.get().isLoaded("irons_spellbooks")) {
                DungeonData.replace(initial);
                var magic=DungeonData.table(id).orElseThrow().normal().stream().filter(e->BuiltInRegistries.ITEM.getKey(e.template().getItem()).getNamespace().equals("irons_spellbooks")).findFirst().orElseThrow();
                for(int i=0;i<48;i++) {
                    var token=DungeonEquipment.reward(magic,level,new Random(9187L+i));var stack=UnidentifiedWeaponItem.baseResult(level,token);
                    require(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("irons_spellbooks:wizard_helmet"),"Magic reward rerolled to global gear");
                    require(stack.get(Apoth.Components.AFFIXES).keySet().stream().allMatch(h->h.getId().getPath().startsWith("dungeon/armor/")),"Magic-only random pool selected a physical affix");
                    require(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().anyMatch(e->e.attribute().unwrapKey().orElseThrow().location().toString().equals("irons_spellbooks:max_mana")&&e.modifier().amount()==25),"Magic mana guarantee missing");
                    require(stack.getAttributeModifiers().modifiers().stream().anyMatch(e->e.attribute().unwrapKey().orElseThrow().location().toString().equals("irons_spellbooks:max_mana")&&e.modifier().amount()==25),"Native attribute hooks removed the guaranteed mana bonus");
                }
                results.addProperty("magic_helmet_checks",48);
            }
            return results;
        } finally { DungeonData.replace(initial); }
    }
}
