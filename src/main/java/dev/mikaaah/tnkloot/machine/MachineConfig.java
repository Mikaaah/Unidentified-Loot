package dev.mikaaah.tnkloot.machine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

public final class MachineConfig {
   private static MachineConfig.Settings current = new MachineConfig.Settings(
      4000, 40, Map.of("create_enchantment_industry:experience", 1.0, "sophisticatedcore:xp_still", 20.0)
   );

   private MachineConfig() {
   }

   public static MachineConfig.Settings get() {
      return current;
   }

   public static boolean reload() {
      try {
         Path path = TnkConfig.path().resolveSibling("mechanical-identifier.json");
         Gson gson = new GsonBuilder().setPrettyPrinting().create();
         if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(current));
         }

         MachineConfig.Settings candidate = (MachineConfig.Settings)gson.fromJson(Files.readString(path), MachineConfig.Settings.class);
         validate(candidate);
         current = new MachineConfig.Settings(candidate.tankCapacity(), candidate.processingTicks(), Map.copyOf(candidate.millibucketsPerXp()));
         return true;
      } catch (Exception var3) {
         TnkLoot.LOG.error("Invalid mechanical-identifier.json; retaining working machine settings", var3);
         return false;
      }
   }

   public static void validate(MachineConfig.Settings s) {
      if (s != null
         && s.tankCapacity() >= 1000
         && s.tankCapacity() <= 1000000
         && s.processingTicks() >= 1
         && s.processingTicks() <= 12000
         && s.millibucketsPerXp() != null
         && !s.millibucketsPerXp().isEmpty()
         && s.millibucketsPerXp().size() <= 128) {
         s.millibucketsPerXp().forEach((id, unit) -> {
            if (ResourceLocation.tryParse(id) == null || unit == null || !Double.isFinite(unit) || unit <= 0.0 || unit > 1000.0) {
               throw new IllegalArgumentException("Invalid XP fluid conversion: " + id);
            }
         });
      } else {
         throw new IllegalArgumentException("Invalid tank or processing settings");
      }
   }

   public static double unit(FluidStack stack) {
      return stack.isEmpty() ? 0.0 : current.millibucketsPerXp().getOrDefault(BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(), 0.0);
   }

   public static int fluidCost(int xp, double unit) {
      return xp <= 0 ? 0 : (unit <= 0.0 ? Integer.MAX_VALUE : (int)Math.min(2.147483647E9, Math.ceil((double)xp * unit)));
   }

   public static record Settings(int tankCapacity, int processingTicks, Map<String, Double> millibucketsPerXp) {
   }
}
