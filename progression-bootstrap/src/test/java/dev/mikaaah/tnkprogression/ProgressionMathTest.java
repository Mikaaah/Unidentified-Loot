package dev.mikaaah.tnkprogression;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProgressionMathTest {
 @Test void exactDistributions(){int[][] expected={{75,25},{50,35,15},{50,35,15},{50,35,15},{50,35,15},{50,35,15},{60,35,5}};int[] levels={1,11,31,51,71,81,95};for(int b=0;b<levels.length;b++){int[] counts=new int[8];for(int r=0;r<100;r++)counts[ProgressionMath.rollTier(levels[b],r)]++;int first=b<2?0:b-1;for(int i=0;i<expected[b].length;i++)assertEquals(expected[b][i],counts[first+i]);}}
 @Test void boundaries(){for(int l=1;l<=100;l++){for(int r=0;r<100;r++)assertTrue(ProgressionMath.rollTier(l,r)>=0&&ProgressionMath.rollTier(l,r)<=7);}}
 @Test void progressiveCosts(){for(int i=1;i<99;i++)assertTrue(ProgressionMath.levelCost(i+1)>ProgressionMath.levelCost(i));for(int r=0;r<10000;r++)assertTrue(ProgressionMath.taskCost(100,r+1)>ProgressionMath.taskCost(100,r));assertTrue(ProgressionMath.taskCost(100,1000000)>1000000);}
}
