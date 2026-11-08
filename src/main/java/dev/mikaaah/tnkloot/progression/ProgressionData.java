package dev.mikaaah.tnkloot.progression;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class ProgressionData extends SavedData {
   private static final Factory<ProgressionData> FACTORY = new Factory<>(ProgressionData::new, ProgressionData::load);
   private final Map<String, Integer> ranks = new HashMap<>();

   public static ProgressionData of(MinecraftServer server) {
      return (ProgressionData)server.overworld().getDataStorage().computeIfAbsent(FACTORY, "tnkloot_progress");
   }

   public int get(String key) {
      return this.ranks.getOrDefault(key, 0);
   }

   public boolean unlock(String key, int rank) {
      if (rank <= this.get(key)) {
         return false;
      } else {
         this.ranks.put(key, rank);
         this.setDirty();
         return true;
      }
   }

   public void set(String key, int rank) {
      if (rank != this.get(key)) {
         if (rank == 0) {
            this.ranks.remove(key);
         } else {
            this.ranks.put(key, rank);
         }

         this.setDirty();
      }
   }

   public CompoundTag save(CompoundTag nbt, Provider registries) {
      CompoundTag entries = new CompoundTag();
      this.ranks.forEach(entries::putInt);
      nbt.put("ranks", entries);
      return nbt;
   }

   static ProgressionData load(CompoundTag nbt, Provider registries) {
      ProgressionData data = new ProgressionData();
      CompoundTag entries = nbt.getCompound("ranks");

      for (String key : entries.getAllKeys()) {
         data.ranks.put(key, Math.max(0, entries.getInt(key)));
      }

      return data;
   }
}
