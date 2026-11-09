package dev.mikaaah.tnkloot.apotheosis;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.injection.FinalLoot;
import dev.mikaaah.tnkloot.loot.*;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.*;
import dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingMenu;
import dev.shadowsoffire.apotheosis.affix.reforging.*;
import dev.shadowsoffire.apotheosis.loot.*;
import dev.shadowsoffire.apotheosis.mobs.registries.InvaderRegistry;
import dev.shadowsoffire.apotheosis.tiers.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Real-runtime regression for shared generation, native reforging and invader post-roll boosts. */
public final class UnifiedApotheosisSmoke {
    private UnifiedApotheosisSmoke() {}
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    private static void bounded(ItemStack stack) {
        require(!stack.isEmpty(),"Empty generated equipment");
        var rarity=AffixHelper.getRarity(stack);require(rarity.isBound() && rarity.getId().getNamespace().equals("tnkloot"),"Native rarity escaped: "+rarity);
        var affixes=stack.getOrDefault(Apoth.Components.AFFIXES,ItemAffixes.EMPTY);require(!affixes.isEmpty(),"Missing UL affixes");
        for(var entry:affixes.entrySet()) {
            require(entry.getKey().getId().getNamespace().equals("tnkloot"),"Native affix escaped: "+entry.getKey());
            require(entry.getFloatValue()<=UnifiedApotheosis.maximum(stack)+0.00001f,"Affix boost exceeded UL tier band: "+entry);
        }
    }
    public static void verify(ServerPlayer player) throws Exception {
        var level=player.serverLevel();
        var profile=(CompoundTag)Class.forName("dev.mikaaah.tnkprogression.ProgressionAPI").getMethod("profile",ServerPlayer.class).invoke(null,player);
        profile.putInt("level",100);profile.putInt("ceiling",100);
        ApotheosisIntegration.force(player);WorldTier.setTier(player,WorldTier.HAVEN);
        require(WorldTier.getTier(player)==WorldTier.PINNACLE,"Pinnacle force did not persist");
        require(WorldTier.isUnlocked(player,WorldTier.PINNACLE),"Pinnacle still advancement-gated");
        var rarities=RarityRegistry.getSortedRarities();require(rarities.size()==6,"Expected exactly six visible rarities, got "+rarities);
        for(var rarity:rarities)require(RarityRegistry.INSTANCE.getKey(rarity).getNamespace().equals("tnkloot"),"Visible native rarity");
        for(String legacy:List.of("common","uncommon","rare","epic","mythic")) {
            var old=RarityRegistry.INSTANCE.holder(ResourceLocation.parse("apotheosis:"+legacy));
            require(old.get()==RarityRegistry.INSTANCE.getValue(ResourceLocation.parse("tnkloot:"+legacy)),"Legacy rarity alias failed: "+legacy);
        }
        var context=GenContext.forPlayer(RandomSource.create(123),player);
        require(((ContextOwner)(Object)context).tnkloot$player()==player,"Generation player attribution lost");
        for(int roll=0;roll<512;roll++)require(RarityRegistry.INSTANCE.getKey(LootRarity.random(context)).getNamespace().equals("tnkloot"),"Random rarity selected native group");
        int crafting=0,invaderChecks=0;
        for(var rarity:rarities) {
            for(Item item:List.of(Items.IRON_CHESTPLATE,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_SWORD)) {
                var input=new ItemStack(item);input.setDamageValue(7);input.set(DataComponents.CUSTOM_NAME,Component.literal("Crafted exact item"));
                var data=new CompoundTag();data.putString("recipe_marker","preserved");input.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                var output=LootController.createLootItem(input.copy(),rarity,context);bounded(output);
                require(output.is(item) && output.getDamageValue()==7,"Reforging replaced/repaired the crafted item");
                require(Objects.equals(output.get(DataComponents.CUSTOM_NAME),input.get(DataComponents.CUSTOM_NAME)),"Custom name lost");
                require(UnidentifiedWeaponItem.data(output).getString("recipe_marker").equals("preserved"),"Native custom data lost");
                var boosted=output.get(Apoth.Components.AFFIXES).toBuilder();for(var key:output.get(Apoth.Components.AFFIXES).keySet())boosted.put(key,2);
                AffixHelper.setAffixes(output,boosted.build());bounded(output);
                for(var instance:AffixHelper.getAffixes(output).values())require(!AugmentingMenu.canAugment(instance),"Upgrade consumes resources above UL cap");
                crafting++;
            }
        }
        for(var rarity:rarities) {
            if(RarityRegistry.INSTANCE.getKey(rarity).getPath().equals("common"))continue; // Native invaders begin at Uncommon.
            var definition=InvaderRegistry.INSTANCE.getValues().stream().filter(value->value.entity()==net.minecraft.world.entity.EntityType.ZOMBIE && value.stats().containsKey(rarity)).findFirst().orElseThrow(()->new AssertionError("No invader definition for "+rarity));
            var boss=definition.createBoss(level,new BlockPos(0,180,0),context,rarity);
            var prize=java.util.stream.StreamSupport.stream(boss.getAllSlots().spliterator(),false).filter(stack->stack.getOrDefault(Apoth.Components.FROM_BOSS,false)).findFirst().orElseThrow(()->new AssertionError("Native invader generated no marked reward"));
            bounded(prize);
            var drops=new ArrayList<ItemEntity>();drops.add(new ItemEntity(level,0,180,0,prize.copy()));
            FinalLoot.drops(new LivingDropsEvent(boss,player.damageSources().playerAttack(player),drops,true));
            require(drops.size()==1 && drops.getFirst().getItem().getItem() instanceof UnidentifiedWeaponItem,"Invader reward was not sealed once");
            var token=drops.getFirst().getItem();
            require(dev.mikaaah.tnkloot.dimension.DimensionLoot.options(token).size()==1,"Invader preview did not retain exact item");
            var base=UnidentifiedWeaponItem.baseResult(level,token);require(base.is(prize.getItem()),"Invader identity changed on identification");
            require(Objects.equals(base.get(DataComponents.ENCHANTMENTS),prize.get(DataComponents.ENCHANTMENTS)),"Invader enchantments lost");
            var revealed=ApotheosisCompat.apply(base,UnidentifiedWeaponItem.tier(token),player,UnidentifiedWeaponItem.data(token).getLong("roll_seed"));bounded(revealed);
            require(!InvaderRewards.raw(revealed),"Identified invader equipment will be sealed again");invaderChecks++;
        }
        BlockPos position=new BlockPos(0,185,0);var oldState=level.getBlockState(position);
        try {
            level.setBlock(position,Apoth.Blocks.REFORGING_TABLE.value().defaultBlockState(),3);
            var tile=(ReforgingTableTile)level.getBlockEntity(position);
            player.experienceLevel=1000;player.totalExperience=5_000_000;
            for(var rarity:rarities) {
                require(tile.getRecipeFor(rarity)!=null,"No reforging recipe for "+rarity);
                var menu=new ReforgingMenu(91,player.getInventory(),position);
                menu.getSlot(0).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
                menu.getSlot(1).set(new ItemStack(rarity.getMaterial(),64));
                menu.getSlot(2).set(new ItemStack(Apoth.Items.SIGIL_OF_REBIRTH,64));
                menu.slotsChanged(player.getInventory());
                for(int slot=3;slot<=5;slot++) { bounded(menu.getSlot(slot).getItem());require(menu.getSlot(slot).getItem().is(Items.DIAMOND_CHESTPLATE),"Table changed recipe item"); }
                require(menu.getSlot(3).mayPickup(player),"Legitimate reforging pickup refused");
                int mat=menu.getMatCount(),sigils=menu.getSigilCount();int matCost=menu.getMatCost(0),sigilCost=menu.getSigilCost(0);
                var output=menu.getSlot(3).getItem().copy();menu.getSlot(3).onTake(player,output);
                require(menu.getSlot(0).getItem().isEmpty() && menu.getMatCount()==mat-matCost && menu.getSigilCount()==sigils-sigilCost,"Reforging payment/input consumption failed");
            }
            profile.putInt("level",1);profile.putInt("ceiling",10);
            var menu=new ReforgingMenu(92,player.getInventory(),position);
            var mythic=RarityRegistry.INSTANCE.getValue(ResourceLocation.parse("tnkloot:mythic"));
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND_CHESTPLATE));menu.getSlot(1).set(new ItemStack(mythic.getMaterial(),64));menu.getSlot(2).set(new ItemStack(Apoth.Items.SIGIL_OF_REBIRTH,64));
            menu.slotsChanged(player.getInventory());require(menu.getRarity()==null && menu.getSlot(3).getItem().isEmpty(),"Reforging bypassed TNK rarity cap");
            var low=LootController.createLootItem(new ItemStack(Items.DIAMOND_SWORD),mythic,GenContext.forPlayer(player));bounded(low);
            require(UnifiedApotheosis.index(UnidentifiedWeaponItem.data(low).getString("tnk_rarity"))<=TnkLevelBridge.rarityCap(player),"Direct generation bypassed rarity cap");
        } finally { level.setBlock(position,oldState,3);profile.putInt("level",100);profile.putInt("ceiling",100); }
        TnkLoot.LOG.info("TNKLOOT_UNIFIED_APOTHEOSIS_OK: six canonical rarities/materials, 512 native-generator rolls, {} exact crafted-item checks, six paid reforging paths, {} actual invader prizes, boss/malice/augment bounds, milestone caps, automatic Pinnacle",crafting,invaderChecks);
    }
}
