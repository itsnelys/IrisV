package net.opal.irisv.option;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.neoforged.fml.loading.FMLPaths;
import net.opal.irisv.commun.utils.FunctionUtilsLogs;

public class ConfigOptions {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigOptions INSTANCE;

    public boolean enableDebugChat = false;
    public boolean enableBlockTooltipOverlay = false;

    public boolean advancedTooltips = true;
    public boolean advancedLiquidStats = true;

    public boolean enableIndicators = true;           // Switch ON/OFF global
    public int indicatorPosition = 2;                 // 1: Gauche, 2: Droite

    public boolean enableEntityTooltip = true;

    public int themeIndex = 0;

    // Dans ConfigOptions.java
    public int tooltipPosition = 0; // 0 = Centre, 1 = Gauche, 2 = Droite
    public boolean compactMode = false; // true = pas d'espaces

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("irisv")
            .resolve("options.json");

    private ConfigOptions() {}

    public static ConfigOptions getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        try {
            Path dir = CONFIG_PATH.getParent();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                FunctionUtilsLogs.actionLog("Config", "Created config directory: " + dir);
            }

            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, ConfigOptions.class);
                FunctionUtilsLogs.infoLog("Config", "Config loaded from " + CONFIG_PATH);
            } else {
                INSTANCE = new ConfigOptions();
                INSTANCE.save();
                FunctionUtilsLogs.actionLog("Config", "New config created with default values");
            }

        } catch (IOException e) {
            FunctionUtilsLogs.errorLog("Config", "Error loading config: " + e.getMessage());
            INSTANCE = new ConfigOptions();
        }
    }

    public void save() {
        try {
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_PATH, json);
            FunctionUtilsLogs.infoLog("Config", "Config saved to " + CONFIG_PATH);
        } catch (IOException e) {
            FunctionUtilsLogs.errorLog("Config", "Error saving config: " + e.getMessage());
        }
    }
}
