package com.gamblerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

/** Draws every visible panel on the in-game HUD. */
public final class Hud {
    public static final int GREEN = 0xFF4ADE80;
    public static final int RED = 0xFFF87171;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int GREY = 0xFF9CA3AF;
    public static final int DIM = 0xFF6B7280;
    public static final int YELLOW = 0xFFFBBF24;
    public static final int ORANGE = 0xFFFB923C;

    private Hud() {
    }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        for (Panel p : Config.get().panels) {
            if (!p.visible) continue;
            if (!p.placed) p.anchorTo(sw, sh);
            draw(g, p);
        }
    }

    public static void draw(GuiGraphics g, Panel p) {
        switch (p.type) {
            case Panel.TEXT -> drawText(g, p);
            case Panel.PROFIT -> drawProfit(g, p);
            case Panel.GRAPH -> drawGraph(g, p);
            case Panel.PAY_ME -> drawTop(g, p, true);
            case Panel.PAID_OUT -> drawTop(g, p, false);
            case Panel.AUCTION -> drawAuction(g, p);
            default -> {
            }
        }
    }

    // ---------- helpers ----------

    private static void frame(GuiGraphics g, Panel p) {
        if ((p.bg >>> 24) != 0) Draw.rect(g, p.x, p.y, p.w, p.h, p.bg);
        if ((p.border >>> 24) != 0) Draw.outline(g, p.x, p.y, p.w, p.h, p.border);
    }

    private static Identifier fontId(String f) {
        try {
            return Identifier.parse(f);
        } catch (Exception e) {
            return Identifier.parse("minecraft:default");
        }
    }

    private static Component styled(String s, Panel p) {
        Identifier id = fontId(p.font);
        return Component.literal(s).withStyle(st -> st.withFont(new FontDescription.Resource(id)));
    }

    /** Draws text at screen x,y, scaled, using the panel's font and shadow setting. */
    private static void text(GuiGraphics g, Panel p, String s, int x, int y, int color, float scale) {
        Minecraft mc = Minecraft.getInstance();
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.drawString(mc.font, styled(s, p), 0, 0, color, p.shadow);
        g.pose().popMatrix();
    }

    private static int width(Panel p, String s, float scale) {
        return (int) (Minecraft.getInstance().font.width(styled(s, p)) * scale);
    }

    /** Replaces {tokens} in a text box with live numbers. */
    public static String expand(String t) {
        boolean all = Config.get().allTime;
        return t.replace("{session}", Tracker.money(Tracker.current().net(), true))
                .replace("{alltime}", Tracker.money(Tracker.allNet(), true))
                .replace("{in}", Tracker.money(Tracker.inOf(all), false))
                .replace("{out}", Tracker.money(Tracker.outOf(all), false))
                .replace("{timer}", timerText())
                .replace("{auction}", Auction.state == Auction.State.IDLE ? "-" : Auction.itemName)
                .replace("{auction_profit}", Auction.state == Auction.State.IDLE
                        ? "-" : Tracker.money(Auction.profitCents(), true));
    }

    public static String timerText() {
        if (Auction.state == Auction.State.IDLE) return "--:--";
        if (Auction.state == Auction.State.ENDED) return "ENDED";
        long secs = (Auction.remainingMs() + 999) / 1000;
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    // ---------- panels ----------

    private static void drawText(GuiGraphics g, Panel p) {
        String s = expand(p.text);
        p.w = width(p, s, p.scale) + 12;
        p.h = (int) (10 * p.scale) + 8;
        frame(g, p);
        text(g, p, s, p.x + 6, p.y + 4, p.fg, p.scale);
    }

    private static void drawProfit(GuiGraphics g, Panel p) {
        boolean all = Config.get().allTime;
        long net = Tracker.netOf(all);
        int c = net > 0 ? GREEN : net < 0 ? RED : WHITE;
        frame(g, p);
        text(g, p, all ? "ALL TIME PROFIT" : "PROFIT", p.x + 6, p.y + 4, GREY, 0.75f);
        text(g, p, Tracker.money(net, true), p.x + 6, p.y + 14, c, 1.4f);
        text(g, p, "in " + Tracker.money(Tracker.inOf(all), false)
                + "  out " + Tracker.money(Tracker.outOf(all), false),
                p.x + 6, p.y + p.h - 12, GREY, 0.75f);
    }

    private static void drawGraph(GuiGraphics g, Panel p) {
        boolean all = Config.get().allTime;
        long net = Tracker.netOf(all);
        int c = net > 0 ? GREEN : net < 0 ? RED : WHITE;
        frame(g, p);
        text(g, p, (all ? "ALL TIME " : "") + Tracker.money(net, true), p.x + 6, p.y + 4, c, 0.9f);

        List<Long> pts = Tracker.graph(all);
        int ix = p.x + 6, iy = p.y + 18, iw = p.w - 12, ih = p.h - 22;
        long maxAbs = 1;
        for (long v : pts) maxAbs = Math.max(maxAbs, Math.abs(v));
        int base = iy + ih / 2;
        Draw.rect(g, ix, base, iw, 1, 0x88FFFFFF);          // zero baseline
        int n = pts.size();
        if (n < 2) return;
        double half = ih / 2.0;
        int prevX = ix;
        int prevY = base - (int) Math.round(pts.get(0) * half / maxAbs);
        for (int i = 1; i < n; i++) {
            int x = ix + (int) Math.round((double) i / (n - 1) * iw);
            long v = pts.get(i);
            int y = base - (int) Math.round(v * half / maxAbs);
            Draw.line(g, prevX, prevY, x, y, v >= 0 ? GREEN : RED);
            prevX = x;
            prevY = y;
        }
    }

    private static void drawTop(GuiGraphics g, Panel p, boolean incoming) {
        boolean all = Config.get().allTime;
        frame(g, p);
        text(g, p, incoming ? "WHO PAID ME MOST" : "WHO I PAID MOST", p.x + 6, p.y + 4, GREY, 0.7f);
        int rows = Math.max(1, (p.h - 16) / 12);
        List<Map.Entry<String, Long>> list = Tracker.top(all, incoming, rows);
        if (list.isEmpty()) {
            text(g, p, "nobody yet", p.x + 6, p.y + 16, DIM, 0.9f);
            return;
        }
        int y = p.y + 16;
        for (int i = 0; i < list.size(); i++) {
            Map.Entry<String, Long> e = list.get(i);
            String amt = Tracker.money(e.getValue(), false);
            text(g, p, (i + 1) + ". " + e.getKey(), p.x + 6, y, WHITE, 0.9f);
            int aw = width(p, amt, 0.9f);
            text(g, p, amt, p.x + p.w - 6 - aw, y, incoming ? GREEN : RED, 0.9f);
            y += 12;
        }
    }

    private static void drawAuction(GuiGraphics g, Panel p) {
        frame(g, p);
        text(g, p, "AUCTION", p.x + 6, p.y + 4, GREY, 0.7f);
        if (Auction.state == Auction.State.IDLE) {
            text(g, p, "No auction running", p.x + 6, p.y + 18, DIM, 0.9f);
            return;
        }
        text(g, p, Auction.itemName, p.x + 6, p.y + 14, WHITE, 0.9f);

        long rem = Auction.remainingMs();
        double frac = Auction.durationMs > 0 ? (double) rem / Auction.durationMs : 0;
        frac = Math.max(0, Math.min(1, frac));
        int col = frac > 0.5 ? GREEN : frac > 0.2 ? YELLOW : RED;
        if (Auction.state == Auction.State.RUNNING && rem <= 10_000 && (System.currentTimeMillis() / 250) % 2 == 0) {
            col = WHITE;   // flash in the last 10 seconds
        }
        if (Auction.state == Auction.State.ENDED) col = ORANGE;

        text(g, p, timerText(), p.x + 6, p.y + 24, col, 1.5f);
        int barW = p.w - 12;
        Draw.rect(g, p.x + 6, p.y + 40, barW, 3, 0xFF1F2937);
        Draw.rect(g, p.x + 6, p.y + 40, (int) (barW * frac), 3, col);

        List<Map.Entry<String, Long>> top = Auction.top3();
        int y = p.y + 46;
        for (int i = 0; i < 3; i++) {
            boolean has = i < top.size();
            text(g, p, (i + 1) + ". " + (has ? top.get(i).getKey() : "-"), p.x + 6, y, i == 0 ? YELLOW : GREY, 0.8f);
            if (has) {
                String amt = Tracker.money(top.get(i).getValue(), false);
                text(g, p, amt, p.x + p.w - 6 - width(p, amt, 0.8f), y, GREY, 0.8f);
            }
            y += 10;
        }
        long profit = Auction.profitCents();
        text(g, p, "profit " + Tracker.money(profit, true), p.x + 6, p.y + p.h - 12,
                profit >= 0 ? GREEN : RED, 0.9f);
    }
}
