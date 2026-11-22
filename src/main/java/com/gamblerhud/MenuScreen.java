package com.gamblerhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** The GamblerHud menu: UI, PROFIT, AUCTIONS and SESSION tabs. */
public class MenuScreen extends Screen {
    private static final String[] TABS = {"UI", "PROFIT", "AUCTIONS", "SESSION"};
    private static final int[] COLOURS = {
            0xFFFFFFFF, 0xFF4ADE80, 0xFFF87171, 0xFFFBBF24, 0xFF38BDF8,
            0xFF3B82F6, 0xFFA855F7, 0xFFF472B6, 0xFF9CA3AF, 0xFF000000};
    private static final String[] COLOUR_NAMES = {
            "White", "Green", "Red", "Gold", "Sky", "Blue", "Purple", "Pink", "Grey", "Black"};
    private static final int[] BGS = {0xF2101418, 0xF2000000, 0xF21E2A44, 0xF2261A38, 0xF2301818, 0xF2181C23};
    private static final String[] BG_NAMES = {"Dark", "Black", "Navy", "Plum", "Maroon", "Slate"};
    private static final String[] FONTS = {"minecraft:default", "minecraft:uniform", "minecraft:alt"};
    private static final String[] FONT_NAMES = {"Default", "Uniform", "Alt"};
    private static final String[] ANCHORS = {"TL", "TC", "TR", "ML", "MC", "MR", "BL", "BC", "BR"};
    private static final int PW = 520, PH = 340;
    private static final int GREEN = 0xFF4ADE80, RED = 0xFFF87171, GREY = 0xFF9CA3AF, DIM = 0xFF6B7280;
    private static final int WHITE = 0xFFFFFFFF, ORANGE = 0xFFFB923C;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM HH:mm").withZone(ZoneId.systemDefault());

    private static int tab = 0;
    private static int selectedPanel = -1;
    private static int selectedHistory = -1;
    private static boolean capturing = false;
    private static String status = "";

    private int px, py;

