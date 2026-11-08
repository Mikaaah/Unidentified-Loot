package dev.mikaaah.tnkloot;

import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.features.FeatureItems;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.machine.MechanicalIdentifier;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TnkCreativeTabs {
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "tnkloot");
   public static final Supplier<CreativeModeTab> MAIN = TABS.register(
      "main",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tnkloot"))
            .icon(() -> new ItemStack((ItemLike)TnkLoot.UNIDENTIFIED_WEAPON.get()))
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)TnkLoot.IDENTIFIER_ITEM.get());
               if (MechanicalIdentifier.ENABLED) {
                  output.accept((ItemLike)MechanicalIdentifier.ITEM.get());
               }

               output.accept((ItemLike)FeatureItems.JOURNAL.get());

               for (TnkConfig.Category category : TnkConfig.Category.values()) {
                  output.accept(UnidentifiedWeaponItem.creativeSample(category, 0));
               }

               for (BossRewards.Pool pool : BossRewards.active()) {
                  TnkConfig.Category category = TnkConfig.Category.WEAPON;
                  if (!BossRewards.options(pool, category).isEmpty()) {
                     ItemStack sample = BossRewards.token(pool, category, null, ItemStack.EMPTY);
                     CompoundTag tag = UnidentifiedWeaponItem.data(sample);
                     tag.putBoolean("creative_template", true);
                     tag.putBoolean("seed_bound", false);
                     tag.remove("roll_seed");
                     sample.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                     output.accept(RaritySupport.template(sample));
                  }
               }
            })
            .build()
   );

   private TnkCreativeTabs() {
   }
}
