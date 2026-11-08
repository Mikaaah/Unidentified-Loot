package dev.mikaaah.tnkloot.client.ponder;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** This entry point must remain loadable without Create or Ponder. */
@EventBusSubscriber(modid="tnkloot", value=Dist.CLIENT)
public final class OptionalPonderClient {
 @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
  if (ModList.get().isLoaded("create") && ModList.get().isLoaded("ponder"))
   event.enqueueWork(() -> LootPonderPlugin.install());
 }
 private OptionalPonderClient() {}
}