    public MenuScreen() {
        super(Component.literal("GamblerHud"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** True while a text box has focus, so typing the menu key does not close the menu. */
    public boolean isTyping() {
        return getFocused() instanceof EditBox;
    }

    private static int idx(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return 0;
    }

    private static int idx(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return 0;
    }

    private static String keyName() {
        return InputConstants.getKey(Config.get().keyCode).getDisplayName().getString();
    }

    private static void toggleAll() {
        Config.get().allTime = !Config.get().allTime;
        Config.save();
    }

    private Panel selected() {
        List<Panel> ps = Config.get().panels;
        return selectedPanel >= 0 && selectedPanel < ps.size() ? ps.get(selectedPanel) : null;
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            FlatButton b = new FlatButton(px + 10 + i * 125, py + 34, 120, 18, () -> TABS[t], () -> {
                tab = t;
                selectedHistory = -1;
                rebuild();
            });
            b.highlight = tab == t;
            addRenderableWidget(b);
        }
        switch (tab) {
            case 0 -> initUi();
            case 1 -> initProfit();
            case 2 -> initAuction();
            default -> initSession();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, 0x88000000);
        Draw.rect(g, px, py, PW, PH, Config.get().menuBg);
        Draw.outline(g, px, py, PW, PH, 0xFF2A3038);
    }

    // ---------------- UI tab ----------------

    private void initUi() {
        Config cfg = Config.get();
        int lx = px + 10, rx = px + 170, top = py + 62;

        for (int i = 0; i < Math.min(cfg.panels.size(), 12); i++) {
            final int sel = i;
            Panel p = cfg.panels.get(i);
            FlatButton b = new FlatButton(lx, top + i * 17, 150, 16,
                    () -> (p.visible ? "" : "[hidden] ") + p.label(), () -> {
                selectedPanel = sel;
                rebuild();
            });
            b.highlight = sel == selectedPanel;
            addRenderableWidget(b);
        }
        addRenderableWidget(new FlatButton(lx, top + 12 * 17 + 2, 150, 16, () -> "+ Text box",
                () -> addPanel(Panel.TEXT)));
        addRenderableWidget(new FlatButton(lx, py + PH - 40, 150, 18, () -> "Layout",
                () -> minecraft.setScreen(new LayoutScreen(this))));

        addRenderableWidget(new FlatButton(rx, top, 340, 18, () -> capturing
                ? "Press a key...  (Esc cancels)"
                : "Menu key: " + keyName(), () -> {
            capturing = true;
            rebuild();
        }));
        addRenderableWidget(new FlatButton(rx, top + 21, 340, 18,
                () -> "Menu colour: " + BG_NAMES[idx(BGS, cfg.menuBg)], () -> {
            cfg.menuBg = BGS[(idx(BGS, cfg.menuBg) + 1) % BGS.length];
            Config.save();
            rebuild();
        }));
        addRenderableWidget(new FlatButton(rx, top + 42, 340, 18,
                () -> "Accent colour: " + COLOUR_NAMES[idx(COLOURS, cfg.accent)], () -> {
            cfg.accent = COLOURS[(idx(COLOURS, cfg.accent) + 1) % COLOURS.length];
            Config.save();
            rebuild();
        }));

        Panel p = selected();
        if (p == null) return;
        final int sel = selectedPanel;
        int y = top + 70;

        if (p.type.equals(Panel.TEXT)) {
            EditBox box = new EditBox(font, rx, y, 340, 18, Component.literal("Text"));
            box.setMaxLength(120);
            box.setValue(p.text);
            box.setResponder(s -> {
                p.text = s;
                Config.save();
            });
            addRenderableWidget(box);
            y += 22;
        }

        addRenderableWidget(new FlatButton(rx, y, 340, 18,
                () -> "Font: " + FONT_NAMES[idx(FONTS, p.font)], () -> {
            p.font = FONTS[(idx(FONTS, p.font) + 1) % FONTS.length];
            Config.save();
            rebuild();
        }));
        y += 22;
        addRenderableWidget(new FlatButton(rx, y, 340, 18,
                () -> "Text colour: " + COLOUR_NAMES[idx(COLOURS, p.fg)], () -> {
            p.fg = COLOURS[(idx(COLOURS, p.fg) + 1) % COLOURS.length];
            Config.save();
            rebuild();
        }));
        y += 22;
        addRenderableWidget(new FlatButton(rx, y, 340, 18,
                () -> "Box colour: " + BG_NAMES[idx(BGS, p.bg)], () -> {
            p.bg = BGS[(idx(BGS, p.bg) + 1) % BGS.length];
            Config.save();
            rebuild();
        }));
        y += 22;
        addRenderableWidget(new FlatButton(rx, y, 340, 18,
                () -> "Snap to: " + p.anchor, () -> {
            p.anchor = ANCHORS[(idx(ANCHORS, p.anchor) + 1) % ANCHORS.length];
            p.anchorTo(width, height);
            Config.save();
            rebuild();
        }));
        y += 22;
        if (p.type.equals(Panel.TEXT)) {
            addRenderableWidget(new FlatButton(rx, y, 60, 18, () -> "-", () -> {
                p.scale = Math.max(0.5f, Math.round((p.scale - 0.1f) * 10) / 10f);
                Config.save();
            }));
            addRenderableWidget(new FlatButton(rx + 70, y, 200, 18,
                    () -> String.format("Size %.1fx  (click to reset)", p.scale), () -> {
                p.scale = 1f;
                Config.save();
            }));
            addRenderableWidget(new FlatButton(rx + 280, y, 60, 18, () -> "+", () -> {
                p.scale = Math.min(3f, Math.round((p.scale + 0.1f) * 10) / 10f);
                Config.save();
            }));
            y += 22;
        }
        addRenderableWidget(new FlatButton(rx, y, 166, 18,
                () -> "Shadow: " + (p.shadow ? "on" : "off"), () -> {
            p.shadow = !p.shadow;
            Config.save();
            rebuild();
        }));
        addRenderableWidget(new FlatButton(rx + 174, y, 166, 18,
                () -> "Visible: " + (p.visible ? "on" : "off"), () -> {
            p.visible = !p.visible;
            Config.save();
            rebuild();
        }));
        y += 22;
        addRenderableWidget(new FlatButton(rx, y, 340, 18, () -> "Delete this box", () -> {
            if (sel < cfg.panels.size()) cfg.panels.remove(sel);
            selectedPanel = -1;
            Config.save();
            rebuild();
        }));
    }

    private void addPanel(String type) {
        Config.get().panels.add(new Panel(type));
        Config.save();
        status = type.equals(Panel.TEXT) ? "Text box added. Edit it in the UI tab." : "Box added to your screen.";
        rebuild();
    }

    private void renderUi(GuiGraphics g) {
        int rx = px + 170, top = py + 62;
        if (selected() == null) {
            Draw.str(g, "Select a box on the left to edit it.", rx, top + 70, GREY, false);
        }
    }

    // ---------------- PROFIT tab ----------------

    private void initProfit() {
        Config cfg = Config.get();
        int x = px + 10, y = py + 62;
        addRenderableWidget(new FlatButton(x, y, 240, 18,
                () -> "Tracking: " + (cfg.tracking ? "ON" : "PAUSED"), () -> {
            cfg.tracking = !cfg.tracking;
            Config.save();
            rebuild();
        }));
        addRenderableWidget(new FlatButton(x, y + 21, 240, 18,
                () -> "Showing: " + (cfg.allTime ? "All time" : "This session"), () -> {
            toggleAll();
            rebuild();
        }));
        addRenderableWidget(new FlatButton(x, y + 60, 118, 18, () -> "+ Profit box", () -> addPanel(Panel.PROFIT)));
        addRenderableWidget(new FlatButton(x + 122, y + 60, 118, 18, () -> "+ Graph", () -> addPanel(Panel.GRAPH)));
        addRenderableWidget(new FlatButton(x, y + 81, 118, 18, () -> "+ Top payers", () -> addPanel(Panel.PAY_ME)));
        addRenderableWidget(new FlatButton(x + 122, y + 81, 118, 18, () -> "+ Top paid to", () -> addPanel(Panel.PAID_OUT)));
        addRenderableWidget(new FlatButton(x, y + 102, 240, 18, () -> "+ Text box", () -> addPanel(Panel.TEXT)));
    }

    private void renderProfit(GuiGraphics g) {
        int x = px + 10, y = py + 62;
        Draw.str(g, "ADD TO SCREEN", x, y + 48, GREY, false);
        Draw.str(g, "RECENT PAYMENTS", x, y + 130, GREY, false);
        List<Tracker.Payment> list = Tracker.payments(false);
        int ly = y + 142;
        for (int i = list.size() - 1; i >= 0 && i >= list.size() - 6; i--) {
            Tracker.Payment p = list.get(i);
            String line = (p.incoming ? "<- " : "-> ") + p.player + "  " + Tracker.money(p.cents, false);
            Draw.str(g, line, x, ly, p.incoming ? GREEN : RED, false);
            ly += 11;
        }
        if (list.isEmpty()) Draw.str(g, "No payments yet this session.", x, ly, DIM, false);

        int rx = px + 270;
        statBlock(g, rx, y, "SESSION", Tracker.netOf(false), Tracker.inOf(false), Tracker.outOf(false));
        statBlock(g, rx, y + 110, "ALL TIME", Tracker.netOf(true), Tracker.inOf(true), Tracker.outOf(true));
    }

    private static void statBlock(GuiGraphics g, int x, int y, String label, long net, long in, long out) {
        Draw.str(g, label, x, y, GREY, false);
        Draw.str(g, Tracker.money(net, true), x, y + 12, net > 0 ? GREEN : net < 0 ? RED : WHITE, true);
        Draw.str(g, "in  " + Tracker.money(in, false), x, y + 30, GREEN, false);
        Draw.str(g, "out " + Tracker.money(out, false), x, y + 41, RED, false);
    }

    // ---------------- AUCTIONS tab ----------------

    private void initAuction() {
        int x = px + 10, y = py + 62;
        if (Auction.state == Auction.State.IDLE) {
            addRenderableWidget(new FlatButton(x, y + 12, 300, 18,
                    () -> Auction.draftItemName.isEmpty() ? "Choose item from the list" : "Item: " + Auction.draftItemName,
                    () -> minecraft.setScreen(new ItemPickerScreen(this))));

            EditBox dur = new EditBox(font, x, y + 46, 140, 18, Component.literal("Duration"));
            dur.setMaxLength(6);
            dur.setValue(Auction.draftDuration);
            dur.setResponder(s -> Auction.draftDuration = s);
            addRenderableWidget(dur);

            EditBox worth = new EditBox(font, x + 160, y + 46, 140, 18, Component.literal("Worth"));
            worth.setMaxLength(12);
            worth.setValue(Auction.draftWorth);
            worth.setResponder(s -> Auction.draftWorth = s);
            addRenderableWidget(worth);

            addRenderableWidget(new FlatButton(x, y + 78, 300, 20, () -> "Start auction", this::startAuction));
            addRenderableWidget(new FlatButton(x, y + 104, 300, 18, () -> "Show auction box on screen", this::showAuctionBox));
        } else {
            if (Auction.state == Auction.State.RUNNING) {
                addRenderableWidget(new FlatButton(x, y + 170, 300, 18, () -> "End auction now", () -> {
                    Auction.endNow();
                    rebuild();
                }));
            }
            addRenderableWidget(new FlatButton(x, y + 194, 300, 18, () -> "Clear auction", () -> {
                Auction.clear();
                Auction.draftItemName = "";
                Auction.draftItemId = "";
                rebuild();
            }));
        }
    }

    private void startAuction() {
        long secs;
        try {
            secs = Long.parseLong(Auction.draftDuration.trim());
        } catch (NumberFormatException e) {
            secs = -1;
        }
        long worth = Auction.draftWorth.isBlank() ? 0 : Tracker.parseCents(Auction.draftWorth);

        if (Auction.draftItemId.isEmpty()) {
            status = "Pick an item first.";
        } else if (secs <= 0 || secs > 86400) {
            status = "Duration must be a number of seconds, e.g. 60.";
        } else if (worth < 0) {
            status = "Worth looks wrong. Try 500k or 1m.";
        } else {
            Auction.start(Auction.draftItemId, Auction.draftItemName, secs, worth);
            showAuctionBox();
            status = "Auction started.";
        }
        rebuild();
    }

    private void showAuctionBox() {
        List<Panel> ps = Config.get().panels;
        boolean found = false;
        for (Panel p : ps) {
            if (p.type.equals(Panel.AUCTION)) {
                p.visible = true;
                found = true;
            }
        }
        if (!found) ps.add(new Panel(Panel.AUCTION));
        Config.save();
        status = "Auction box is on your screen.";
        rebuild();
    }

    private void renderAuction(GuiGraphics g) {
        int x = px + 10, y = py + 62;
        if (Auction.state == Auction.State.IDLE) {
            Draw.str(g, "Duration (seconds)", x, y + 36, GREY, false);
            Draw.str(g, "Worth ($, e.g. 500k or 1m)", x + 160, y + 36, GREY, false);
            Draw.str(g, "Top bid = biggest single payment. Profit = money in - money out - worth.", x, y + 130, DIM, false);
            return;
        }
        Draw.str(g, "Item: " + Auction.itemName, x, y, WHITE, false);
        Draw.str(g, "Time left: " + Hud.timerText(), x, y + 14,
                Auction.state == Auction.State.ENDED ? ORANGE : GREEN, false);

        Draw.str(g, "TOP BIDS (single payment)", x, y + 32, GREY, false);
        List<Map.Entry<String, Long>> top = Auction.top3();
        for (int i = 0; i < 3; i++) {
            String line = (i + 1) + ". " + (i < top.size()
                    ? top.get(i).getKey() + "  " + Tracker.money(top.get(i).getValue(), false) : "-");
            Draw.str(g, line, x, y + 44 + i * 12, i == 0 ? 0xFFFBBF24 : GREY, false);
        }

        Draw.str(g, "In: " + Tracker.money(Auction.inCents, false), x, y + 88, GREEN, false);
        Draw.str(g, "Out: " + Tracker.money(Auction.outCents, false), x, y + 100, RED, false);
        Draw.str(g, "Worth set: " + Tracker.money(Auction.worthCents, false), x, y + 112, GREY, false);
        long profit = Auction.profitCents();
        Draw.str(g, "Profit: " + Tracker.money(profit, true), x, y + 126, profit >= 0 ? GREEN : RED, false);
    }

    // ---------------- SESSION tab ----------------

    private void initSession() {
        int x = px + 10, y = py + 62;
        addRenderableWidget(new FlatButton(x, y, 240, 20, () -> "Start new session", () -> {
            Tracker.newSession();
            selectedHistory = -1;
            status = "New session started. Old one is in the history.";
            rebuild();
        }));
        addRenderableWidget(new FlatButton(x, y + 24, 240, 18,
                () -> "Showing: " + (Config.get().allTime ? "All time" : "This session"), () -> {
            toggleAll();
            rebuild();
        }));
        List<Tracker.Session> hist = Tracker.history();
        for (int i = 0; i < Math.min(hist.size(), 6); i++) {
            final int sel = i;
            Tracker.Session s = hist.get(i);
            String label = s.name + "   " + Tracker.money(s.net(), true);
            FlatButton b = new FlatButton(x, y + 52 + i * 20, 240, 18, () -> label, () -> {
                selectedHistory = sel;
                rebuild();
            });
            b.highlight = sel == selectedHistory;
            addRenderableWidget(b);
        }
    }

    private void renderSession(GuiGraphics g) {
        int rx = px + 270, y = py + 62;
        Tracker.Session s;
        if (selectedHistory >= 0 && selectedHistory < Tracker.history().size()) {
            s = Tracker.history().get(selectedHistory);
        } else {
            s = Tracker.current();
        }
        long end = s.endedAt > 0 ? s.endedAt : System.currentTimeMillis();
        long mins = Math.max(0, (end - s.startedAt) / 60000);

        Draw.str(g, s.name, rx, y, WHITE, false);
        Draw.str(g, "Started " + DATE.format(Instant.ofEpochMilli(s.startedAt)), rx, y + 12, GREY, false);
        Draw.str(g, "Length " + (mins / 60) + "h " + (mins % 60) + "m", rx, y + 23, GREY, false);
        Draw.str(g, "In  " + Tracker.money(s.inCents, false), rx, y + 40, GREEN, false);
        Draw.str(g, "Out " + Tracker.money(s.outCents, false), rx, y + 51, RED, false);
        Draw.str(g, "Net " + Tracker.money(s.net(), true), rx, y + 62, s.net() >= 0 ? GREEN : RED, false);
        Draw.str(g, "Payments " + s.payments.size(), rx, y + 76, GREY, false);
        List<Map.Entry<String, Long>> payer = Tracker.topOf(s.payments, true, 1);
        List<Map.Entry<String, Long>> paid = Tracker.topOf(s.payments, false, 1);
        Draw.str(g, "Most paid by " + (payer.isEmpty() ? "-" : payer.get(0).getKey()
                + " " + Tracker.money(payer.get(0).getValue(), false)), rx, y + 88, GREY, false);
        Draw.str(g, "Most paid to " + (paid.isEmpty() ? "-" : paid.get(0).getKey()
                + " " + Tracker.money(paid.get(0).getValue(), false)), rx, y + 99, GREY, false);
    }

    // ---------------- drawing ----------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        Config cfg = Config.get();
        Draw.rect(g, px + 10, py + 10, 8, 8, cfg.accent);
        Draw.str(g, "GamblerHud", px + 24, py + 10, WHITE, true);
        String st = cfg.tracking ? "TRACKING" : "PAUSED";
        Draw.str(g, st, px + PW - 10 - Draw.width(st), py + 10, cfg.tracking ? GREEN : GREY, false);
        Draw.rect(g, px, py + 28, PW, 1, 0xFF2A3038);

        switch (tab) {
            case 0 -> renderUi(g);
            case 1 -> renderProfit(g);
            case 2 -> renderAuction(g);
            default -> renderSession(g);
        }

        if (!status.isEmpty()) Draw.str(g, status, px + 10, py + PH - 26, ORANGE, false);
        String footer = "Made by Onxon";
        Draw.str(g, footer, px + (PW - Draw.width(footer)) / 2, py + PH - 12, DIM, false);
    }

    // ---------------- keys ----------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturing) {
            capturing = false;
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                Config.get().keyCode = event.key();
                Config.save();
                status = "Menu key set to " + keyName() + ".";
            }
            GamblerHudMod.ignoreKeyUntilRelease = true;
            rebuild();
            return true;
        }
        return super.keyPressed(event);
    }
}
