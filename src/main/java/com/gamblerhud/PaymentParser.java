package com.gamblerhud;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reads chat lines for payments. The patterns are editable in config.json (incomingRegex / outgoingRegex).
 * Each pattern needs three groups: player name, amount, and optional k/m/b suffix.
 */
public final class PaymentParser {
    private static final String DEFAULT_IN = "(\\w{3,16}) (?:paid you|sent you) \\$?([\\d,]+(?:\\.\\d+)?)\\s*([kKmMbB]?)";
    private static final String DEFAULT_OUT = "You (?:paid|sent) (\\w{3,16}) \\$?([\\d,]+(?:\\.\\d+)?)\\s*([kKmMbB]?)";

    private PaymentParser() {
    }

    public static void handle(String msg) {
        Config c = Config.get();
        if (!c.tracking) return;

        Matcher m = compile(c.incomingRegex, DEFAULT_IN).matcher(msg);
        if (m.find()) {
            record(m, true);
            return;
        }
        m = compile(c.outgoingRegex, DEFAULT_OUT).matcher(msg);
        if (m.find()) record(m, false);
    }

    private static void record(Matcher m, boolean incoming) {
        try {
            String suffix = m.groupCount() >= 3 && m.group(3) != null ? m.group(3) : "";
            long cents = Tracker.toCents(m.group(2), suffix);
            if (cents > 0) Tracker.record(m.group(1), cents, incoming);
        } catch (Exception e) {
            System.err.println("[GamblerHud] Could not read payment from chat: " + e);
        }
    }

    private static Pattern compile(String regex, String fallback) {
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            return Pattern.compile(fallback);
        }
    }
}
