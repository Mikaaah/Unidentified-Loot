package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.dungeon.DungeonData;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DungeonOddsTest {
    @Test void weightedSelectionMatchesDisplayedOddsAndNeverChoosesZeroWeight() {
        Map<String,Integer> weights = new LinkedHashMap<>(); weights.put("blade",50); weights.put("crown",30); weights.put("bow",20); weights.put("disabled",0);
        Map<String,Integer> counts = new HashMap<>(); Random random = new Random(220113L);
        for(int i=0;i<40000;i++)counts.merge(DungeonData.weighted(weights,random),1,Integer::sum);
        assertFalse(counts.containsKey("disabled"));
        assertEquals(.5,counts.get("blade")/40000.0,.01);
        assertEquals(.3,counts.get("crown")/40000.0,.01);
        assertEquals(.2,counts.get("bow")/40000.0,.01);
    }
    @Test void partyRareChanceCountsClaimantsAndNotPrizeCandidates() {
        var perPlayer = new DungeonData.Rare(Optional.of(.1),true,Optional.empty(),null);
        var shared = new DungeonData.Rare(Optional.of(.1),false,Optional.empty(),null);
        assertEquals(0,perPlayer.partyChance(.1,0));
        assertEquals(.1,perPlayer.partyChance(.1,1),1e-12);
        assertEquals(.19,perPlayer.partyChance(.1,2),1e-12);
        assertEquals(.271,perPlayer.partyChance(.1,3),1e-12);
        assertEquals(.1,shared.partyChance(.1,12),1e-12);
    }
    @Test void completeAndDisabledRareProbabilitiesStayBounded() {
        var policy = new DungeonData.Rare(Optional.empty(),true,Optional.empty(),null);
        for(int n=0;n<50;n++) {
            assertEquals(0,policy.partyChance(0,n));
            assertEquals(n==0?0:1,policy.partyChance(1,n));
            assertTrue(policy.partyChance(.37,n)>=0 && policy.partyChance(.37,n)<=1);
        }
    }
    @Test void emptyWeightedPoolFailsInsteadOfReturningAnUnrelatedFallback() {
        assertThrows(IllegalArgumentException.class,()->DungeonData.weighted(Map.of("disabled",0),new Random(1)));
    }
}
