package dev.mikaaah.tnkloot.client.ponder;

import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;

public final class LootPonderPlugin implements PonderPlugin {
   private static boolean installed;

   public static synchronized void install() {
      if (!installed) {
         PonderIndex.addPlugin(new LootPonderPlugin());
         installed = true;
      }
   }

   public String getModId() {
      return "tnkloot";
   }

   public static List<ResourceLocation> subjects() {
      ArrayList<ResourceLocation> ids = new ArrayList<>();
      ids.add(id("identifier"));
      ids.add(id("loot_journal"));
      ids.add(id("unidentified_boss_drop"));

      for (String category : List.of("weapon", "bow", "helmet", "chestplate", "leggings", "boots", "tool", "accessory")) {
         ids.add(id("unidentified_" + category));
         ids.add(id("boss_unidentified_" + category));
      }

      return List.copyOf(ids);
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("tnkloot", path);
   }

   public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
      for (ResourceLocation subject : subjects()) {
         helper.addStoryBoard(subject, "loot_workshop", LootScenes::loot, new ResourceLocation[0]);
         helper.addStoryBoard(subject, "loot_workshop", LootScenes::identify, new ResourceLocation[0]);
         helper.addStoryBoard(subject, "loot_workshop", LootScenes::salvage, new ResourceLocation[0]);
      }

      helper.addStoryBoard(id("mechanical_identifier"), "mechanical_workshop", LootScenes::mechanical, new ResourceLocation[0]);
   }
}
