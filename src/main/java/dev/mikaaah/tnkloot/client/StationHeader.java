package dev.mikaaah.tnkloot.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** One title plate, cap line and color. Screen-specific Y/color overrides are not accepted. */
public final class StationHeader {
    public static final int PLATE_Y = -7, PLATE_HEIGHT = 19;
    public static final int CAP_HEIGHT = 6;
    public static final int TEXT_Y = PLATE_Y + (PLATE_HEIGHT - CAP_HEIGHT) / 2;
    public static final int TEXT_COLOR = 0xFFF4EBE5;
    public static final int CANVAS_HEIGHT = 260;
    public static final int NAV_Y = 18, NAV_HEIGHT = 16;
    private static final ResourceLocation FONT = ResourceLocation.parse("tnkloot:menu");

    /** Keep station headers stationary when switching between menus of different heights. */
    public static int top(int screenHeight) {
        return Math.max(16, (screenHeight - CANVAS_HEIGHT) / 2);
    }

    public static void draw(GuiGraphics g, Font font, String text, int plateX, int plateWidth) {
        draw(g, font, Component.literal(text), plateX, plateWidth);
    }

    public static void draw(GuiGraphics g, Font font, Component text, int plateX, int plateWidth) {
        Component label = text.copy().withStyle(style -> style.withFont(FONT)
            .withBold(false).withItalic(false).withColor(TEXT_COLOR & 0xFFFFFF));
        int x = plateX + (plateWidth - font.width(label)) / 2 + 1;
        g.drawString(font, label, x, TEXT_Y, TEXT_COLOR, false);
    }

    private StationHeader() {}
}
