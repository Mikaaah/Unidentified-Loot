package dev.mikaaah.tnkloot.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Iterator;

public final class PresetCleanup {
   private PresetCleanup() {
   }

   /** Modular jewellery must be supplied by its own pattern/material generator. */
   public static boolean requiresNativePayload(String id) {
      return id.equals("irons_jewelry:ring") || id.equals("irons_jewelry:necklace");
   }

   public static boolean excluded(String var0) {
      if (!PackItemPolicy.allowed(var0) || PackEquipmentRules.nativeOnly(var0) || "wom:claw".equals(var0) || var0.startsWith("efn:") || requiresNativePayload(var0)) {
         return true;
      } else {
         String var1 = var0.substring(var0.indexOf(58) + 1);

         String[] var5;
         for (String var2 : var5 = var1.split("/")) {
            if (var2.equals("gold") || var2.equals("golden") || var2.startsWith("gold_") || var2.startsWith("golden_")) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean sanitize(JsonObject var0) {
      JsonArray var1 = var0.getAsJsonArray("weapons");
      if (var1 == null) {
         return false;
      } else {
         boolean var2 = false;
         Iterator var3 = var1.iterator();

         while (var3.hasNext()) {
            JsonObject var4 = ((JsonElement)var3.next()).getAsJsonObject();
            if (excluded(var4.get("item").getAsString())) {
               var3.remove();
               var2 = true;
            }
         }

         return var2;
      }
   }
}
