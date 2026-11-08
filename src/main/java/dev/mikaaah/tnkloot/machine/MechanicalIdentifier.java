package dev.mikaaah.tnkloot.machine;

import dev.mikaaah.tnkloot.TnkLoot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/** Only registers the optional Create machine when Create exists. No Create classes are linked. */
public final class MechanicalIdentifier {
 public static final boolean ENABLED=ModList.get().isLoaded("create");
 private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,TnkLoot.MOD_ID);
 private static final DeferredRegister<net.minecraft.world.item.Item> ITEMS=DeferredRegister.create(Registries.ITEM,TnkLoot.MOD_ID);
 private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,TnkLoot.MOD_ID);
 private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,TnkLoot.MOD_ID);
 public static final Supplier<MechanicalIdentifierBlock> BLOCK=BLOCKS.register("mechanical_identifier",()->new MechanicalIdentifierBlock(BlockBehaviour.Properties.of().noOcclusion().strength(3.5f).requiresCorrectToolForDrops()));
 public static final Supplier<BlockItem> ITEM=ITEMS.register("mechanical_identifier",()->new BlockItem(BLOCK.get(),new net.minecraft.world.item.Item.Properties()));
 public static final Supplier<BlockEntityType<MechanicalIdentifierEntity>> ENTITY=ENTITIES.register("mechanical_identifier",()->BlockEntityType.Builder.of(MechanicalIdentifierEntity::new,BLOCK.get()).build(null));
 public static final Supplier<MenuType<MechanicalIdentifierMenu>> MENU=MENUS.register("mechanical_identifier",()->IMenuTypeExtension.create((id,inv,buf)->new MechanicalIdentifierMenu(id,inv,buf.readBlockPos())));
 private MechanicalIdentifier(){}
 public static void register(IEventBus bus){
  if(!ENABLED)return;
  BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);
  MachineConfig.reload();bus.addListener(MechanicalIdentifier::capabilities);
  net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MachineSmoke::run);
 }
 private static void capabilities(RegisterCapabilitiesEvent event){
  event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ENTITY.get(),MechanicalIdentifierEntity::items);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,ENTITY.get(),MechanicalIdentifierEntity::fluids);
 }
}
