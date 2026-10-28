package dev.mikaaah.tnkprogression;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Adds a font-backed badge without changing scoreboards, usernames or chat messages. */
public final class LevelBadges {
    public static final ResourceLocation FONT = ResourceLocation.parse("tnkprogression:level_badges");
    // Only read/written on the client main thread. Server names always use authoritative profiles.
    private static final Map<UUID, Integer> CLIENT_LEVELS = new HashMap<>();

    public static int clamp(int level) { return Math.max(0, Math.min(100, level)); }
    public static String glyph(int level) { return String.valueOf((char) (0xE000 + clamp(level))); }
    public static Component badge(int level) {
        int value = clamp(level);
        return Component.literal(glyph(value)).withStyle(Style.EMPTY.withFont(FONT).withColor(0xFFFFFF)
            .withBold(false).withItalic(false)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Character level " + value))));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void format(PlayerEvent.NameFormat event) {
        Integer level = event.getEntity() instanceof ServerPlayer player
            ? ProgressionAPI.level(player) : CLIENT_LEVELS.get(event.getEntity().getUUID());
        // A newly arriving client entity can briefly have no snapshot yet; never invent a level.
        if (level != null) event.setDisplayname(Component.empty().append(badge(level))
            .append(Component.literal(" ")).append(event.getDisplayname()));
    }

    public static void accept(UUID id, int level, net.minecraft.world.entity.player.Player viewer) {
        CLIENT_LEVELS.put(id, clamp(level));
        var target = viewer.level().getPlayerByUUID(id);
        if (target != null) target.refreshDisplayName();
    }
    public static void clearClient() { CLIENT_LEVELS.clear(); }

    public static void changed(ServerPlayer player) {
        player.refreshDisplayName();
        PacketDistributor.sendToAllPlayers(new LevelBadgePayload(player.getUUID(), ProgressionAPI.level(player)));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (ServerPlayer online : player.server.getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(player, new LevelBadgePayload(online.getUUID(), ProgressionAPI.level(online)));
        changed(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) changed(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) changed(player);
    }
}
