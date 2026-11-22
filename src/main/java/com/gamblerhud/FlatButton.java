package com.gamblerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** A flat dark button that matches the menu style, instead of the vanilla grey button. */
public class FlatButton extends AbstractWidget {
    private final Supplier<String> label;
    private final Runnable action;
    public boolean highlight;

    public FlatButton(int x, int y, int w, int h, Supplier<String> label, Runnable action) {
        super(x, y, w, h, Component.literal(""));
        this.label = label;
        this.action = action;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
        boolean hover = isMouseOver(mx, my);
        int border = highlight ? Config.get().accent : (hover ? 0xFF4B5563 : 0xFF2A3038);
        int bg = hover ? 0xFF262C36 : 0xFF181C23;
        Draw.rect(g, getX(), getY(), width, height, border);
        Draw.rect(g, getX() + 1, getY() + 1, width - 2, height - 2, bg);

        String s = label.get();
        Minecraft mc = Minecraft.getInstance();
        int tw = mc.font.width(s);
        int color = highlight ? 0xFFFFFFFF : 0xFFCBD5E1;
        g.drawString(mc.font, s, getX() + (width - tw) / 2, getY() + (height - 8) / 2 + 1, color, false);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
