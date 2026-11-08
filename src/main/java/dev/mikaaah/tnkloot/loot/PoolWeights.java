package dev.mikaaah.tnkloot.loot;

import java.util.List;

public final class PoolWeights {
   private PoolWeights() {
   }

   public static int[] setPercent(List<Integer> weights, int selected, int basisPoints) {
      if (selected >= 0 && selected < weights.size() && basisPoints > 0 && basisPoints < 10000 && weights.size() >= 2) {
         int[] result = new int[weights.size()];
         int target = basisPoints * 100;
         int remaining = 1000000 - target;
         if (remaining < weights.size() - 1) {
            throw new IllegalArgumentException("Too many items for that percentage.");
         } else {
            long total = 0L;

            for (int i = 0; i < weights.size(); i++) {
               if (i != selected) {
                  total += (long)weights.get(i).intValue();
               }
            }

            if (total <= 0L) {
               throw new IllegalArgumentException("No other weighted items.");
            } else {
               result[selected] = target;
               int assigned = 0;
               int distributable = remaining - (weights.size() - 1);

               for (int ix = 0; ix < weights.size(); ix++) {
                  if (ix != selected) {
                     result[ix] = 1 + (int)((long)distributable * (long)weights.get(ix).intValue() / total);
                     assigned += result[ix];
                  }
               }

               for (int ixx = 0; assigned < remaining; ixx = (ixx + 1) % weights.size()) {
                  if (ixx != selected) {
                     result[ixx]++;
                     assigned++;
                  }
               }

               return result;
            }
         }
      } else {
         throw new IllegalArgumentException("A partial chance needs another enabled item in this category.");
      }
   }
}
