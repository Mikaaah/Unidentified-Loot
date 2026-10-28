package dev.mikaaah.tnkloot.loot;
public final class LevelTierRoll {
 public static int roll(int level,int percentile){
  if(level<1||level>100||percentile<0||percentile>99)throw new IllegalArgumentException();
  if(level<=10)return percentile<75?0:1;
  int first=level<=30?0:level<=50?1:level<=70?2:level<=80?3:level<=94?4:5;
  int low=level>=95?60:50,mid=level>=95?95:85;
  return first+(percentile<low?0:percentile<mid?1:2);
 }
}
