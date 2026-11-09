package dev.mikaaah.tnkloot.apotheosis;

import com.google.gson.*;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.shadowsoffire.apotheosis.AdventureConfig;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import java.nio.file.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class ApotheosisIntegration {
    private ApotheosisIntegration() {}
    private static volatile boolean pinnacle = true;
    private static Boolean manualBeforeForce;
    public static boolean forcePinnacle() { return pinnacle; }
    public static void clientPolicy(boolean force) { pinnacle=force; }
    private static void syncState(ServerPlayer player,boolean force) {
        if(player.connection==null)return;
        int level=dev.mikaaah.tnkloot.loot.TnkLevelBridge.level(player),ceiling=dev.mikaaah.tnkloot.loot.TnkLevelBridge.ceiling(player),cap=dev.mikaaah.tnkloot.loot.TnkLevelBridge.rarityCap(player);
        int packed=level|(ceiling<<8)|(cap<<16)|(pinnacle?1<<20:0);
        var data=player.getPersistentData();
        if(force || data.getInt("tnkloot_apotheosis_sync")!=packed) {
            data.putInt("tnkloot_apotheosis_sync",packed);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new dev.mikaaah.tnkloot.menu.TnkMenuNetwork.ApotheosisState(level,ceiling,cap,pinnacle));
        }
    }
    public static boolean reload() {
        try {
            Path path=FMLPaths.CONFIGDIR.get().resolve("tnkloot/apotheosis-integration.json");
            Files.createDirectories(path.getParent());
            if(!Files.exists(path)) Files.writeString(path,"{\n  \"force_pinnacle\": true\n}\n");
            var object=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            var value=object.get("force_pinnacle");
            if(value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("force_pinnacle must be boolean");
            pinnacle=value.getAsBoolean();
            if(!pinnacle && manualBeforeForce!=null) AdventureConfig.enableManualWorldTierChanges=manualBeforeForce;
            return true;
        } catch(Exception error) { TnkLoot.LOG.error("Invalid Apotheosis integration settings; retaining previous settings",error); return false; }
    }
    private static void configureTierSelection() {
        if(pinnacle) {
            if(manualBeforeForce==null) manualBeforeForce=AdventureConfig.enableManualWorldTierChanges;
            AdventureConfig.enableManualWorldTierChanges=false;
        }
    }
    public static void force(ServerPlayer player) {
        if(pinnacle) WorldTier.setTier(player,WorldTier.PINNACLE);
    }
    public static void register() {
        reload();
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,ApotheosisIntegration::join);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,ApotheosisIntegration::sync);
        NeoForge.EVENT_BUS.addListener(ApotheosisIntegration::started);
        NeoForge.EVENT_BUS.addListener(ApotheosisIntegration::tick);
    }
    private static void join(EntityJoinLevelEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) { configureTierSelection(); force(player);syncState(player,true); }
    }
    private static void sync(OnDatapackSyncEvent event) { configureTierSelection();event.getRelevantPlayers().forEach(player->syncState(player,true)); }
    private static void started(ServerStartedEvent event) {
        configureTierSelection(); event.getServer().getPlayerList().getPlayers().forEach(ApotheosisIntegration::force);
    }
    private static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer player && player.tickCount%40==0) {
            if(pinnacle && WorldTier.getTier(player)!=WorldTier.PINNACLE) force(player);
            syncState(player,false);
            for(int slot=0;slot<player.getInventory().getContainerSize();slot++) {
                var stack=player.getInventory().getItem(slot);var tag=UnidentifiedWeaponItem.data(stack);
                if(!(stack.getItem() instanceof UnidentifiedWeaponItem) && tag.getBoolean("tnk_external_generated")) {
                    tag.remove("tnk_external_generated");stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
                }
            }
        }
    }
}
