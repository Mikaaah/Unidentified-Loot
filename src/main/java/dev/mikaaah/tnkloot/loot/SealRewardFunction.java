package dev.mikaaah.tnkloot.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.dimension.DimensionLoot;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Explicit, server-authored rewards can supply strength without borrowing a player. */
public final class SealRewardFunction extends LootItemConditionalFunction {
    private static final Codec<String> RARITY = Codec.STRING.comapFlatMap(value -> {
        String id = value.startsWith("tnkloot:") ? value.substring(8) : value;
        return List.of(TnkLootRolls.RARITIES).contains(id)
            ? DataResult.success(id)
            : DataResult.error(() -> "Unknown TNK reward rarity: " + value);
    }, value -> value);
    public static final MapCodec<SealRewardFunction> CODEC = RecordCodecBuilder.mapCodec(builder ->
        commonFields(builder).and(builder.group(
            Codec.intRange(0, 7).fieldOf("tier").forGetter(value -> value.tier),
            Codec.intRange(1, 100).fieldOf("source_level").forGetter(value -> value.sourceLevel),
            RARITY.fieldOf("rarity").forGetter(value -> value.rarity)
        )).apply(builder, SealRewardFunction::new));

    private final int tier;
    private final int sourceLevel;
    private final String rarity;

    private SealRewardFunction(List<LootItemCondition> conditions, int tier, int sourceLevel, String rarity) {
        super(conditions);
        this.tier = tier;
        this.sourceLevel = sourceLevel;
        this.rarity = rarity;
    }

    @Override
    public LootItemFunctionType<SealRewardFunction> getType() {
        return TnkLoot.SEAL_REWARD.get();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (!(stack.getItem() instanceof UnidentifiedWeaponItem item) || item.boss()) return stack;
        var fresh = UnidentifiedWeaponItem.create(item.category(), tier, "", "datapack_reward");
        var tag = UnidentifiedWeaponItem.data(stack);
        tag.merge(UnidentifiedWeaponItem.data(fresh));
        tag.remove("creative_template");
        tag.remove("source_item");
        tag.putString("origin_dimension", context.getLevel().dimension().location().toString());
        tag.putInt("tnk_source_level", sourceLevel);
        tag.putBoolean("tnk_progression_sealed", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        DimensionLoot.bindOrigin(stack, context.getLevel());
        RaritySupport.force(stack, rarity);
        return stack;
    }
}
