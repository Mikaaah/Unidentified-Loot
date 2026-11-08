package dev.mikaaah.tnkloot.client;

import dev.mikaaah.tnkloot.loot.IdentifierEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public final class IdentifierRenderer extends MobRenderer<IdentifierEntity, VillagerModel<IdentifierEntity>> {
   private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("tnkloot", "textures/entity/item_identifier.png");

   public IdentifierRenderer(Context context) {
      super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
   }

   public ResourceLocation getTextureLocation(IdentifierEntity entity) {
      return SKIN;
   }
}
