package dev.mikaaah.tnkloot.rarity;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import java.util.*;
import java.lang.reflect.Method;

/** Small adapter: all Minecraft calls use the 1.21.1 signatures from the preserved build. */
public final class RaritySupport {
    private static final String ID="tnk_rarity", NAME="tnk_rarity_name", COLOR="tnk_rarity_color", APO="tnk_apotheosis_rarity";
    private RaritySupport() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(RaritySupport::identifiedTooltip);
        NeoForge.EVENT_BUS.addListener(RarityCommands::register);
        NeoForge.EVENT_BUS.addListener(dev.mikaaah.tnkloot.boss.BossCommands::register);
        NeoForge.EVENT_BUS.addListener(RarityIntegration::run);
        dev.mikaaah.tnkloot.features.FeatureHooks.register();
    }
    public static boolean reload() { return RarityPolicy.reload(TnkConfig.path().resolveSibling("rarities.json")); }
    public static boolean reloadAll() { return dev.mikaaah.tnkloot.apotheosis.ApotheosisIntegration.reload() && dev.mikaaah.tnkloot.workshop.ExperienceCosts.reload() && reload() && TnkConfig.reload() && dev.mikaaah.tnkloot.features.FeatureConfig.reload() && dev.mikaaah.tnkloot.dimension.DimensionSupport.reload(); }
    private static boolean token(ItemStack stack) { return stack.getItem() instanceof UnidentifiedWeaponItem; }
    private static void save(ItemStack stack, CompoundTag tag) { stack.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag)); }
    private static void snapshot(CompoundTag tag, RarityPolicy.Quality q) {
        tag.putString(ID,q.id()); tag.putString(NAME,q.name());tag.putInt(COLOR,q.color());tag.putString(APO,q.apotheosis());
    }
    private static RarityPolicy.Quality quality(CompoundTag tag) { return RarityPolicy.get().quality(tag.getString(ID)); }
    private static void vanillaRarity(ItemStack stack,RarityPolicy.Quality q) {
        Rarity rarity=switch(q.id()) { case "uncommon"->Rarity.UNCOMMON;case "rare"->Rarity.RARE;case "epic","legendary","mythic"->Rarity.EPIC;default->Rarity.COMMON;};
        stack.set(DataComponents.RARITY,rarity);
    }
    private static Component text(String text,int color) { return Component.literal(text).withStyle(Style.EMPTY.withColor(color).withItalic(false)); }
    private static void nameToken(ItemStack stack,CompoundTag tag) {
        if (!(stack.getItem() instanceof UnidentifiedWeaponItem item)) return;
        String category=item.category().name().toLowerCase(Locale.ROOT).replace('_',' ');
        String name=tag.getString(NAME)+" Unidentified "+Character.toUpperCase(category.charAt(0))+category.substring(1);
        if (tag.getBoolean("boss_universal")) name=tag.getString(NAME)+" Unidentified "+tag.getString("boss_name")+" Drop";
        else if (!tag.getString("boss_name").isBlank()) name+=" · "+tag.getString("boss_name");
        stack.set(DataComponents.CUSTOM_NAME,text(name,tag.getInt(COLOR)));
    }
    public static ItemStack ensure(ItemStack stack) {
        if (!token(stack)) return stack;
        CompoundTag tag=UnidentifiedWeaponItem.data(stack);
        if (tag.getBoolean("creative_template")) return stack;
        // The earlier TNK beta saved namespaced IDs instead of the policy's short IDs.
        if (tag.getString(ID).equals("ancient")) { snapshot(tag,RarityPolicy.get().quality("mythic"));save(stack,tag); }
        if (tag.getString(ID).startsWith("tnkloot:")) {
            snapshot(tag,RarityPolicy.get().quality(tag.getString(ID).substring(8)));save(stack,tag);
        }
        if (!tag.contains(ID)) {
            snapshot(tag,RarityPolicy.get().roll(UnidentifiedWeaponItem.tier(stack),tag.getString("source_type").equals("boss_reward"),tag.getLong("roll_seed")));
            save(stack,tag);
        } else if (!tag.contains(NAME) || !tag.contains(COLOR) || !tag.contains(APO)) {
            snapshot(tag,quality(tag));save(stack,tag);
        }
        tag.putInt(COLOR,quality(tag).color()); save(stack,tag);
        vanillaRarity(stack,quality(tag));nameToken(stack,tag); model(stack,tag.getString(ID));return stack;
    }
    public static ItemStack template(ItemStack stack) {
        if (!token(stack)) return stack;
        CompoundTag tag=UnidentifiedWeaponItem.data(stack);
        if (!tag.getBoolean("creative_template")) return stack;
        clear(tag);save(stack,tag);stack.remove(DataComponents.CUSTOM_MODEL_DATA);stack.remove(DataComponents.CUSTOM_NAME);stack.set(DataComponents.RARITY,Rarity.COMMON);return stack;
    }
    private static void model(ItemStack stack,String id) {
        int index=java.util.List.of("common","uncommon","rare","epic","legendary","mythic").indexOf(id);
        if(index>=0)stack.set(DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(index+1));
    }
    private static void clear(CompoundTag tag) { for(String key:new String[]{ID,NAME,COLOR,APO}) tag.remove(key); }
    public static void beforeBind(ItemStack stack) {
        if (!token(stack)) return;
        CompoundTag tag=UnidentifiedWeaponItem.data(stack);
        if (tag.getBoolean("creative_template") || !tag.contains("roll_seed",4)) { clear(tag);save(stack,tag); }
    }
    public static boolean bound(ItemStack stack,boolean changed) {
        if (!token(stack)) return changed;
        boolean missing=!UnidentifiedWeaponItem.data(stack).contains(ID);
        ensure(stack);return changed||missing;
    }
    public static void stamp(ItemStack stack,RarityPolicy.Quality q,int tier) {
        CompoundTag tag=UnidentifiedWeaponItem.data(stack); snapshot(tag,q);tag.putInt("tnk_tier",tier);save(stack,tag);vanillaRarity(stack,q);
    }
    public static ItemStack base(ItemStack result,ItemStack token) {
        if (result.isEmpty()) return result;
        ensure(token);
        CompoundTag from=UnidentifiedWeaponItem.data(token),tag=UnidentifiedWeaponItem.data(result);
        for(String key:new String[]{ID,NAME,APO}) tag.putString(key,from.getString(key));
        tag.putInt(COLOR,from.getInt(COLOR));tag.putInt("tnk_tier",UnidentifiedWeaponItem.tier(token));
        tag.putString("tnk_tier_name",from.getString("tier_name"));
        if(from.contains("tnk_source_level"))tag.putInt("tnk_source_level",from.getInt("tnk_source_level"));
        for(String key:new String[]{"boss_pool","boss_name","source_type","origin_dimension"})if(from.contains(key))tag.putString(key,from.getString(key));save(result,tag);return result;
    }
    public static ResourceLocation resolve(ItemStack stack,int tier,long seed) {
        CompoundTag tag=UnidentifiedWeaponItem.data(stack);
        if (!tag.contains(ID)) { snapshot(tag,RarityPolicy.get().roll(tier,false,seed));tag.putInt("tnk_tier",tier);save(stack,tag); }
        return ResourceLocation.parse(tag.getString(APO));
    }
    public static ItemStack finish(ItemStack stack,int tier,long seed) {
        CompoundTag tag=UnidentifiedWeaponItem.data(stack);
        RarityPolicy.Quality q=quality(tag);
        vanillaRarity(stack,q);
        try {
            @SuppressWarnings("unchecked") DataComponentType<Object> component=(DataComponentType<Object>)Class.forName("dev.shadowsoffire.apotheosis.Apoth$Components").getField("AFFIXES").get(null);
            Object affixes=stack.get(component);
            if (affixes!=null) {
                Object builder=affixes.getClass().getMethod("toBuilder").invoke(affixes);
                Set<?> keys=(Set<?>)affixes.getClass().getMethod("keySet").invoke(affixes);
                Method put=Arrays.stream(builder.getClass().getMethods()).filter(m->m.getName().equals("put")&&m.getParameterCount()==2).findFirst().orElseThrow();
                Random random=new Random(seed^0x41464649584CL);
                for(Object key:keys) put.invoke(builder,key,RarityPolicy.get().level(tier,q,random));
                Object changed=builder.getClass().getMethod("build").invoke(builder);
                Class.forName("dev.shadowsoffire.apotheosis.affix.AffixHelper").getMethod("setAffixes",ItemStack.class,affixes.getClass()).invoke(null,stack,changed);
            }
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException("Could not apply tier-bounded affix levels",ex); }
        return dev.mikaaah.tnkloot.features.BossSignatures.apply(stack,tier,seed);
    }
    public static int cost(int base,ItemStack token,ItemStack result) {
        if (dev.shadowsoffire.apotheosis.loot.LootCategory.forItem(result).isNone() && !NativeQuality.jewelry(result)) return base;
        ensure(token);
        return RarityPolicy.cost(base,quality(UnidentifiedWeaponItem.data(token)).cost());
    }
    public static void tooltip(ItemStack stack,List<Component> lines) {
        CompoundTag tag=UnidentifiedWeaponItem.data(stack);
        if(tag.contains("tnk_source_level"))lines.add(text("Source level "+tag.getInt("tnk_source_level")+" · sealed at generation",0xAAA18B));
        if (tag.contains(ID)) lines.add(text(tag.getString(NAME).toUpperCase(Locale.ROOT)+" · SEALED",tag.getInt(COLOR)));
        else lines.add(text("RANDOM RARITY · SEALED",0xAAAAAA));
        if(!tag.getString("origin_dimension").isBlank())lines.add(text("Origin · "+tag.getString("origin_dimension"),0x88BBBB));
        String tier=tag.getString("tier_name");
        lines.add(text("Tier "+UnidentifiedWeaponItem.tier(stack)+(tier.isBlank()?"":" · "+tier),0xCCCCCC));
        if (!tag.getString("boss_name").isBlank()) lines.add(text("Boss reward · "+tag.getString("boss_name"),0xDD77FF));
        lines.add(text("This item's power has been sealed.",0x999999));
        lines.add(text("An Item Identifier can reveal its potential.",0x999999));
        lines.add(text("Identification cost is shown before confirmation.",0x777777));
    }
    public static void identifiedTooltip(ItemTooltipEvent event) {
        ItemStack stack=event.getItemStack();
        if (token(stack)) return;
        CompoundTag tag=UnidentifiedWeaponItem.data(stack);
        if (!tag.contains(ID)) return;
        if(!tag.getString("tnk_signature").isBlank())event.getToolTip().add(text(dev.mikaaah.tnkloot.features.BossSignatures.describe(tag.getString("tnk_signature"))+" · shared cooldown",0xDD99FF));
        if(!tag.getString("boss_name").isBlank())event.getToolTip().add(text("Boss reward: "+tag.getString("boss_name"),0xDD99FF));
        event.getToolTip().add(Math.min(1,event.getToolTip().size()),text(tag.getString(NAME)+" · Tier "+tag.getInt("tnk_tier"),tag.getInt(COLOR)));
    }
    public static String clientHint(String text) {
        if (text.startsWith("Affixes: ")) return "Rarity rolls";
        return text.replaceAll("\\nAffixes: [^\\n]*", "\nRarity follows TNK Progression milestone caps.");
    }
    public static void explain(net.minecraft.server.level.ServerPlayer player,int tier) {
        var settings=RarityPolicy.get();int[] weights=dev.mikaaah.tnkloot.loot.TnkBetaConfig.current.rarityWeights;
        int cap=dev.mikaaah.tnkloot.loot.TnkLevelBridge.rarityCap(player);long total=0;
        for(int i=0;i<=cap;i++)total+=weights[i];
        player.sendSystemMessage(text("TNK loot rarity chances · your milestone cap: "+settings.quality(dev.mikaaah.tnkloot.loot.TnkLootRolls.RARITIES[cap]).name(),0xCCCCCC));
        for(int i=0;i<6;i++) {var q=settings.quality(dev.mikaaah.tnkloot.loot.TnkLootRolls.RARITIES[i]);double chance=i<=cap&&total>0?100.0*weights[i]/total:0;
            player.sendSystemMessage(text(q.name()+": "+String.format(Locale.ROOT,"%.2f%%",chance)+" · cost ×"+q.cost(),q.color()));}
        player.sendSystemMessage(text("Normal weights: tnk-beta.json · boss weights, costs and attribute bands: rarities.json",0xAAAAAA));
    }
    public static void force(ItemStack stack,String id) {
        if (!token(stack)) throw new IllegalArgumentException("Hold an unidentified item");
        UnidentifiedWeaponItem.bindRollSeed(stack);
        CompoundTag tag=UnidentifiedWeaponItem.data(stack);snapshot(tag,RarityPolicy.get().quality(id));save(stack,tag);ensure(stack);
    }
}
