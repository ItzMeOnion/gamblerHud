package com.gamblerhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** All user settings. Saved to config/gamblerhud/config.json. */
public final class Config {
    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("gamblerhud");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = DIR.resolve("config.json");

    private static Config instance = new Config();

    public int keyCode = 71;              // G
    public int menuBg = 0xF2101418;       // menu background (ARGB)
    public int accent = 0xFF3B82F6;       // highlight colour
    public boolean tracking = true;
    public boolean allTime = false;       // false = current session, true = all time
    public String incomingRegex = "(\\w{3,16}) (?:paid you|sent you) \\$?([\\d,]+(?:\\.\\d+)?)\\s*([kKmMbB]?)";
    public String outgoingRegex = "You (?:paid|sent) (\\w{3,16}) \\$?([\\d,]+(?:\\.\\d+)?)\\s*([kKmMbB]?)";
    public List<Panel> panels = new ArrayList<>();

    public static Config get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            System.err.println("[GamblerHud] Could not read config, using defaults: " + e);
        }
        if (instance.panels == null) instance.panels = new ArrayList<>();
        if (instance.panels.isEmpty()) {
            instance.panels.add(new Panel(Panel.PROFIT));
            instance.panels.add(new Panel(Panel.GRAPH));
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            System.err.println("[GamblerHud] Could not save config: " + e);
        }
    }
}
