package dev.mikaaah.tnkloot.apotheosis;
import net.minecraft.world.entity.player.Player;
/** Original player attribution carried through Apotheosis generation contexts. */
public interface ContextOwner {
    Player tnkloot$player();
    void tnkloot$player(Player player);
    int tnkloot$sourceLevel();
    void tnkloot$sourceLevel(int level);
}
