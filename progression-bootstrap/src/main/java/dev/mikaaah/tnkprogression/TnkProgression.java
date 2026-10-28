package dev.mikaaah.tnkprogression;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
@Mod("tnkprogression")
public final class TnkProgression {
 public static final Logger LOG=LogUtils.getLogger();
 public TnkProgression(net.neoforged.bus.api.IEventBus modBus){ProgressionConfig.reload();NeoForge.EVENT_BUS.register(ProgressionEvents.class);NeoForge.EVENT_BUS.register(LevelBadges.class);modBus.addListener(LevelBadgePayload::register);NeoForge.EVENT_BUS.addListener(ProgressionCommands::register);NeoForge.EVENT_BUS.addListener(ProgressionSmoke::run);}
}
