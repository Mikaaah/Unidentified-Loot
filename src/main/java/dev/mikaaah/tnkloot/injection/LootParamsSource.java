package dev.mikaaah.tnkloot.injection;

/** Origin metadata retained when a loot table builds its evaluation context. */
public interface LootParamsSource {
    boolean tnkloot$isAdvancementReward();
    void tnkloot$setAdvancementReward(boolean value);
}
