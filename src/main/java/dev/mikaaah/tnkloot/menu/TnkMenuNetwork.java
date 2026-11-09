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
      event.registrar("3").playToClient(ApotheosisState.TYPE,ApotheosisState.CODEC,
         (message,context)->context.enqueueWork(()->dev.mikaaah.tnkloot.apotheosis.ApotheosisClientState.update(message.level(),message.ceiling(),message.rarityCap(),message.pinnacle())));
      event.registrar("3")
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
   public static record ApotheosisState(int level,int ceiling,int rarityCap,boolean pinnacle) implements CustomPacketPayload {
      public static final Type<ApotheosisState> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("tnkloot","apotheosis_state"));
      public static final StreamCodec<RegistryFriendlyByteBuf,ApotheosisState> CODEC=StreamCodec.of((buffer,state)->{
         buffer.writeVarInt(state.level());buffer.writeVarInt(state.ceiling());buffer.writeVarInt(state.rarityCap());buffer.writeBoolean(state.pinnacle());
      },buffer->new ApotheosisState(buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readBoolean()));
      public Type<ApotheosisState> type() { return TYPE; }
   }

}
