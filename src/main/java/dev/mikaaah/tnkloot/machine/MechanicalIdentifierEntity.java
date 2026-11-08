package dev.mikaaah.tnkloot.machine;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.ApotheosisCompat;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.workshop.ExperienceCosts;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class MechanicalIdentifierEntity extends BlockEntity implements MenuProvider {
   private final ItemStackHandler inventory = new ItemStackHandler(14) {
      public boolean isItemValid(int slot, ItemStack stack) {
         return slot < 7 && stack.getItem() instanceof UnidentifiedWeaponItem;
      }

      public int getSlotLimit(int slot) {
         return slot < 7 ? 1 : 64;
      }

      protected void onContentsChanged(int slot) {
         MechanicalIdentifierEntity.this.setChanged();
         MechanicalIdentifierEntity.this.idleDelay = 0;
      }
   };
   private final IItemHandler input = new MechanicalIdentifierEntity.ItemHandlerView(false);
   private final IItemHandler output = new MechanicalIdentifierEntity.ItemHandlerView(true);
   private final FluidTank tank = new FluidTank(MachineConfig.get().tankCapacity(), f -> MachineConfig.unit(f) > 0.0) {
      protected void onContentsChanged() {
         MechanicalIdentifierEntity.this.setChanged();
         MechanicalIdentifierEntity.this.idleDelay = 0;
      }
   };
   private final IFluidHandler fluidInput = new IFluidHandler() {
      public int getTanks() {
         return 1;
      }

      public FluidStack getFluidInTank(int i) {
         return MechanicalIdentifierEntity.this.tank.getFluidInTank(i).copy();
      }

      public int getTankCapacity(int i) {
         return MechanicalIdentifierEntity.this.tank.getCapacity();
      }

      public boolean isFluidValid(int i, FluidStack f) {
         return MechanicalIdentifierEntity.this.tank.isFluidValid(f);
      }

      public int fill(FluidStack f, FluidAction action) {
         return MechanicalIdentifierEntity.this.tank.fill(f, action);
      }

      public FluidStack drain(FluidStack f, FluidAction action) {
         return FluidStack.EMPTY;
      }

      public FluidStack drain(int max, FluidAction action) {
         return FluidStack.EMPTY;
      }
   };
   private int activeSlot = -1;
   private int progress;
   private int xpCost;
   private int idleDelay;
   private int status;
   private ItemStack activeToken = ItemStack.EMPTY;
   private ItemStack prepared = ItemStack.EMPTY;
   private ItemStack lastFailure = ItemStack.EMPTY;
   private final ContainerData data = new ContainerData() {
      public int getCount() {
         return 14;
      }

      public int get(int index) {
         int field = index / 2;

         int value = switch (field) {
            case 0 -> MechanicalIdentifierEntity.this.tank.getFluidAmount();
            case 1 -> MechanicalIdentifierEntity.this.tank.getCapacity();
            case 2 -> MechanicalIdentifierEntity.this.progress;
            case 3 -> MachineConfig.get().processingTicks();
            case 4 -> MechanicalIdentifierEntity.this.status;
            case 5 -> MechanicalIdentifierEntity.this.requiredFluid();
            case 6 -> BuiltInRegistries.FLUID.getId(MechanicalIdentifierEntity.this.tank.getFluid().getFluid());
            default -> 0;
         };
         return index % 2 == 0 ? value & 65535 : value >>> 16;
      }

      public void set(int index, int value) {
      }
   };

   public MechanicalIdentifierEntity(BlockPos pos, BlockState state) {
      super(MechanicalIdentifier.ENTITY.get(), pos, state);
   }

   public ItemStackHandler inventory() {
      return this.inventory;
   }

   public IFluidHandler fluidInput() {
      return this.fluidInput;
   }

   public ContainerData data() {
      return this.data;
   }

   public IItemHandler items(Direction side) {
      return (IItemHandler)(side == null
         ? this.inventory
         : (
            side == Direction.DOWN
               ? this.output
               : (side == ((Direction)this.getBlockState().getValue(MechanicalIdentifierBlock.FACING)).getClockWise() ? null : this.input)
         ));
   }

   public IFluidHandler fluids(Direction side) {
      return side != null && side != ((Direction)this.getBlockState().getValue(MechanicalIdentifierBlock.FACING)).getClockWise() ? null : this.fluidInput;
   }

   public int requiredFluid() {
      return MachineConfig.fluidCost(this.xpCost, MachineConfig.unit(this.tank.getFluid()));
   }

   private int outputSlot(ItemStack result) {
      for (int i = 7; i < 14; i++) {
         ItemStack stack = this.inventory.getStackInSlot(i);
         if (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, result) && stack.getCount() + result.getCount() <= stack.getMaxStackSize()) {
            return i;
         }
      }

      return -1;
   }

   private boolean hasOutputRoom() {
      for (int i = 7; i < 14; i++) {
         if (this.inventory.getStackInSlot(i).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private void clearJob() {
      this.activeSlot = -1;
      this.activeToken = this.prepared = ItemStack.EMPTY;
      this.progress = this.xpCost = 0;
      this.setChanged();
   }

   public static void tick(Level level, BlockPos pos, BlockState state, MechanicalIdentifierEntity machine) {
      if (level instanceof ServerLevel server) {
         machine.work(server);
      }
   }

   private void work(ServerLevel world) {
      if (TnkConfig.get().enableIdentification() && TnkConfig.get().stationIdentification()) {
         if (this.activeSlot >= 0) {
            if (this.activeSlot < 7 && ItemStack.matches(this.inventory.getStackInSlot(this.activeSlot), this.activeToken) && !this.prepared.isEmpty()) {
               int destination = this.outputSlot(this.prepared);
               if (destination < 0) {
                  this.status = 3;
               } else {
                  int cost = this.requiredFluid();
                  if (cost > this.tank.getFluidAmount()) {
                     this.status = 2;
                  } else {
                     this.status = 1;
                     this.progress++;
                     this.setChanged();
                     if (this.progress >= MachineConfig.get().processingTicks()) {
                        if (cost > 0 && this.tank.drain(cost, FluidAction.SIMULATE).getAmount() != cost) {
                           this.status = 2;
                        } else {
                           ItemStack result = this.prepared.copy();
                           int slot = this.activeSlot;
                           this.clearJob();
                           if (cost > 0) {
                              this.tank.drain(cost, FluidAction.EXECUTE);
                           }

                           this.inventory.setStackInSlot(slot, ItemStack.EMPTY);
                           ItemStack existing = this.inventory.getStackInSlot(destination);
                           this.inventory
                              .setStackInSlot(destination, existing.isEmpty() ? result : existing.copyWithCount(existing.getCount() + result.getCount()));
                           this.status = 0;
                        }
                     }
                  }
               }
            } else {
               this.clearJob();
            }
         } else if (this.idleDelay-- <= 0) {
            this.idleDelay = 20;
            this.status = 0;
            if (!this.hasOutputRoom()) {
               this.status = 3;
            } else {
               for (int slot = 0; slot < 7; slot++) {
                  ItemStack original = this.inventory.getStackInSlot(slot);
                  if (!original.isEmpty()) {
                     if (original.getItem() instanceof UnidentifiedWeaponItem && original.getCount() == 1) {
                        try {
                           ItemStack token = original.copy();
                           UnidentifiedWeaponItem.bindRollSeed(token);
                           if (!ItemStack.matches(token, original)) {
                              this.inventory.setStackInSlot(slot, token.copy());
                           }

                           ItemStack base = UnidentifiedWeaponItem.baseResult(world, token);
                           if (!ItemStack.matches(token, this.inventory.getStackInSlot(slot))) {
                              this.inventory.setStackInSlot(slot, token.copy());
                           }

                           if (base.isEmpty()) {
                              this.status = 4;
                           } else {
                              this.xpCost = (int)Math.min(
                                 2.147483647E9, Math.ceil((double)UnidentifiedWeaponItem.cost(token, base) * ExperienceCosts.get().multiplier())
                              );
                              if (this.requiredFluid() > this.tank.getFluidAmount()) {
                                 this.status = 2;
                              } else {
                                 FakePlayer context = FakePlayerFactory.getMinecraft(world);
                                 context.setPos(
                                    (double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5
                                 );
                                 ItemStack result = ApotheosisCompat.apply(
                                    base, UnidentifiedWeaponItem.tier(token), context, UnidentifiedWeaponItem.data(token).getLong("roll_seed")
                                 );
                                 if (!result.isEmpty() && this.outputSlot(result) >= 0) {
                                    this.activeSlot = slot;
                                    this.activeToken = token.copy();
                                    this.prepared = result.copy();
                                    this.lastFailure = ItemStack.EMPTY;
                                    this.progress = 0;
                                    this.status = 1;
                                    this.setChanged();
                                    return;
                                 }

                                 this.status = 3;
                              }
                           }
                        } catch (RuntimeException var8) {
                           this.status = 4;
                           if (!ItemStack.matches(this.lastFailure, original)) {
                              this.lastFailure = original.copy();
                              TnkLoot.LOG.warn("Mechanical Identifier at {} could not prepare this token; input and fluid retained", this.worldPosition, var8);
                           }
                        }
                     } else {
                        this.status = 4;
                     }
                  }
               }
            }
         }
      } else {
         this.status = 5;
      }
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("Inventory", this.inventory.serializeNBT(registries));
      tag.put("Tank", this.tank.writeToNBT(registries, new CompoundTag()));
      tag.putInt("ActiveSlot", this.activeSlot);
      tag.putInt("Progress", this.progress);
      tag.putInt("XpCost", this.xpCost);
      if (!this.prepared.isEmpty()) {
         tag.put("Prepared", this.prepared.save(registries));
      }

      if (!this.activeToken.isEmpty()) {
         tag.put("ActiveToken", this.activeToken.save(registries));
      }
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
      this.tank.readFromNBT(registries, tag.getCompound("Tank"));
      this.activeSlot = tag.contains("ActiveSlot") ? tag.getInt("ActiveSlot") : -1;
      this.progress = Math.clamp((long)tag.getInt("Progress"), 0, MachineConfig.get().processingTicks());
      this.xpCost = Math.max(0, tag.getInt("XpCost"));
      this.prepared = ItemStack.parseOptional(registries, tag.getCompound("Prepared"));
      this.activeToken = ItemStack.parseOptional(registries, tag.getCompound("ActiveToken"));
      if (this.activeSlot < 0 || this.activeSlot >= 7 || this.prepared.isEmpty() || this.activeToken.isEmpty()) {
         this.activeSlot = -1;
         this.prepared = this.activeToken = ItemStack.EMPTY;
         this.progress = this.xpCost = 0;
      }
   }

   public Component getDisplayName() {
      return Component.translatable("block.tnkloot.mechanical_identifier");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new MechanicalIdentifierMenu(id, inv, this);
   }

   private final class ItemHandlerView implements IItemHandler {
      private final boolean outputs;

      ItemHandlerView(boolean outputs) {
         this.outputs = outputs;
      }

      public int getSlots() {
         return 7;
      }

      private int index(int slot) {
         if (slot >= 0 && slot < 7) {
            return slot + (this.outputs ? 7 : 0);
         } else {
            throw new IndexOutOfBoundsException(slot);
         }
      }

      public ItemStack getStackInSlot(int slot) {
         return MechanicalIdentifierEntity.this.inventory.getStackInSlot(this.index(slot));
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return this.outputs ? stack : MechanicalIdentifierEntity.this.inventory.insertItem(this.index(slot), stack, simulate);
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return this.outputs ? MechanicalIdentifierEntity.this.inventory.extractItem(this.index(slot), amount, simulate) : ItemStack.EMPTY;
      }

      public int getSlotLimit(int slot) {
         return MechanicalIdentifierEntity.this.inventory.getSlotLimit(this.index(slot));
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return !this.outputs && MechanicalIdentifierEntity.this.inventory.isItemValid(this.index(slot), stack);
      }
   }
}
