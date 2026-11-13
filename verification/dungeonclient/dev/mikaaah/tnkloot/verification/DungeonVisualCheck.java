package dev.mikaaah.tnkloot.verification;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.dungeon.SpiralCompat;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** Development-only: the actual Spiral screen, native tooltips and component-aware icons. */
@EventBusSubscriber(modid="tnkloot", value=Dist.CLIENT)
public final class DungeonVisualCheck {
    private static boolean opening, requested;
    private static volatile Object packet;
    private static int stage, ticks, scale=2;
    private static double targetX,targetY;
    private static final String[] NAMES={"overview","magic-helmet","potion","golden-prize"};
    private static Object field(Object object,String name) throws Exception {
        var field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);
    }
    private static void set(Object object,String name,Object value) throws Exception {
        var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        try {
            if(mc.player==null || mc.level==null) {
                if(!opening && mc.screen instanceof TitleScreen) {
                    opening=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                    mc.createWorldOpenFlows().createFreshLevel("DungeonVisual113",
                        new LevelSettings("Dungeon Reward Verification",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                        new WorldOptions(12345L,false,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
                }
                return;
            }
            if(!requested) {
                requested=true;
                var server=mc.getSingleplayerServer();
                server.execute(()->{
                    try {
                        var owner=server.getPlayerList().getPlayer(mc.player.getUUID());
                        var network=Class.forName("com.pla.spiral_abyss.network.AbyssNetwork");
                        var previews=network.getDeclaredMethod("previews",ServerPlayer.class);previews.setAccessible(true);
                        var floors=(List<?>)previews.invoke(null,owner);
                        var type=Class.forName("com.pla.spiral_abyss.network.AbyssNetwork$OpenFloors");
                        packet=type.getConstructor(UUID.class,int.class,int.class,int.class,int.class,boolean.class,long.class,List.class)
                            .newInstance(UUID.randomUUID(),0,254,254,254,false,86_400_000L,floors);
                    } catch(Throwable error) { TnkLoot.LOG.error("TNKLOOT_DUNGEON_VISUAL_FAILED",error);mc.execute(mc::stop); }
                });
                return;
            }
            if(packet==null)return;
            if(ticks++==0)open(mc);
            set(mc.mouseHandler,"xpos",targetX);set(mc.mouseHandler,"ypos",targetY);
            if(ticks==40) {
                if(stage>0 && (int)field(field(mc.screen,"rewardPanel"),"hovered")<0)throw new AssertionError("Tooltip was not actually hovered");
                var directory=Path.of("visual-check");Files.createDirectories(directory);
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    image.writeToFile(directory.resolve(NAMES[stage]+"-scale-"+scale+".png"));
                }
                if(++stage==NAMES.length) {
                    if(scale==2) {scale=3;stage=0;ticks=0;return;}
                    TnkLoot.LOG.info("TNKLOOT_DUNGEON_VISUAL_OK: actual Spiral floor screen, normal odds, magic helmet, potion and golden prize at scales 2 and 3");
                    mc.stop();return;
                }
                ticks=0;
            }
        } catch(Throwable error) {TnkLoot.LOG.error("TNKLOOT_DUNGEON_VISUAL_FAILED",error);mc.stop();}
    }
    private static void open(Minecraft mc) throws Exception {
        mc.options.guiScale().set(scale);mc.resizeDisplay();
        var type=Class.forName("com.pla.spiral_abyss.client.screen.FloorSelectionScreen");
        var screen=(Screen)type.getConstructor(packet.getClass()).newInstance(packet);
        set(screen,"keepOpenTicks",10_000);
        mc.setScreen(screen);
        var navigate=type.getDeclaredMethod("navigate",int.class,int.class);navigate.setAccessible(true);navigate.invoke(screen,1,0);
        double mx=12,my=12;
        if(stage>0) {
            Object panel=field(screen,"rewardPanel");var rewards=(List<?>)field(panel,"rewards");
            int chosen=-1;
            for(int i=0;i<rewards.size();i++) {
                var reward=rewards.get(i);var icon=(ItemStack)SpiralCompat.get(reward,"icon");
                String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(icon.getItem()).toString();
                if(stage==1 && id.equals("irons_spellbooks:wizard_helmet")) {
                    if(Screen.getTooltipFromItem(mc,icon).stream().noneMatch(line->line.getString().contains("Bonus: 25 Max Mana")))throw new AssertionError("Guaranteed mana is missing from the actual tooltip");
                }
                if(stage==1 && id.equals("irons_spellbooks:wizard_helmet") || stage==2 && id.equals("minecraft:potion") || stage==3 && (boolean)SpiralCompat.get(reward,"rare")) {chosen=i;break;}
            }
            if(chosen<0)throw new AssertionError("Missing visual fixture: "+NAMES[stage]);
            var columns=panel.getClass().getDeclaredMethod("columns");columns.setAccessible(true);int count=(int)columns.invoke(panel);
            int cell=(int)field(panel,"cellSize"),tile=(int)field(panel,"tileSize");
            int scroll=chosen/count*cell;set(panel,"scroll",scroll);
            mx=(int)field(panel,"x")+chosen%count*cell+tile/2.0;
            my=(int)field(panel,"y")+tile/2.0;
        }
        targetX=mx*mc.getWindow().getGuiScale();targetY=my*mc.getWindow().getGuiScale();
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),targetX,targetY);
        var move=mc.mouseHandler.getClass().getDeclaredMethod("onMove",long.class,double.class,double.class);move.setAccessible(true);
        move.invoke(mc.mouseHandler,mc.getWindow().getWindow(),mx*mc.getWindow().getGuiScale(),my*mc.getWindow().getGuiScale());
    }
}
