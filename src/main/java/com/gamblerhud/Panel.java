package com.gamblerhud;

import java.util.UUID;

/** One thing drawn on the HUD: a text box, profit box, graph, leaderboard or auction box. */
public final class Panel {
    public static final String TEXT = "TEXT";
    public static final String PROFIT = "PROFIT";
    public static final String GRAPH = "GRAPH";
    public static final String PAY_ME = "PAY_ME";
    public static final String PAID_OUT = "PAID_OUT";
    public static final String AUCTION = "AUCTION";

    public String id = UUID.randomUUID().toString().substring(0, 8);
    public String type = PROFIT;
    public int x = 8, y = 8, w = 150, h = 44;
    public boolean visible = true;
    public boolean placed = false;       // false = snap to its anchor on the next frame
    public String anchor = "TL";         // TL TC TR ML MC MR BL BC BR
    public float scale = 1.0f;           // text boxes only
    public boolean shadow = true;
    public int fg = 0xFFFFFFFF;
    public int bg = 0xD8101418;
    public int border = 0xFF2A3038;
    public String font = "minecraft:default";
    public String text = "{session}";

    public Panel() {
    }

    public Panel(String type) {
        this.type = type;
        switch (type) {
            case GRAPH -> {
                w = 180;
                h = 70;
                anchor = "BL";
            }
            case PAY_ME -> {
                w = 150;
                h = 66;
                anchor = "MR";
            }
            case PAID_OUT -> {
                w = 150;
                h = 66;
                anchor = "BR";
            }
            case AUCTION -> {
                w = 160;
                h = 92;
                anchor = "TR";
            }
            case TEXT -> {
                w = 80;
                h = 16;
                anchor = "TC";
            }
            default -> {
                w = 170;
                h = 44;
                anchor = "TL";
            }
        }
    }

    public String label() {
        return switch (type) {
            case TEXT -> {
                String t = text.replace('\n', ' ');
                yield "Text: " + (t.length() > 14 ? t.substring(0, 14) + "..." : t);
            }
            case PROFIT -> "Profit box";
            case GRAPH -> "Graph";
            case PAY_ME -> "Top payers";
            case PAID_OUT -> "Top paid to";
            case AUCTION -> "Auction box";
            default -> type;
        };
    }

    /** Moves this panel to its anchor point on a screen of the given size. */
    public void anchorTo(int sw, int sh) {
        int m = 8;
        char v = anchor.charAt(0);
        char hz = anchor.charAt(1);
        x = hz == 'L' ? m : hz == 'R' ? sw - w - m : (sw - w) / 2;
        y = v == 'T' ? m : v == 'B' ? sh - h - m : (sh - h) / 2;
        placed = true;
    }
}
