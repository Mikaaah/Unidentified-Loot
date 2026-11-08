package dev.mikaaah.tnkloot.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class MechanicalIdentifierMenu extends AbstractContainerMenu {
 private final MechanicalIdentifierEntity machine;
 private final BlockPos pos;
 private final ContainerData data;
 public MechanicalIdentifierMenu(int id,Inventory inventory,BlockPos pos){this(id,inventory,inventory.player.level().getBlockEntity(pos) instanceof MechanicalIdentifierEntity m?m:null,pos);}
 public MechanicalIdentifierMenu(int id,Inventory inventory,MechanicalIdentifierEntity machine){this(id,inventory,machine,machine.getBlockPos());}
 private MechanicalIdentifierMenu(int id,Inventory inv,MechanicalIdentifierEntity machine,BlockPos pos){
  super(MechanicalIdentifier.MENU.get(),id);this.machine=machine;this.pos=pos.immutable();
  var slots=machine==null?new ItemStackHandler(14){@Override public boolean isItemValid(int slot,ItemStack stack){return slot<7&&stack.getItem() instanceof dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;}@Override public int getSlotLimit(int slot){return slot<7?1:64;}}:machine.inventory();
  data=inv.player.level().isClientSide?new SimpleContainerData(14):machine.data();addDataSlots(data);
  for(int i=0;i<7;i++)addSlot(new SlotItemHandler(slots,i,27+i*24,48));
  for(int i=0;i<7;i++)addSlot(new SlotItemHandler(slots,7+i,27+i*24,91){@Override public boolean mayPlace(ItemStack s){return false;}});
  for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,9+r*9+c,27+c*18,166+r*18));
  for(int c=0;c<9;c++)addSlot(new Slot(inv,c,27+c*18,224));
 }
 public int value(int field){return (data.get(field*2)&65535)|(data.get(field*2+1)<<16);}
 public net.neoforged.neoforge.fluids.FluidStack fluid(){
  var fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(value(6));
  return fluid==null||fluid==net.minecraft.world.level.material.Fluids.EMPTY||value(0)<=0?net.neoforged.neoforge.fluids.FluidStack.EMPTY:new net.neoforged.neoforge.fluids.FluidStack(fluid,value(0));
 }
 @Override public boolean stillValid(Player p){return p.isAlive()&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64&&p.level().getBlockState(pos).is(MechanicalIdentifier.BLOCK.get())&&(p.level().isClientSide||p.level().getBlockEntity(pos)==machine);}
 @Override public ItemStack quickMoveStack(Player p,int index){
  if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
  if(index<14){if(!moveItemStackTo(stack,14,slots.size(),true))return ItemStack.EMPTY;}
  else if(!moveItemStackTo(stack,0,7,false))return ItemStack.EMPTY;
  if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;
 }
}
