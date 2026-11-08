package dev.mikaaah.tnkloot.workshop;

import dev.mikaaah.tnkloot.features.*;
import dev.mikaaah.tnkloot.loot.IdentifierBlock;
import dev.mikaaah.tnkloot.menu.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Real input slots; the server owns the quote, confirmation and item return. */
public final class SalvageMenu extends AbstractContainerMenu {
 public static final int REVIEW=0,IDENTIFY=90,GUIDE=91;
 private final Player owner;
 private final Vec3 opening;
 private BlockPos station;
 private final SimpleContainer input=new SimpleContainer(7){@Override public void setChanged(){super.setChanged();if(!committing)invalidate();}};
 private final ContainerData data=new SimpleContainerData(12);
 private final ItemStack[] reviewed=new ItemStack[7];
 private boolean armed,committing,returned;private int reviewedValue,reviewedXp;
 public SalvageMenu(int id,Inventory inv){
  super(TnkMenus.SALVAGE.get(),id);owner=inv.player;opening=owner.position();
  for(int i=0;i<7;i++)addSlot(new Slot(input,i,SalvageLayout.INPUT_X+i*SalvageLayout.INPUT_PITCH,SalvageLayout.INPUT_Y){
   @Override public boolean mayPlace(ItemStack s){return FeatureConfig.get().salvage()&&PlayerLedger.salvageValue(s)>0;}
   @Override public int getMaxStackSize(){return 1;}
  });
  for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,c+r*9+9,SalvageLayout.PLAYER_X+c*18,SalvageLayout.PLAYER_Y+r*18));
  for(int c=0;c<9;c++)addSlot(new Slot(inv,c,SalvageLayout.PLAYER_X+c*18,SalvageLayout.HOTBAR_Y));
  addDataSlots(data);quote();
 }
 public static void open(ServerPlayer p,BlockPos pos){p.openMenu(new dev.mikaaah.tnkloot.menu.StationMenuProvider((id,inv,who)->new SalvageMenu(id,inv).atStation(pos),Component.translatable("menu.tnkloot.salvage")));}
 public SalvageMenu atStation(BlockPos pos){station=pos==null?null:pos.immutable();return this;}
 private void invalidate(){armed=false;if(data!=null)quote();}
 public int payout(){return (data.get(0)&65535)|(data.get(1)<<16);}
 public int balance(){return (data.get(2)&65535)|(data.get(3)<<16);}
 public int xpCost(){return (data.get(7)&65535)|(data.get(8)<<16);}
 public int xpBalance(){return (data.get(9)&65535)|(data.get(10)<<16);}
 public int count(){return data.get(4);}
 public boolean ready(){return data.get(5)==1;}
 public boolean reviewed(){return data.get(6)==1;}
 private void number(int slot,int value){data.set(slot,value&65535);data.set(slot+1,value>>>16);}
 private int value(){int n=0;for(int i=0;i<7;i++)n+=PlayerLedger.salvageValue(input.getItem(i));return n;}
 private void quote(){
  if(!(owner instanceof ServerPlayer))return;
  int total=value(),count=0;boolean valid=FeatureConfig.get().salvage();
  for(int i=0;i<7;i++)if(!input.getItem(i).isEmpty()){count++;valid&=input.getItem(i).getCount()==1&&PlayerLedger.salvageValue(input.getItem(i))>0;}
  int xp=0;for(int i=0;i<7;i++)if(!input.getItem(i).isEmpty())xp+=ExperienceCosts.salvage(input.getItem(i));number(7,xp);number(9,ExperienceCosts.points(owner));
  int credit=PlayerLedger.credit(owner);number(0,total);number(2,credit);data.set(4,count);data.set(5,valid&&ExperienceCosts.points(owner)>=xp&&count>0&&total>0&&(long)credit+total<=FeatureConfig.get().maxCredit()?1:0);data.set(6,armed?1:0);
 }
 private boolean unchanged(){int xp=0;for(int i=0;i<7;i++)xp+=ExperienceCosts.salvage(input.getItem(i));if(value()!=reviewedValue||xp!=reviewedXp)return false;for(int i=0;i<7;i++)if(!ItemStack.matches(reviewed[i],input.getItem(i)))return false;return true;}
 @Override public boolean clickMenuButton(Player p,int id){
  if(!(p instanceof ServerPlayer server)||!stillValid(p))return false;
  if(id==IDENTIFY){TnkMenuProvider.openIdentifier(server,station);return true;}
  if(id==92){CatalogMenu.open(server,false,station);return true;}
  if(id==GUIDE){server.closeContainer();LootJournal.open(server);return true;}
  if(id!=REVIEW)return false;quote();if(!ready()){armed=false;quote();return false;}
  if(!armed){for(int i=0;i<7;i++)reviewed[i]=input.getItem(i).copy();reviewedValue=value();reviewedXp=xpCost();armed=true;quote();return true;}
  if(!unchanged()){invalidate();return false;}
  // Validate the entire batch and capacity before crediting or consuming anything.
  int amount=value();if(!ExperienceCosts.spendPoints(server,xpCost())){invalidate();return false;}PlayerLedger.addCredit(server,amount);committing=true;
  try{for(int i=0;i<7;i++)input.setItem(i,ItemStack.EMPTY);}finally{committing=false;armed=false;quote();}
  server.playNotifySound(SoundEvents.ANVIL_USE,SoundSource.BLOCKS,.45f,1.05f);
  server.sendSystemMessage(Component.translatable("message.tnkloot.salvaged",amount,PlayerLedger.credit(server)));return true;
 }
 @Override public void broadcastChanges(){if(owner instanceof ServerPlayer){if(armed&&!unchanged())armed=false;quote();}super.broadcastChanges();}
 @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&p.distanceToSqr(opening)<=64&&dev.mikaaah.tnkloot.config.TnkConfig.get().stationIdentification()&&(station==null||p.level().getBlockState(station).getBlock() instanceof IdentifierBlock);}
 @Override public ItemStack quickMoveStack(Player p,int index){
  if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;
  var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var original=s.copy();
  if(index<7){if(!moveItemStackTo(s,7,slots.size(),true))return ItemStack.EMPTY;}
  else if(!FeatureConfig.get().salvage()||PlayerLedger.salvageValue(s)<=0||!moveItemStackTo(s,0,7,false))return ItemStack.EMPTY;
  if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();invalidate();return original;
 }
 @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p))return;invalidate();super.clicked(slot,button,type,p);}
 @Override public void removed(Player p){super.removed(p);if(!p.level().isClientSide&&!returned){returned=true;clearContainer(p,input);}}
}
