package dev.mikaaah.tnkprogression;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
public final class ProgressionData extends SavedData {
 private CompoundTag root=new CompoundTag();
 public static ProgressionData get(MinecraftServer server) {return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ProgressionData::new,ProgressionData::load),"tnkprogression");}
 public CompoundTag entry(String key) { if(!root.contains(key))root.put(key,new CompoundTag()); return root.getCompound(key); }
 public static ProgressionData load(CompoundTag tag,HolderLookup.Provider lookup){ProgressionData d=new ProgressionData();d.root=tag.getCompound("progression");return d;}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){tag.put("progression",root);return tag;}
}
