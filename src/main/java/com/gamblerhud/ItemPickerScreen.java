package com.gamblerhud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search every item in the game and click its picture to choose it for the auction. */
public class ItemPickerScreen extends Screen {
    private static final int CELL = 24;
    private static final int COLS = 14;

    private final Screen parent;
    private final List<Item> results = new ArrayList<>();
    private int scroll = 0;
    private int gx, gy, rows;

    public ItemPickerScreen(Screen parent) {
        super(Component.literal("Choose item"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        EditBox search = new EditBox(font, width / 2 - 150, 36, 300, 18, Component.literal("Search"));
        search.setResponder(s -> {
            scroll = 0;
            filter(s);
        });
        addRenderableWidget(search);
        gx = width / 2 - COLS * CELL / 2;
        gy = 66;
        rows = Math.max(1, (height - 110) / CELL);
        addRenderableWidget(new FlatButton(10, height - 26, 80, 18, () -> "Back", () -> minecraft.setScreen(parent)));
        filter(search.getValue());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void filter(String q) {
        results.clear();
        String lq = q.trim().toLowerCase(Locale.ROOT);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            String name = new ItemStack(item).getHoverName().getString();
            String id = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (lq.isEmpty() || name.toLowerCase(Locale.ROOT).contains(lq) || id.contains(lq.replace(' ', '_'))) {
                results.add(item);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        g.drawString(font, "Choose an item", width / 2 - 150, 22, 0xFFFFFFFF, true);

        int hovered = -1;
        for (int i = 0; i < rows * COLS; i++) {
            int idx = scroll * COLS + i;
            if (idx >= results.size()) break;
            int cx = gx + (i % COLS) * CELL;
            int cy = gy + (i / COLS) * CELL;
            boolean hot = mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL;
            if (hot) {
                hovered = idx;
                Draw.rect(g, cx, cy, CELL - 2, CELL - 2, 0xFF2A3038);
            }
            g.renderItem(new ItemStack(results.get(idx)), cx + 4, cy + 4);
        }

        String hint = hovered >= 0
                ? new ItemStack(results.get(hovered)).getHoverName().getString() + "  (click to pick)"
                : results.size() + " results, scroll for more";
        Draw.str(g, hint, width / 2 - Draw.width(hint) / 2, height - 40, 0xFF9CA3AF, false);
        Draw.str(g, "Made by Onxon", width / 2 - Draw.width("Made by Onxon") / 2, height - 12, 0xFF6B7280, false);
    }

    private int cellAt(double mx, double my) {
        if (mx < gx || my < gy) return -1;
        int col = (int) ((mx - gx) / CELL);
        int row = (int) ((my - gy) / CELL);
        if (col < 0 || col >= COLS || row < 0 || row >= rows) return -1;
        int idx = scroll * COLS + row * COLS + col;
        return idx < results.size() ? idx : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int i = cellAt(event.x(), event.y());
        if (i < 0) return false;
        Item item = results.get(i);
        Auction.draftItemId = BuiltInRegistries.ITEM.getKey(item).toString();
        Auction.draftItemName = new ItemStack(item).getHoverName().getString();
        minecraft.setScreen(parent);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, (results.size() + COLS - 1) / COLS - rows);
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - Math.signum(scrollY)));
        return true;
    }
}
