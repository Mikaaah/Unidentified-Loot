package dev.mikaaah.tnkloot.client.ponder;

import dev.mikaaah.tnkloot.TnkLoot;
import java.nio.file.*;
import java.util.*;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Development-only real-render verification. Excluded from the release JAR. */
@EventBusSubscriber(modid="tnkloot", value=Dist.CLIENT)
public final class PonderClientCheck {
    private static final List<PonderScene> scenes = new ArrayList<>();
    private static int ticks, sceneIndex, frame;
    private static PonderUI screen;
    private static final int[] TIMES = {65, 250, 450};
    private static final class SingleScene extends PonderUI {
        SingleScene(PonderScene scene) { super(List.of(scene)); }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.level == null) return;
        try {
            if (ticks++ == 0) {
                scenes.addAll(PonderIndex.getSceneAccess().compile(ResourceLocation.parse("tnkloot:identifier")));
                scenes.addAll(PonderIndex.getSceneAccess().compile(ResourceLocation.parse("tnkloot:mechanical_identifier")));
                if (scenes.size() != 4) throw new AssertionError("Expected four Ponder tutorials; got " + scenes.size());
                for (var scene : scenes) {
                    scene.begin(); scene.seekToTime(scene.getTotalTime());
                }
                open(mc);
            }
            if (ticks >= 30) {
                Path out=Path.of("ponder-release-check");Files.createDirectories(out);
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    image.writeToFile(out.resolve("scene-"+sceneIndex+"-frame-"+frame+".png"));
                }
                if (++frame >= TIMES.length) { frame=0; sceneIndex++; }
                if (sceneIndex >= scenes.size()) {
                    TnkLoot.LOG.info("TNKLOOT_PONDER_CLIENT_OK: all four tutorials replayed; twelve rendered checkpoints saved"); mc.stop(); return;
                }
                open(mc); ticks=1;
            }
        } catch (Throwable error) { TnkLoot.LOG.error("TNKLOOT_PONDER_CLIENT_FAILED", error); mc.stop(); }
    }
    private static void open(Minecraft mc) {
        var scene = scenes.get(sceneIndex); screen=new SingleScene(scene); mc.setScreen(screen); scene.begin();
        int key=frame==0?0:frame==1?scene.getKeyframeCount()/2:Math.max(0,scene.getKeyframeCount()-1);
        int time=scene.getKeyframeCount()>0?scene.getKeyframeTime(key)+20:TIMES[frame];
        screen.seekToTime(Math.min(time, Math.max(0, scene.getTotalTime()-5)));
    }
}
