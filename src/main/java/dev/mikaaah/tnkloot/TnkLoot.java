package dev.mikaaah.tnkloot;

import dev.mikaaah.tnkloot.command.TnkCommands;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.config.TnkConfig.Category;
import dev.mikaaah.tnkloot.loot.WeaponLootModifier;
import dev.mikaaah.tnkloot.loot.IdentifierBlock;
import dev.mikaaah.tnkloot.progression.ProgressionEvents;
import dev.mikaaah.tnkloot.menu.TnkMenus;
import dev.mikaaah.tnkloot.loot.IdentifierInteractions;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import com.mojang.serialization.MapCodec;

import java.util.function.Supplier;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(TnkLoot.MOD_ID)
public final class TnkLoot {
    public static final String MOD_ID = "tnkloot";
    public static final Logger LOG = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final Supplier<IdentifierBlock> IDENTIFIER_BLOCK = BLOCKS.register("identifier", () ->
        new IdentifierBlock(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    public static final Supplier<MapCodec<WeaponLootModifier>> WEAPON_LOOT_MODIFIER =
        LOOT_MODIFIER_SERIALIZERS.register("weapon_identification", () -> WeaponLootModifier.CODEC);
    public static final java.util.Map<Category, DeferredItem<UnidentifiedWeaponItem>> TOKENS = new java.util.EnumMap<>(Category.class);
    static {
        for (Category category : Category.values()) TOKENS.put(category,
            ITEMS.registerItem(category.token(), props -> new UnidentifiedWeaponItem(props, category), new Item.Properties().stacksTo(1)));
    }
    public static DeferredItem<UnidentifiedWeaponItem> token(Category category) { return TOKENS.get(category); }
    public static final DeferredItem<UnidentifiedWeaponItem> UNIDENTIFIED_WEAPON = TOKENS.get(Category.WEAPON);
    public static final DeferredItem<BlockItem> IDENTIFIER_ITEM = ITEMS.registerItem("identifier",
        props -> new BlockItem(IDENTIFIER_BLOCK.get(), props), new Item.Properties());

    public TnkLoot(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        LOOT_MODIFIER_SERIALIZERS.register(modBus);
        TnkMenus.MENUS.register(modBus);
        TnkCreativeTabs.TABS.register(modBus);
        TnkConfig.reload();
        dev.mikaaah.tnkloot.loot.TnkBetaConfig.load();
        NeoForge.EVENT_BUS.register(dev.mikaaah.tnkloot.loot.TnkMobDrops.class);
        NeoForge.EVENT_BUS.addListener(IdentifierSmokeTest::run);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onAdvancement);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(TnkCommands::register);
        NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onAttack);
        NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onDamage);
        NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onDeath);
        NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onTick);
    }
}
