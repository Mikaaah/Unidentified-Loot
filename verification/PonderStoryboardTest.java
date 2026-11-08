import com.google.gson.JsonParser;
import dev.mikaaah.tnkloot.client.ponder.LootPonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.*;
import java.util.*;

/** Runs the real storyboard lambdas against the real Ponder API, recording instructions without GL. */
public class PonderStoryboardTest {
 static final Map<ResourceLocation,List<PonderStoryBoard>> boards=new LinkedHashMap<>();
 static List<String> captions;static String title;static int ticks,created,removed;
 static Object proxy(Class<?> type) {
  return Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->{
   switch(m.getName()) {
    case "title":title=(String)a[0];break;
    case "text":captions.add((String)a[0]);break;
    case "idle":ticks+=(Integer)a[0];break;
    case "createItemEntity":created++;break;
    case "modifyEntity":removed++;break;
    case "getId":return new UUID(0,1);
    case "toString":return "Recorded "+type.getSimpleName();
   }
   if(m.getReturnType().isInterface())return proxy(m.getReturnType());
   if(m.getReturnType()==boolean.class)return false;
   if(m.getReturnType()==int.class)return 0;
   return null;
  });
 }
 @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception {
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
  PonderSceneRegistrationHelper<ResourceLocation> helper=(PonderSceneRegistrationHelper<ResourceLocation>)Proxy.newProxyInstance(PonderStoryboardTest.class.getClassLoader(),new Class[]{PonderSceneRegistrationHelper.class},(p,m,a)->{
   if(m.getName().equals("addStoryBoard")) {
    if(!a[1].equals("loot_workshop"))throw new AssertionError(a[1]);
    boards.computeIfAbsent((ResourceLocation)a[0],k->new ArrayList<>()).add((PonderStoryBoard)a[2]);
   }
   return null;
  });
  new LootPonderPlugin().registerScenes(helper);
  if(boards.size()!=19||boards.values().stream().anyMatch(b->b.size()!=3))throw new AssertionError(boards);
  var lang=JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(args[0]))).getAsJsonObject();
  try(var input=java.nio.file.Files.newInputStream(java.nio.file.Path.of(args[1]))) {
   var nbt=net.minecraft.nbt.NbtIo.readCompressed(input,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
   var template=new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate();
   template.load(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),nbt);
   if(!template.getSize().equals(new net.minecraft.core.Vec3i(7,3,7))||nbt.getList("blocks",10).size()!=56)throw new AssertionError("Broken workshop structure");
  }
  int programs=0;
  for(var entry:boards.entrySet())for(var board:entry.getValue()) {
   captions=new ArrayList<>();ticks=created=removed=0;title=null;
   board.program((SceneBuilder)proxy(SceneBuilder.class),(SceneBuildingUtil)proxy(SceneBuildingUtil.class));
   if(title==null||ticks<550||created<1||created!=removed)throw new AssertionError(title+" "+ticks+" "+created+" "+removed);
   String prefix="tnkloot.ponder."+title+".";
   if(!lang.has(prefix+"header"))throw new AssertionError("Missing title: "+title);
   for(int i=0;i<captions.size();i++)if(!captions.get(i).equals(lang.get(prefix+"text_"+i).getAsString()))throw new AssertionError("Missing/out-of-order caption "+title+":"+i);
   programs++;
  }
  if(programs!=57)throw new AssertionError(programs);
  System.out.println("PONDER_STORYBOARD_OK: 19 subjects, 57 real storyboard executions, 18 translated captions, timed keyframes and paired demonstration item cleanup");
 }
}
