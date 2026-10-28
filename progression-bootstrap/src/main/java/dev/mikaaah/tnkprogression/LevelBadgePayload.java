package dev.mikaaah.tnkprogression;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server-to-client only: clients cannot submit or change anyone's level. */
public record LevelBadgePayload(UUID playerId, int level) implements CustomPacketPayload {
    public static final Type<LevelBadgePayload> TYPE = new Type<>(ResourceLocation.parse("tnkprogression:level_badge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LevelBadgePayload> CODEC = StreamCodec.of(
        (buffer, payload) -> { buffer.writeUUID(payload.playerId()); buffer.writeVarInt(payload.level()); },
        buffer -> new LevelBadgePayload(buffer.readUUID(), buffer.readVarInt()));
    @Override public Type<LevelBadgePayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC,
            (payload, context) -> LevelBadges.accept(payload.playerId(), payload.level(), context.player()));
    }
}
