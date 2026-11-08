package dev.mikaaah.tnkloot.client;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.machine.MechanicalIdentifier;
import dev.mikaaah.tnkloot.menu.TnkMenus;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

@EventBusSubscriber(
   modid = "tnkloot",
   value = {Dist.CLIENT}
)
public final class TnkClientMenus {
   @SubscribeEvent
   public static void renderers(RegisterRenderers e) {
      e.registerEntityRenderer(TnkLoot.IDENTIFIER_ENTITY.get(), IdentifierRenderer::new);
   }

   @SubscribeEvent
   public static void register(RegisterMenuScreensEvent e) {
      e.register((MenuType)TnkMenus.IDENTIFY.get(), IdentifierScreen::new);
      e.register((MenuType)TnkMenus.ADMIN.get(), TnkMenuScreen::new);
      if (MechanicalIdentifier.ENABLED) {
         e.register(MechanicalIdentifier.MENU.get(), MechanicalIdentifierScreen::new);
      }
   }
}
