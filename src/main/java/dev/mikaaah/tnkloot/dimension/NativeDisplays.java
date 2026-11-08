package dev.mikaaah.tnkloot.dimension;
import dev.mikaaah.tnkloot.rarity.NativeQuality;
import dev.mikaaah.tnkloot.menu.TnkMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
public final class NativeDisplays {
 private static final java.lang.reflect.Field OWNER=ownerField();private static java.lang.reflect.Field ownerField(){try{var f=TnkMenu.class.getDeclaredField("owner");f.setAccessible(true);return f;}catch(ReflectiveOperationException ex){throw new ExceptionInInitializerError(ex);}}
 public static ItemStack prepare(ItemStack stack,ServerPlayer player,int tier,long seed){return NativeQuality.jewelry(stack)?NativeQuality.prepare(stack,tier,player,seed):stack;}
 public static void decorate(AbstractContainerMenu menu,ServerPlayer player,int tier,int count){
  for(int i=0;i<Math.min(count,menu.slots.size());i++){var slot=menu.getSlot(i);var stack=slot.getItem();if(NativeQuality.jewelry(stack))prepare(stack,player,tier,0x544E4B00L+i);}
 }
 public static void identifier(TnkMenu menu){try{if(OWNER.get(menu) instanceof ServerPlayer p)decorate(menu,p,menu.admin()?menu.selectedTier().rank():0,menu.admin()?37:7);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
}
