package dev.mikaaah.tnkloot.machine;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.boss.BossItems;
import dev.mikaaah.tnkloot.boss.BossStore;
import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import dev.mikaaah.tnkloot.workshop.ExperienceCosts;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;

public final class MachineSmoke {
   private static void require(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }

   private static void tick(ServerLevel world, BlockPos pos, MechanicalIdentifierEntity machine, int n) {
      for (int i = 0; i < n; i++) {
         MechanicalIdentifierEntity.tick(world, pos, world.getBlockState(pos), machine);
      }
   }

   public static void run(ServerStartedEvent event) {
      if (Boolean.getBoolean("tnkloot.machineSmokeTest")) {
         MinecraftServer server = event.getServer();
         ServerLevel world = server.overworld();
         BlockPos pos = new BlockPos(10, 100, 10);
         BlockState old = world.getBlockState(pos);

         try {
            ResourceLocation xpId = ResourceLocation.parse("create_enchantment_industry:experience");
            require(BuiltInRegistries.FLUID.containsKey(xpId), "Real Enchantment Industry XP fluid missing from test runtime");
            Fluid xp = (Fluid)BuiltInRegistries.FLUID.get(xpId);
            BlockState state = (BlockState)MechanicalIdentifier.BLOCK.get().defaultBlockState().setValue(MechanicalIdentifierBlock.FACING, Direction.NORTH);
            world.setBlock(pos, state, 3);
            MechanicalIdentifierEntity machine = (MechanicalIdentifierEntity)world.getBlockEntity(pos);
            require(machine != null, "Block entity did not register");
            require(world.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(2.0)).isEmpty(), "Mechanical identifier spawned an NPC");
            IFluidHandler pipe = (IFluidHandler)world.getCapability(FluidHandler.BLOCK, pos, Direction.EAST);
            require(pipe != null && world.getCapability(FluidHandler.BLOCK, pos, Direction.WEST) == null, "Wrong fluid port side");
            require(pipe.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0, "Machine accepted water as XP");
            require(pipe.fill(new FluidStack(xp, 5000), FluidAction.SIMULATE) == 4000 && pipe.getFluidInTank(0).isEmpty(), "Tank simulation changed contents");
            require(pipe.fill(new FluidStack(xp, 5000), FluidAction.EXECUTE) == 4000 && pipe.getTankCapacity(0) == 4000, "Tank capacity/overflow failed");
            require(pipe.drain(1000, FluidAction.EXECUTE).isEmpty(), "Input port allowed automated fuel extraction");
            IItemHandler input = (IItemHandler)world.getCapability(ItemHandler.BLOCK, pos, Direction.UP);
            IItemHandler output = (IItemHandler)world.getCapability(ItemHandler.BLOCK, pos, Direction.DOWN);
            require(
               input != null && output != null && input.insertItem(0, new ItemStack(Items.STONE), false).is(Items.STONE), "Input validation/capability failed"
            );
            ItemStack token = UnidentifiedWeaponItem.create(1);
            RaritySupport.force(token, "rare");
            CompoundTag tokenData = UnidentifiedWeaponItem.data(token);
            int xpCost = (int)Math.ceil(
               (double)UnidentifiedWeaponItem.cost(token, UnidentifiedWeaponItem.baseResult(world, token.copy())) * ExperienceCosts.get().multiplier()
            );
            require(input.insertItem(0, token.copy(), true).isEmpty() && input.getStackInSlot(0).isEmpty(), "Simulated input changed inventory");
            require(input.insertItem(0, token.copy(), false).isEmpty() && input.extractItem(0, 1, false).isEmpty(), "Input insertion/extraction policy failed");
            require(output.insertItem(0, token.copy(), false).getCount() == 1, "Output accepted insertion");
            tick(world, pos, machine, 8);
            require(input.getStackInSlot(0).getItem() instanceof UnidentifiedWeaponItem, "Input consumed before processing finished");
            CompoundTag saved = machine.saveWithFullMetadata(world.registryAccess());
            MechanicalIdentifierEntity loaded = new MechanicalIdentifierEntity(pos, state);
            loaded.loadWithComponents(saved, world.registryAccess());
            world.setBlockEntity(loaded);
            world.invalidateCapabilities(pos);
            machine = loaded;
            tick(world, pos, loaded, MachineConfig.get().processingTicks() + 3);
            ItemStack result = loaded.inventory().getStackInSlot(7).copy();
            require(!result.isEmpty() && !(result.getItem() instanceof UnidentifiedWeaponItem), "Machine did not finish identification after reload");
            require(
               UnidentifiedWeaponItem.data(result).getString("tnk_rarity").equals("rare") && UnidentifiedWeaponItem.data(result).getInt("tnk_tier") == 1,
               "Machine changed sealed tier/rarity"
            );
            require(AffixHelper.hasAffixes(result), "Automated output has no actual Apotheosis affixes");
            require(loaded.fluidInput().getFluidInTank(0).getAmount() == 4000 - xpCost, "Machine charged the wrong liquid XP amount");
            int paid = loaded.fluidInput().getFluidInTank(0).getAmount();
            tick(world, pos, loaded, 90);
            require(
               loaded.fluidInput().getFluidInTank(0).getAmount() == paid && loaded.inventory().getStackInSlot(7).getCount() == 1,
               "Idle machine charged twice or duplicated output"
            );
            loaded.inventory().setStackInSlot(7, ItemStack.EMPTY);

            for (int i = 7; i < 14; i++) {
               machine.inventory().setStackInSlot(i, new ItemStack(Items.STONE, 64));
            }

            machine.inventory().setStackInSlot(0, token.copy());
            tick(world, pos, machine, 70);
            require(
               machine.inventory().getStackInSlot(0).getItem() instanceof UnidentifiedWeaponItem && machine.fluidInput().getFluidInTank(0).getAmount() == paid,
               "Blocked output consumed fuel or input"
            );

            for (int i = 7; i < 14; i++) {
               machine.inventory().setStackInSlot(i, ItemStack.EMPTY);
            }

            tick(world, pos, machine, 5);
            machine.inventory().setStackInSlot(0, ItemStack.EMPTY);
            tick(world, pos, machine, 60);
            require(
               machine.inventory().getStackInSlot(7).isEmpty() && machine.fluidInput().getFluidInTank(0).getAmount() == paid,
               "Removed input duplicated output or lost XP"
            );
            machine.loadWithComponents(new MechanicalIdentifierEntity(pos, state).saveWithFullMetadata(world.registryAccess()), world.registryAccess());
            machine.inventory().setStackInSlot(0, token.copy());
            tick(world, pos, machine, 70);
            require(
               machine.inventory().getStackInSlot(0).getItem() instanceof UnidentifiedWeaponItem && machine.inventory().getStackInSlot(7).isEmpty(),
               "Unpaid identification succeeded"
            );
            machine.fluidInput().fill(new FluidStack(xp, 1000), FluidAction.EXECUTE);
            ItemStack invalid = new ItemStack((ItemLike)BossItems.DROP.get());
            CompoundTag invalidData = UnidentifiedWeaponItem.data(token);
            invalidData.putString("boss_pool", "test/unknown");
            invalid.set(DataComponents.CUSTOM_DATA, CustomData.of(invalidData));
            UnidentifiedWeaponItem.bindRollSeed(invalid);
            DimensionLoot.bindOrigin(invalid, world);
            machine.inventory().setStackInSlot(0, invalid.copy());
            tick(world, pos, machine, 70);
            require(
               machine.fluidInput().getFluidInTank(0).getAmount() == 1000 && ItemStack.matches(machine.inventory().getStackInSlot(0), invalid),
               "Invalid boss pool consumed token or XP"
            );
            Path bossPath = BossRewards.path();
            BossRewards.reload();
            String prior = Files.readString(bossPath);

            try {
               String poolId = BossStore.add("minecraft:silverfish");
               BossStore.importItem(poolId, new ItemStack(Items.DIAMOND_SWORD));
               BossStore.edit(poolId, p -> p.addProperty("enabled", true));
               BossRewards.Pool pool = BossRewards.find(poolId).orElseThrow();
               ItemStack boss = BossRewards.token(pool, TnkConfig.Category.WEAPON, world, ItemStack.EMPTY);
               machine.inventory().setStackInSlot(0, boss);
               tick(world, pos, machine, MachineConfig.get().processingTicks() + 25);
               ItemStack bossResult = machine.inventory().getStackInSlot(7);
               require(
                  bossResult.is(Items.DIAMOND_SWORD) && UnidentifiedWeaponItem.data(bossResult).getString("boss_pool").equals(poolId),
                  "Automated boss crate lost its exclusive pool"
               );
            } finally {
               Files.writeString(bossPath, prior);
               BossRewards.reload();
            }

            TnkLoot.LOG
               .info(
                  "TNKLOOT_MACHINE_OK: actual Create/CEI runtime, no NPC, sided capabilities, real liquid XP, capacity, simulations, affixes, tier/rarity, persisted in-flight job, no double payment, blocked/removed inputs, no-fluid and invalid boss safety"
               );
         } catch (Throwable var36) {
            TnkLoot.LOG.error("TNKLOOT_MACHINE_FAILED", var36);
         } finally {
            world.setBlock(pos, old, 3);
            server.halt(false);
         }
      }
   }
}
