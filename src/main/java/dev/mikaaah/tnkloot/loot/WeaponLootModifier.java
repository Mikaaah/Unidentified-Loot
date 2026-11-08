package dev.mikaaah.tnkloot.loot;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.*;
/** Retains the existing datapack serializer; replacement now runs after every GLM. */
public final class WeaponLootModifier extends LootModifier {
 public static final MapCodec<WeaponLootModifier> CODEC=RecordCodecBuilder.mapCodec(inst->codecStart(inst).apply(inst,WeaponLootModifier::new));
 public WeaponLootModifier(LootItemCondition[] conditions){super(conditions);}
 @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext ctx){return loot;}
 @Override public MapCodec<? extends IGlobalLootModifier> codec(){return CODEC;}
}
