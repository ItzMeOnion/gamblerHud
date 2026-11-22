package com.gamblerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Small drawing helpers shared by the HUD and the menus. */
public final class Draw {
    private Draw() {
    }

    public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + Math.round((float) dx * i / steps);
            int y = y1 + Math.round((float) dy * i / steps);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    public static void str(GuiGraphics g, String s, int x, int y, int color, boolean shadow) {
        g.drawString(Minecraft.getInstance().font, s, x, y, color, shadow);
    }

    public static int width(String s) {
        return Minecraft.getInstance().font.width(s);
    }
}
