package dev.mikaaah.tnkprogression;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = "tnkprogression", value = Dist.CLIENT)
public final class LevelBadgeClient {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        LevelBadges.clearClient();
    }
}
