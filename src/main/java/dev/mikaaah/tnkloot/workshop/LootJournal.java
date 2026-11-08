package dev.mikaaah.tnkloot.workshop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import vazkii.patchouli.api.PatchouliAPI;
public final class LootJournal {
 public static final ResourceLocation BOOK=ResourceLocation.parse("tnkloot:loot_journal");
 public static void open(ServerPlayer player){PatchouliAPI.get().openBookGUI(player,BOOK);}
 private LootJournal(){}
}
