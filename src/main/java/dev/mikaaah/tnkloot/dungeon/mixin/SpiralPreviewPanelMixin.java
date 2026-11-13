package dev.mikaaah.tnkloot.dungeon.mixin;
import dev.mikaaah.tnkloot.dungeon.*;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.pla.spiral_abyss.client.screen.RewardPreviewPanel", remap = false)
public abstract class SpiralPreviewPanelMixin {
    @Shadow @Final private List<?> rewards;
    @Shadow @Final private int x, y, height, tileSize, cellSize;
    @Shadow private int scroll;
    @Shadow private int columns() { throw new AssertionError(); }
    @Shadow private void percentage(GuiGraphics graphics, double chance, int x, int y) { throw new AssertionError(); }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", remap = true))
    private void tnkloot$tooltipMargin(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY) {
        if (!DungeonEquipment.preview(stack)) { graphics.renderTooltip(font, stack, mouseX, mouseY); return; }
        // Vanilla clamps the text to the bottom edge without allowing for its frame.
        graphics.pose().pushPose(); graphics.pose().translate(0, -6, 0);
        graphics.renderTooltip(font, stack, mouseX, mouseY + 6);
        graphics.pose().popPose();
    }
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;disableScissor()V", remap = true))
    private void tnkloot$normalItemChances(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight, CallbackInfo callback) {
        for (int i = 0; i < rewards.size(); i++) {
            Object reward = rewards.get(i); var icon = (ItemStack)SpiralCompat.get(reward, "icon");
            if (!DungeonEquipment.preview(icon) || (boolean)SpiralCompat.get(reward, "rare")) continue;
            int sx = x + i % columns() * cellSize, sy = y + i / columns() * cellSize - scroll;
            if (sy + tileSize <= y || sy >= y + height) continue;
            percentage(graphics, (double)SpiralCompat.get(reward, "displayChance"), sx, sy);
        }
    }
}
