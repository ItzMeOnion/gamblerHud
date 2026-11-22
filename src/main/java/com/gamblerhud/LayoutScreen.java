package com.gamblerhud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Drag panels to move them, drag the corner handle to resize them. */
public class LayoutScreen extends Screen {
    private final Screen parent;
    private Panel dragging;
    private boolean resizing;
    private int offX, offY;

    public LayoutScreen(Screen parent) {
        super(Component.literal("Layout"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        addRenderableWidget(new FlatButton(width / 2 - 60, height - 30, 120, 20,
                () -> "Done", () -> minecraft.setScreen(parent)));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        g.fill(0, 0, width, height, 0x66000000);
        Hud.render(g);   // panels that are placed already draw themselves, layout overlays below
        List<Panel> panels = Config.get().panels;
        for (Panel p : panels) {
            if (!p.visible) continue;
            int c = p == dragging ? 0xFF3B82F6 : 0xAA3B82F6;
            Draw.outline(g, p.x - 1, p.y - 1, p.w + 2, p.h + 2, c);
            if (!p.type.equals(Panel.TEXT)) {
                Draw.rect(g, p.x + p.w - 6, p.y + p.h - 6, 6, 6, c);
            }
        }
        Draw.str(g, "LAYOUT  -  drag to move, drag the corner to resize, Esc to finish", 8, 8, 0xFFFFFFFF, true);
    }

    private Panel panelAt(double mx, double my) {
        List<Panel> panels = Config.get().panels;
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.visible && mx >= p.x && mx < p.x + p.w && my >= p.y && my < p.y + p.h) return p;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        Panel p = panelAt(event.x(), event.y());
        if (p == null) return false;
        dragging = p;
        resizing = !p.type.equals(Panel.TEXT)
                && event.x() >= p.x + p.w - 8 && event.y() >= p.y + p.h - 8;
        offX = (int) event.x() - p.x;
        offY = (int) event.y() - p.y;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging == null) return super.mouseDragged(event, dragX, dragY);
        if (resizing) {
            dragging.w = Math.max(30, (int) event.x() - dragging.x);
            dragging.h = Math.max(12, (int) event.y() - dragging.y);
        } else {
            dragging.x = Math.max(0, Math.min(width - dragging.w, (int) event.x() - offX));
            dragging.y = Math.max(0, Math.min(height - dragging.h, (int) event.y() - offY));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            Config.save();
            return true;
        }
        return super.mouseReleased(event);
    }
}
