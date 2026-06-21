package net.opal.irisv.option;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import net.opal.irisv.commun.utils.FunctionUtilsLogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class ConfigOptions {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigOptions INSTANCE;

    public boolean enableDebugChat = false;
    public boolean enableBlockTooltipOverlay = false;
    public boolean advancedTooltips = true;
    public boolean advancedLiquidStats = true;
    public boolean enableIndicators = true;
    public IndicatorPosition indicatorPosition = IndicatorPosition.RIGHT;
    public boolean enableEntityTooltip = true;
    public Theme theme = Theme.DARKNESS;
    public TooltipPosition tooltipPosition = TooltipPosition.TOP_CENTER;
    public boolean compactMode = false;

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("irisv")
            .resolve("options.json");

    ConfigOptions() {}

    public static ConfigOptions getInstance() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                INSTANCE = fromJson(Files.readString(CONFIG_PATH));
                INSTANCE.save();
                FunctionUtilsLogs.infoLog("Config", "Config loaded from " + CONFIG_PATH);
            } else {
                INSTANCE = new ConfigOptions();
                INSTANCE.save();
                FunctionUtilsLogs.actionLog("Config", "New config created with default values");
            }
        } catch (IOException | RuntimeException e) {
            FunctionUtilsLogs.errorLog("Config", "Error loading config: " + e.getMessage());
            INSTANCE = new ConfigOptions();
        }
    }

    static ConfigOptions fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        ConfigOptions options = new ConfigOptions();
        options.enableDebugChat = readBoolean(root, "enableDebugChat", options.enableDebugChat);
        options.enableBlockTooltipOverlay = readBoolean(root, "enableBlockTooltipOverlay", options.enableBlockTooltipOverlay);
        options.advancedTooltips = readBoolean(root, "advancedTooltips", options.advancedTooltips);
        options.advancedLiquidStats = readBoolean(root, "advancedLiquidStats", options.advancedLiquidStats);
        options.enableIndicators = readBoolean(root, "enableIndicators", options.enableIndicators);
        options.enableEntityTooltip = readBoolean(root, "enableEntityTooltip", options.enableEntityTooltip);
        options.compactMode = readBoolean(root, "compactMode", options.compactMode);
        options.indicatorPosition = readEnum(root, "indicatorPosition", IndicatorPosition.class, IndicatorPosition.RIGHT);
        options.tooltipPosition = readEnum(root, "tooltipPosition", TooltipPosition.class, TooltipPosition.TOP_CENTER);
        options.theme = readEnum(root, root.has("theme") ? "theme" : "themeIndex", Theme.class, Theme.DARKNESS);
        return options;
    }

    String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("enableDebugChat", enableDebugChat);
        root.addProperty("enableBlockTooltipOverlay", enableBlockTooltipOverlay);
        root.addProperty("advancedTooltips", advancedTooltips);
        root.addProperty("advancedLiquidStats", advancedLiquidStats);
        root.addProperty("enableIndicators", enableIndicators);
        root.addProperty("indicatorPosition", indicatorPosition.name());
        root.addProperty("enableEntityTooltip", enableEntityTooltip);
        root.addProperty("theme", theme.name());
        root.addProperty("tooltipPosition", tooltipPosition.name());
        root.addProperty("compactMode", compactMode);
        return GSON.toJson(root);
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, toJson());
            FunctionUtilsLogs.infoLog("Config", "Config saved to " + CONFIG_PATH);
        } catch (IOException e) {
            FunctionUtilsLogs.errorLog("Config", "Error saving config: " + e.getMessage());
        }
    }

    private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
        JsonElement value = root.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
    }

    private static <E extends Enum<E> & LegacyValue> E readEnum(JsonObject root, String key, Class<E> type, E fallback) {
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonPrimitive()) return fallback;
        if (value.getAsJsonPrimitive().isNumber()) {
            int legacy = value.getAsInt();
            for (E candidate : type.getEnumConstants()) {
                if (candidate.legacyValue() == legacy) return candidate;
            }
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.getAsString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public interface LegacyValue { int legacyValue(); }

    public enum IndicatorPosition implements LegacyValue {
        LEFT(1), RIGHT(2);
        private final int legacyValue;
        IndicatorPosition(int legacyValue) { this.legacyValue = legacyValue; }
        public int legacyValue() { return legacyValue; }
        public IndicatorPosition next() { return this == LEFT ? RIGHT : LEFT; }
    }

    public enum TooltipPosition implements LegacyValue {
        TOP_CENTER(0), TOP_LEFT(1), TOP_RIGHT(2), BOTTOM_LEFT(3), BOTTOM_RIGHT(4);
        private final int legacyValue;
        TooltipPosition(int legacyValue) { this.legacyValue = legacyValue; }
        public int legacyValue() { return legacyValue; }
        public static TooltipPosition fromSlider(int value) {
            for (TooltipPosition position : values()) if (position.legacyValue == value) return position;
            return TOP_CENTER;
        }
    }

    public enum Theme implements LegacyValue {
        DARKNESS(0), FROST(1), ELDER(2), ABYSS(3), FOREST(4), CRIMSON(5), VALHALLA(6);
        private final int legacyValue;
        Theme(int legacyValue) { this.legacyValue = legacyValue; }
        public int legacyValue() { return legacyValue; }
        public Theme next() { return values()[(ordinal() + 1) % values().length]; }
        public String translationKey() { return "menu.irisv.theme." + name().toLowerCase(Locale.ROOT); }
    }
}
