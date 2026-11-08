package dev.mikaaah.tnkloot.client;
import dev.mikaaah.tnkloot.menu.TnkMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(modid="tnkloot",value=Dist.CLIENT)
public final class WorkshopClient {
 @SubscribeEvent public static void register(RegisterMenuScreensEvent e){e.register(TnkMenus.SALVAGE.get(),SalvageScreen::new);e.register(TnkMenus.PREVIEW.get(),PreviewScreen::new);e.register(TnkMenus.COLLECTION.get(),CollectionScreen::new);}
 public static final class Screens {}
}
