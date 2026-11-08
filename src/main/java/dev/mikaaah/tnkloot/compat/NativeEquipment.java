package dev.mikaaah.tnkloot.compat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class NativeEquipment {
   private NativeEquipment() {
   }

   private static Object call(Object target, String method) throws ReflectiveOperationException {
      return target.getClass().getMethod(method).invoke(target);
   }

   public static ItemStack prepare(ItemStack stack, int tier, Player player, long seed) {
      if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("irons_jewelry")) {
         return stack;
      } else {
         try {
            Class<?> data = Class.forName("io.redspace.ironsjewelry.core.data.JewelryData");
            if ((Boolean)data.getMethod("has", ItemStack.class).invoke(null, stack)) {
               return stack;
            } else {
               Class<?> registries = Class.forName("io.redspace.ironsjewelry.registry.IronsJewelryRegistries");
               Registry<?> patterns = (Registry<?>)registries.getMethod("patternRegistry", RegistryAccess.class).invoke(null, player.registryAccess());
               Registry<?> materials = (Registry<?>)registries.getMethod("materialRegistry", RegistryAccess.class).invoke(null, player.registryAccess());
               List<Holder<?>> candidates = new ArrayList<>();

               for (Reference<?> holder : patterns.holders().toList()) {
                  Object type = call(holder.value(), "jewelryType");
                  if (((Holder)call(type, "item")).value() == stack.getItem()) {
                     candidates.add(holder);
                  }
               }

               candidates.sort(Comparator.comparingDouble(h -> number(h.value(), "qualityMultiplier")));
               Random random = new Random(seed);
               int low = (int)Math.floor((double)Math.min(7, tier) / 7.0 * (double)Math.max(0, candidates.size() - 1));
               if (candidates.isEmpty()) {
                  throw new IllegalStateException("No jewelry patterns");
               } else {
                  Holder<?> pattern = candidates.get(low + random.nextInt(Math.min(2, candidates.size() - low)));
                  Map<Holder<?>, Holder<?>> parts = new HashMap<>();

                  for (Object ingredient : (List)call(pattern.value(), "partTemplate")) {
                     Holder<?> part = (Holder<?>)call(ingredient, "part");
                     List<Holder<?>> eligible = new ArrayList<>();

                     for (Reference<?> material : materials.holders().toList()) {
                        if ((Boolean)part.value().getClass().getMethod("canUseMaterial", Holder.class).invoke(part.value(), material)) {
                           eligible.add(material);
                        }
                     }

                     eligible.sort(Comparator.comparingDouble(h -> number(h.value(), "quality")));
                     if (eligible.isEmpty()) {
                        throw new IllegalStateException("No material for jewelry part");
                     }

                     int start = (int)Math.floor((double)Math.min(7, tier) / 7.0 * (double)(eligible.size() - 1));
                     parts.put(part, eligible.get(start + random.nextInt(Math.min(2, eligible.size() - start))));
                  }

                  Object jewelry = data.getConstructor(Holder.class, Map.class).newInstance(pattern, parts);
                  if (!(Boolean)data.getMethod("isValid").invoke(jewelry)) {
                     throw new IllegalStateException("Invalid generated jewelry");
                  } else {
                     data.getMethod("set", ItemStack.class, data).invoke(null, stack, jewelry);
                     return stack;
                  }
               }
            }
         } catch (ReflectiveOperationException var20) {
            throw new IllegalStateException("Iron's Jewelry API could not generate this reward", var20);
         }
      }
   }

   private static double number(Object value, String method) {
      try {
         return ((Number)call(value, method)).doubleValue();
      } catch (ReflectiveOperationException var3) {
         throw new IllegalStateException(var3);
      }
   }
}
