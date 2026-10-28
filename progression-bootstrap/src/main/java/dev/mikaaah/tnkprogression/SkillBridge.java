package dev.mikaaah.tnkprogression;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import java.util.Optional;
public final class SkillBridge {
 private static boolean warned;
 public static void sync(ServerPlayer p){
  if(!ModList.get().isLoaded("puffish_skills"))return;
  try{
   Class<?> api=Class.forName("net.puffish.skillsmod.api.SkillsAPI");
   Optional<?> category=(Optional<?>)api.getMethod("getCategory",ResourceLocation.class).invoke(null,ResourceLocation.parse(ProgressionConfig.current.skillCategory));
   if(category.isEmpty())return;
   Class<?> type=Class.forName("net.puffish.skillsmod.api.Category");
   type.getMethod("setPoints",ServerPlayer.class,ResourceLocation.class,int.class).invoke(category.get(),p,ResourceLocation.parse("tnkprogression:character_level"),ProgressionAPI.level(p));
  }catch(ReflectiveOperationException|RuntimeException e){if(!warned){warned=true;TnkProgression.LOG.warn("Pufferfish API unavailable: check installed version/category; no command fallback or duplicate awards",e);}}
 }
}
