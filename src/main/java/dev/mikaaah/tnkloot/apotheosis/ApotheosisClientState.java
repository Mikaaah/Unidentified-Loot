package dev.mikaaah.tnkloot.apotheosis;
/** Server-authoritative values used by native reforging previews on the client. */
public final class ApotheosisClientState {
    private ApotheosisClientState() {}
    public static volatile int level=1,ceiling=10,rarityCap=1;
    public static void update(int newLevel,int newCeiling,int newCap,boolean pinnacle) {
        level=Math.clamp(newLevel,1,100);ceiling=Math.clamp(newCeiling,1,100);rarityCap=Math.clamp(newCap,0,5);
        ApotheosisIntegration.clientPolicy(pinnacle);
    }
}
