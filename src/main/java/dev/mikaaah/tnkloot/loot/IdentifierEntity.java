package dev.mikaaah.tnkloot.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

public final class IdentifierEntity extends Villager {
   public IdentifierEntity(EntityType<? extends Villager> type, Level level) {
      super(type, level);
      this.setNoAi(true);
      this.setInvulnerable(true);
      this.setPersistenceRequired();
      this.setCustomName(Component.translatable("entity.tnkloot.identifier").withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD}));
      this.setCustomNameVisible(true);
   }
}
