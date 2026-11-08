package dev.mikaaah.tnkloot.features;
import dev.mikaaah.tnkloot.TnkLoot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
public final class FeatureItems {
 public static final DeferredItem<Item> JOURNAL=TnkLoot.ITEMS.registerItem("loot_journal",p->new Item(p){
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){if(player instanceof ServerPlayer server)dev.mikaaah.tnkloot.workshop.LootJournal.open(server);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
 },new Item.Properties().stacksTo(1));
 public static void initialize(){}
}
