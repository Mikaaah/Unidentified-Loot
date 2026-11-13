package dev.mikaaah.tnkloot.dungeon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.mikaaah.tnkloot.TnkLoot;
import java.util.List;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Thin vanilla loot-table entry point; content is edited in one dungeon reward file. */
public final class DungeonRewardFunction extends LootItemConditionalFunction {
    public static final MapCodec<DungeonRewardFunction> CODEC = RecordCodecBuilder.mapCodec(builder ->
        commonFields(builder).and(ResourceLocation.CODEC.fieldOf("table").forGetter(value -> value.table)).apply(builder, DungeonRewardFunction::new));
    private final ResourceLocation table;
    private DungeonRewardFunction(List<LootItemCondition> conditions, ResourceLocation table) { super(conditions); this.table = table; }
    @Override public LootItemFunctionType<DungeonRewardFunction> getType() { return TnkLoot.DUNGEON_REWARD.get(); }
    @Override public ItemStack run(ItemStack ignored, LootContext context) {
        var rewards = DungeonData.table(table).orElseThrow(() -> new IllegalArgumentException("Missing dungeon reward table " + table));
        Random random = new Random(context.getRandom().nextLong());
        return DungeonEquipment.reward(DungeonData.choose(rewards.availableNormal(), random), context.getLevel(), random);
    }
}
