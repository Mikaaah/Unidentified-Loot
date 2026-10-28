package dev.mikaaah.tnkloot.loot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LevelTierRollTest {
 @Test void everyBoundaryHasExactWeights(){for(int level=1;level<=100;level++){int[] counts=new int[8];for(int r=0;r<100;r++)counts[LevelTierRoll.roll(level,r)]++;int first=level<=30?0:level<=50?1:level<=70?2:level<=80?3:level<=94?4:5;if(level<=10){assertEquals(75,counts[0]);assertEquals(25,counts[1]);}else{assertEquals(level>=95?60:50,counts[first]);assertEquals(35,counts[first+1]);assertEquals(level>=95?5:15,counts[first+2]);}}}
}
