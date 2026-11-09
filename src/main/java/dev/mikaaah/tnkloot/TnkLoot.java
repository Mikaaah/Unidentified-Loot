package dev.mikaaah.tnkloot;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import dev.mikaaah.tnkloot.boss.BossItems;
import dev.mikaaah.tnkloot.command.ProgressionRewardCommands;
import dev.mikaaah.tnkloot.command.TnkCommands;
import dev.mikaaah.tnkloot.compat.CompatPresets;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.BetaIntegration;
import dev.mikaaah.tnkloot.loot.IdentifierBlock;
import dev.mikaaah.tnkloot.loot.IdentifierEntity;
import dev.mikaaah.tnkloot.loot.IdentifierInteractions;
import dev.mikaaah.tnkloot.loot.TnkBetaConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.loot.WeaponLootModifier;
import dev.mikaaah.tnkloot.machine.MechanicalIdentifier;
import dev.mikaaah.tnkloot.menu.TnkMenuNetwork;
import dev.mikaaah.tnkloot.menu.TnkMenus;
import dev.mikaaah.tnkloot.progression.ProgressionEvents;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import org.slf4j.Logger;

@Mod("tnkloot")
public final class TnkLoot {
   public static final String MOD_ID = "tnkloot";
   public static final Logger LOG = LogUtils.getLogger();
   public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "tnkloot");
   public static final Supplier<EntityType<IdentifierEntity>> IDENTIFIER_ENTITY = ENTITIES.register(
      "identifier", () -> Builder.of(IdentifierEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).build("tnkloot:identifier")
   );
   public static final Items ITEMS = DeferredRegister.createItems("tnkloot");
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("tnkloot");
   public static final Supplier<IdentifierBlock> IDENTIFIER_BLOCK = BLOCKS.register(
      "identifier", () -> new IdentifierBlock(Properties.of().strength(3.0F).requiresCorrectToolForDrops())
   );
   public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(
      Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "tnkloot"
   );
   public static final Supplier<MapCodec<WeaponLootModifier>> WEAPON_LOOT_MODIFIER = LOOT_MODIFIER_SERIALIZERS.register(
      "weapon_identification", () -> WeaponLootModifier.CODEC
   );
   public static final Map<TnkConfig.Category, DeferredItem<UnidentifiedWeaponItem>> TOKENS = new EnumMap<>(TnkConfig.Category.class);
   public static final Map<TnkConfig.Category, DeferredItem<UnidentifiedWeaponItem>> BOSS_TOKENS = new EnumMap<>(TnkConfig.Category.class);
   public static final DeferredItem<UnidentifiedWeaponItem> UNIDENTIFIED_WEAPON;
   public static final DeferredItem<BlockItem> IDENTIFIER_ITEM;

   public static DeferredItem<UnidentifiedWeaponItem> bossToken(TnkConfig.Category category) {
      return BOSS_TOKENS.get(category);
   }

   public static DeferredItem<UnidentifiedWeaponItem> token(TnkConfig.Category category) {
      return TOKENS.get(category);
   }

   public TnkLoot(IEventBus modBus) {
      MechanicalIdentifier.register(modBus);
      ITEMS.register(modBus);
      ENTITIES.register(modBus);
      modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) -> event.put(IDENTIFIER_ENTITY.get(), Villager.createAttributes().build()));
      BLOCKS.register(modBus);
      LOOT_MODIFIER_SERIALIZERS.register(modBus);
      TnkMenus.MENUS.register(modBus);
      modBus.addListener(TnkMenuNetwork::register);
      TnkCreativeTabs.TABS.register(modBus);
      TnkConfig.reload();
      TnkBetaConfig.load();
      dev.mikaaah.tnkloot.apotheosis.ApotheosisIntegration.register();
      NeoForge.EVENT_BUS.addListener(BetaIntegration::run);
      NeoForge.EVENT_BUS.addListener(CompatPresets::started);
      NeoForge.EVENT_BUS.addListener(IdentifierSmokeTest::run);
      // TNK Progression owns level and milestone unlocks in this edition.
      NeoForge.EVENT_BUS.addListener(TnkCommands::register);
      NeoForge.EVENT_BUS.addListener(ProgressionRewardCommands::register);
      NeoForge.EVENT_BUS.addListener(dev.mikaaah.tnkloot.compat.EquipmentAudit::commands);
      NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, dev.mikaaah.tnkloot.compat.ReleaseConfigMigration::started);
      NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onEntityInteract);
      NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onAttack);
      NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onDamage);
      NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onDeath);
      NeoForge.EVENT_BUS.addListener(IdentifierInteractions::onTick);
      RaritySupport.register();
   }

   static {
      for (TnkConfig.Category category : TnkConfig.Category.values()) {
         TOKENS.put(
            category,
            ITEMS.registerItem(
               category.token(), props -> new UnidentifiedWeaponItem(props, category), new net.minecraft.world.item.Item.Properties().stacksTo(1)
            )
         );
      }

      for (TnkConfig.Category category : TnkConfig.Category.values()) {
         BOSS_TOKENS.put(
            category,
            ITEMS.registerItem(
               "boss_" + category.token(),
               props -> new UnidentifiedWeaponItem(props, category, true),
               new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
            )
         );
      }

      UNIDENTIFIED_WEAPON = TOKENS.get(TnkConfig.Category.WEAPON);
      IDENTIFIER_ITEM = ITEMS.registerItem("identifier", props -> new BlockItem(IDENTIFIER_BLOCK.get(), props), new net.minecraft.world.item.Item.Properties());
      BossItems.initialize();
   }
}
