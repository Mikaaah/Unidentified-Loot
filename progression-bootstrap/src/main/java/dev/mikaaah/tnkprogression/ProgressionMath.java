package dev.mikaaah.tnkprogression;
/** Pure balance rules. All thresholds use additional work, not character levels. */
public final class ProgressionMath {
 private ProgressionMath() {}
 public static long levelCost(int level) { return 100L + 40L * level * level; }
 public static long taskCost(long base, long completed) {
  // Saturation protects persisted counters without imposing an activity-rank limit.
  double cost = base * (1.0 + completed * 0.65 + completed * (double) completed * 0.08);
  return cost >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1, (long) cost);
 }
 public static long taskReward(long base, long completed) {
  return Math.max(1, (long) (base * Math.sqrt(completed + 1.0)));
 }
 public static int rollTier(int level, int roll) {
  if (level < 1 || level > 100 || roll < 0 || roll >= 100) throw new IllegalArgumentException();
  if (level <= 10) return roll < 75 ? 0 : 1;
  int first = level <= 30 ? 0 : level <= 50 ? 1 : level <= 70 ? 2 : level <= 80 ? 3 : level <= 94 ? 4 : 5;
  return first + (roll < (level >= 95 ? 60 : 50) ? 0 : roll < 95 - (level >= 95 ? 0 : 10) ? 1 : 2);
 }
}
