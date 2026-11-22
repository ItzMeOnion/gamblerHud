package com.gamblerhud;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Records every payment in cents and keeps the session history. Saved to config/gamblerhud/sessions.json. */
public final class Tracker {

    public static final class Payment {
        public String player = "";
        public long cents;
        public boolean incoming;
        public long time;
    }

    public static final class Session {
        public String name = "";
        public long startedAt, endedAt;
        public long inCents, outCents;
        public List<Payment> payments = new ArrayList<>();

        public Session() {
        }

        public Session(String name) {
            this.name = name;
            this.startedAt = System.currentTimeMillis();
        }

        public long net() {
            return inCents - outCents;
        }
    }

    public static final class Store {
        public Session current = new Session("Session 1");
        public List<Session> history = new ArrayList<>();   // newest first
    }

    private static final int MAX_PAYMENTS = 4000;   // per session, for the graph and leaderboards
    private static final Path FILE = Config.DIR.resolve("sessions.json");
    private static Store store = new Store();

    private Tracker() {
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Store s = Config.GSON.fromJson(Files.readString(FILE), Store.class);
                if (s != null) store = s;
            }
        } catch (Exception e) {
            System.err.println("[GamblerHud] Could not read sessions, starting fresh: " + e);
        }
        if (store.current == null) store.current = new Session("Session 1");
        if (store.history == null) store.history = new ArrayList<>();
    }

    private static void save() {
        try {
            Files.createDirectories(Config.DIR);
            Files.writeString(FILE, Config.GSON.toJson(store));
        } catch (IOException e) {
            System.err.println("[GamblerHud] Could not save sessions: " + e);
        }
    }

    public static void record(String player, long cents, boolean incoming) {
        Payment p = new Payment();
        p.player = player;
        p.cents = cents;
        p.incoming = incoming;
        p.time = System.currentTimeMillis();

        Session s = store.current;
        s.payments.add(p);
        while (s.payments.size() > MAX_PAYMENTS) s.payments.remove(0);
        if (incoming) s.inCents += cents;
        else s.outCents += cents;

        Auction.onPayment(p);
        save();
    }

    public static Session current() {
        return store.current;
    }

    public static List<Session> history() {
        return store.history;
    }

    /** Archives the current session and starts a fresh one. */
    public static void newSession() {
        store.current.endedAt = System.currentTimeMillis();
        store.history.add(0, store.current);
        store.current = new Session("Session " + (store.history.size() + 1));
        save();
    }

    public static long allIn() {
        long t = store.current.inCents;
        for (Session s : store.history) t += s.inCents;
        return t;
    }

    public static long allOut() {
        long t = store.current.outCents;
        for (Session s : store.history) t += s.outCents;
        return t;
    }

    public static long inOf(boolean all) {
        return all ? allIn() : store.current.inCents;
    }

    public static long outOf(boolean all) {
        return all ? allOut() : store.current.outCents;
    }

    public static long netOf(boolean all) {
        return inOf(all) - outOf(all);
    }

    /** Payments for the current session, or all sessions oldest-first when all is true. */
    public static List<Payment> payments(boolean all) {
        if (!all) return store.current.payments;
        List<Payment> list = new ArrayList<>();
        for (int i = store.history.size() - 1; i >= 0; i--) list.addAll(store.history.get(i).payments);
        list.addAll(store.current.payments);
        return list;
    }

    /** Running balance after each payment, starting at 0 (the baseline). */
    public static List<Long> graph(boolean all) {
        List<Long> out = new ArrayList<>();
        out.add(0L);
        long run = 0;
        for (Payment p : payments(all)) {
            run += p.incoming ? p.cents : -p.cents;
            out.add(run);
        }
        return out;
    }

    public static List<Map.Entry<String, Long>> top(boolean all, boolean incoming, int n) {
        return topOf(payments(all), incoming, n);
    }

    /** Biggest totals per player for one direction, biggest first. */
    public static List<Map.Entry<String, Long>> topOf(List<Payment> list, boolean incoming, int n) {
        Map<String, Long> totals = new HashMap<>();
        for (Payment p : list) {
            if (p.incoming == incoming) totals.merge(p.player, p.cents, (a, b) -> a + b);
        }
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort(Map.Entry.<String, Long>comparingByValue().reversed());
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    /** Formats cents as $1,234.56, optionally with a + or - sign. */
    public static String money(long cents, boolean signed) {
        long a = Math.abs(cents);
        String s = String.format(Locale.US, "$%,d.%02d", a / 100, a % 100);
        if (cents < 0) return "-" + s;
        if (signed && cents > 0) return "+" + s;
        return s;
    }

    private static final Pattern AMOUNT = Pattern.compile("^\\s*\\$?([\\d,]*\\.?\\d+)\\s*([kKmMbB]?)\\s*$");

    /** Parses user input like "500k", "1.5m" or "2,000" into cents. Returns -1 if invalid. */
    public static long parseCents(String s) {
        Matcher m = AMOUNT.matcher(s == null ? "" : s);
        if (!m.matches()) return -1;
        try {
            return toCents(m.group(1), m.group(2));
        } catch (Exception e) {
            return -1;
        }
    }

    public static long toCents(String amount, String suffix) {
        double v = Double.parseDouble(amount.replace(",", ""));
        return Math.round(v * multiplier(suffix) * 100.0);
    }

    private static double multiplier(String suffix) {
        if (suffix == null) return 1;
        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "k" -> 1e3;
            case "m" -> 1e6;
            case "b" -> 1e9;
            default -> 1;
        };
    }
}
