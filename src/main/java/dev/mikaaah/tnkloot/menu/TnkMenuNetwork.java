package dev.mikaaah.tnkloot.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class TnkMenuNetwork {
   private TnkMenuNetwork() {
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      event.registrar("2")
         .playToServer(
            TnkMenuNetwork.Chance.TYPE,
            TnkMenuNetwork.Chance.CODEC,
            (message, context) -> context.enqueueWork(
                  () -> {
                     if (context.player() instanceof ServerPlayer player
                        && player.containerMenu instanceof TnkMenu menu
                        && menu.containerId == message.container()
                        && message.basisPoints() >= 0
                        && message.basisPoints() <= 10000) {
                        menu.setSelectedChance(player, message.tier(), message.item(), message.basisPoints());
                     }
                  }
               )
         );
   }

   public static record Chance(int container, int tier, String item, int basisPoints) implements CustomPacketPayload {
      public static final Type<TnkMenuNetwork.Chance> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("tnkloot", "pool_chance"));
      public static final StreamCodec<RegistryFriendlyByteBuf, TnkMenuNetwork.Chance> CODEC = StreamCodec.of((b, m) -> {
         b.writeVarInt(m.container);
         b.writeVarInt(m.tier);
         b.writeUtf(m.item, 256);
         b.writeVarInt(m.basisPoints);
      }, b -> new TnkMenuNetwork.Chance(b.readVarInt(), b.readVarInt(), b.readUtf(256), b.readVarInt()));

      public Type<TnkMenuNetwork.Chance> type() {
         return TYPE;
      }
   }
}
