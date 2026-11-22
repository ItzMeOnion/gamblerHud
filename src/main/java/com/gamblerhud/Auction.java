package com.gamblerhud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One auction at a time. Only each player's single biggest payment counts for the ranking,
 * so the winner is whoever paid the most in one go.
 */
public final class Auction {

    public enum State { IDLE, RUNNING, ENDED }

    public static State state = State.IDLE;
    public static String itemId = "";
    public static String itemName = "";
    public static long durationMs, startedAt, worthCents;
    public static long inCents, outCents;
    public static final Map<String, Long> bestSingle = new HashMap<>();

    // Draft values, kept while the menu is rebuilt or the item picker is open.
    public static String draftItemId = "";
    public static String draftItemName = "";
    public static String draftDuration = "60";
    public static String draftWorth = "0";

    private Auction() {
    }

    public static void start(String id, String name, long seconds, long worth) {
        itemId = id;
        itemName = name;
        durationMs = seconds * 1000L;
        worthCents = worth;
        startedAt = System.currentTimeMillis();
        inCents = 0;
        outCents = 0;
        bestSingle.clear();
        state = State.RUNNING;
    }

    public static void onPayment(Tracker.Payment p) {
        if (state != State.RUNNING) return;
        if (p.incoming) {
            inCents += p.cents;
            bestSingle.merge(p.player, p.cents, (a, b) -> Math.max(a, b));
        } else {
            outCents += p.cents;
        }
    }

    public static void tick() {
        if (state == State.RUNNING && remainingMs() <= 0) state = State.ENDED;
    }

    public static void endNow() {
        if (state == State.RUNNING) state = State.ENDED;
    }

    public static void clear() {
        state = State.IDLE;
        bestSingle.clear();
        inCents = 0;
        outCents = 0;
    }

    public static long remainingMs() {
        if (state != State.RUNNING) return 0;
        return Math.max(0, startedAt + durationMs - System.currentTimeMillis());
    }

    /** Money in minus money out minus the worth you set. */
    public static long profitCents() {
        return inCents - outCents - worthCents;
    }

    public static List<Map.Entry<String, Long>> top3() {
        List<Map.Entry<String, Long>> list = new ArrayList<>(bestSingle.entrySet());
        list.sort(Map.Entry.<String, Long>comparingByValue().reversed());
        return list.subList(0, Math.min(3, list.size()));
    }
}
