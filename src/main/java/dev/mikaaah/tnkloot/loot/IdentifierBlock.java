package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.menu.TnkMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class IdentifierBlock extends Block {
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

   private static String npcTag(BlockPos pos) {
      return "tnkloot.identifier." + pos.getX() + "." + pos.getY() + "." + pos.getZ();
   }

   public IdentifierBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public static void particles(ServerLevel level, Villager villager, boolean arriving) {
      level.sendParticles(
         arriving ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.POOF, villager.getX(), villager.getY() + 1.0, villager.getZ(), 14, 0.3, 0.6, 0.3, 0.025
      );
      level.playSound(
         null,
         villager.blockPosition(),
         arriving ? SoundEvents.ENCHANTMENT_TABLE_USE : SoundEvents.ILLUSIONER_MIRROR_MOVE,
         SoundSource.BLOCKS,
         0.45F,
         arriving ? 1.3F : 1.1F
      );
   }

   public static void position(Villager villager, BlockPos pos, BlockState state) {
      Direction front = (Direction)state.getValue(FACING);
      double x = (double)pos.getX() + 0.5 - (double)front.getStepX() * 1.35;
      double z = (double)pos.getZ() + 0.5 - (double)front.getStepZ() * 1.35;
      float yaw = front.toYRot();
      villager.moveTo(x, (double)pos.getY(), z, yaw, 0.0F);
      villager.setYHeadRot(yaw);
      villager.setYBodyRot(yaw);
      villager.yRotO = yaw;
      villager.yHeadRotO = yaw;
      villager.yBodyRotO = yaw;
      villager.setDeltaMovement(Vec3.ZERO);
      villager.setNoAi(true);
      villager.setNoGravity(true);
      villager.setInvulnerable(true);
      villager.clearFire();
   }

   public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.isClientSide) {
         return ItemInteractionResult.SUCCESS;
      } else if (player instanceof ServerPlayer serverPlayer) {
         if (!TnkConfig.get().stationIdentification()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.tnkloot.station_disabled"));
            return ItemInteractionResult.FAIL;
         } else {
            TnkMenuProvider.openIdentifier(serverPlayer, pos);
            return ItemInteractionResult.SUCCESS;
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && TnkConfig.get().stationIdentification()) {
         TnkMenuProvider.openIdentifier(serverPlayer, pos);
      }

      return InteractionResult.SUCCESS;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
         AABB area = new AABB(pos).inflate(1.8);
         String tag = npcTag(pos);
         boolean exists = !serverLevel.getEntitiesOfClass(Villager.class, area, v -> v.getTags().contains(tag)).isEmpty();
         if (!exists) {
            Villager villager = (Villager)TnkLoot.IDENTIFIER_ENTITY.get().create(serverLevel);
            if (villager != null) {
               position(villager, pos, state);
               villager.setNoAi(true);
               villager.setInvulnerable(true);
               villager.setPersistenceRequired();
               villager.setCustomName(
                  Component.translatable("entity.tnkloot.identifier").withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD})
               );
               villager.setCustomNameVisible(true);
               villager.addTag(tag);
               serverLevel.addFreshEntity(villager);
               particles(serverLevel, villager, true);
            }
         }
      }
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && !level.isClientSide && level instanceof ServerLevel serverLevel) {
         String tag = npcTag(pos);
         serverLevel.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(1.8), v -> v.getTags().contains(tag)).forEach(v -> {
            particles(serverLevel, v, false);
            v.discard();
         });
      }

      super.onRemove(state, level, pos, newState, moved);
   }
}
