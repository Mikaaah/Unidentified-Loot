package dev.mikaaah.tnkloot.loot;

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

public final class WeightedPool<T> {
   private final List<WeightedPool.Entry<T>> entries;
   private final long total;

   public WeightedPool(List<WeightedPool.Entry<T>> entries) {
      this.entries = List.copyOf(entries);
      long sum = 0L;

      for (WeightedPool.Entry<T> entry : entries) {
         sum = Math.addExact(sum, (long)entry.weight());
      }

      this.total = sum;
   }

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   public Optional<T> roll(LongSupplier random) {
      if (this.isEmpty()) {
         return Optional.empty();
      } else {
         long bits;
         long ticket;
         do {
            bits = random.getAsLong() >>> 1;
            ticket = bits % this.total;
         } while (bits - ticket + (this.total - 1L) < 0L);

         return Optional.of(this.at(ticket));
      }
   }

   T at(long ticket) {
      if (ticket >= 0L && ticket < this.total) {
         for (WeightedPool.Entry<T> entry : this.entries) {
            if (ticket < (long)entry.weight()) {
               return entry.value();
            }

            ticket -= (long)entry.weight();
         }

         throw new IllegalStateException("Weighted pool exhausted");
      } else {
         throw new IllegalArgumentException("Ticket outside pool");
      }
   }

   public static record Entry<T>(T value, int weight) {
      public Entry(T value, int weight) {
         if (weight < 1) {
            throw new IllegalArgumentException("Weight must be positive");
         } else {
            this.value = value;
            this.weight = weight;
         }
      }
   }
}
