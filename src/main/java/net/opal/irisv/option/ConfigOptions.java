package net.opal.irisv.option;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import net.opal.irisv.commun.utils.FunctionUtilsLogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigOptions {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigOptions INSTANCE;

    public boolean enableDebugChat = false;
    public boolean enableBlockTooltipOverlay = false;
    public boolean advancedTooltips = true;
    public boolean advancedLiquidStats = true;
    public boolean enableBlockProviderTooltips = true;
    public boolean enableDropTooltip = true;
    public boolean enableFluidTooltips = true;
    public boolean enableInventoryTooltips = true;
    public boolean enableRecipeOverlay = true;
    public boolean enableIndicators = true;
    public boolean enableToolDurabilityIndicator = true;
    public boolean showDurabilityDetails = true;
    public IndicatorPosition indicatorPosition = IndicatorPosition.RIGHT;
    public boolean enableEntityTooltip = true;
    public Theme theme = Theme.DARKNESS;
    public TooltipPosition tooltipPosition = TooltipPosition.TOP_CENTER;
    public boolean compactMode = false;
    public String recipeCategory = "ALL";
    public boolean recipeHighlightSearchMode = false;
    public boolean inventorySearchHighlight = true;
    public boolean recipeAvailability = true;
    public List<String> disabledRecipeHudCategories = new ArrayList<>();
    public int recipePage = 0;
    public List<String> recipeFavorites = new ArrayList<>();
    public List<String> savedRecipeBookmarks = new ArrayList<>();
    public String favoriteFilter = "GLOBAL";
    public List<String> favoriteOrder = new ArrayList<>();

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
        options.enableBlockProviderTooltips = readBoolean(root, "enableBlockProviderTooltips", options.enableBlockProviderTooltips);
        options.enableDropTooltip = readBoolean(root, "enableDropTooltip", options.enableDropTooltip);
        options.enableFluidTooltips = readBoolean(root, "enableFluidTooltips", options.enableFluidTooltips);
        options.enableInventoryTooltips = readBoolean(root, "enableInventoryTooltips", options.enableInventoryTooltips);
        options.enableRecipeOverlay = readBoolean(root, "enableRecipeOverlay", options.enableRecipeOverlay);
        options.enableIndicators = readBoolean(root, "enableIndicators", options.enableIndicators);
        options.enableToolDurabilityIndicator = readBoolean(root, "enableToolDurabilityIndicator", options.enableToolDurabilityIndicator);
        options.showDurabilityDetails = readBoolean(root, "showDurabilityDetails", options.showDurabilityDetails);
        options.enableEntityTooltip = readBoolean(root, "enableEntityTooltip", options.enableEntityTooltip);
        options.compactMode = readBoolean(root, "compactMode", options.compactMode);
        options.recipeCategory = readString(root, "recipeCategory", options.recipeCategory);
        options.recipeHighlightSearchMode = readBoolean(root, "recipeHighlightSearchMode", options.recipeHighlightSearchMode);
        options.inventorySearchHighlight = readBoolean(root, "inventorySearchHighlight", options.inventorySearchHighlight);
        options.recipeAvailability = readBoolean(root, "recipeAvailability", options.recipeAvailability);
        options.disabledRecipeHudCategories = readStringList(root, "disabledRecipeHudCategories");
        options.recipePage = readInt(root, "recipePage", options.recipePage);
        options.recipeFavorites = readStringList(root, "recipeFavorites");
        options.savedRecipeBookmarks = readStringList(root, "savedRecipeBookmarks");
        options.favoriteFilter = readString(root, "favoriteFilter", "GLOBAL");
        options.favoriteOrder = readStringList(root, "favoriteOrder");
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
        root.addProperty("enableBlockProviderTooltips", enableBlockProviderTooltips);
        root.addProperty("enableDropTooltip", enableDropTooltip);
        root.addProperty("enableFluidTooltips", enableFluidTooltips);
        root.addProperty("enableInventoryTooltips", enableInventoryTooltips);
        root.addProperty("enableRecipeOverlay", enableRecipeOverlay);
        root.addProperty("enableIndicators", enableIndicators);
        root.addProperty("enableToolDurabilityIndicator", enableToolDurabilityIndicator);
        root.addProperty("showDurabilityDetails", showDurabilityDetails);
        root.addProperty("indicatorPosition", indicatorPosition.name());
        root.addProperty("enableEntityTooltip", enableEntityTooltip);
        root.addProperty("theme", theme.name());
        root.addProperty("tooltipPosition", tooltipPosition.name());
        root.addProperty("compactMode", compactMode);
        root.addProperty("recipeCategory", recipeCategory);
        root.addProperty("recipeHighlightSearchMode", recipeHighlightSearchMode);
        root.addProperty("inventorySearchHighlight", inventorySearchHighlight);
        root.addProperty("recipeAvailability", recipeAvailability);
        JsonArray disabledHuds = new JsonArray();
        for (String key : disabledRecipeHudCategories) disabledHuds.add(key);
        root.add("disabledRecipeHudCategories", disabledHuds);
        root.addProperty("recipePage", recipePage);
        JsonArray favoriteArray = new JsonArray();
        for (String favorite : recipeFavorites) {
            favoriteArray.add(favorite);
        }
        root.add("recipeFavorites", favoriteArray);
        JsonArray bookmarks = new JsonArray();
        for (String bookmark : savedRecipeBookmarks) bookmarks.add(bookmark);
        root.add("savedRecipeBookmarks", bookmarks);
        root.addProperty("favoriteFilter", favoriteFilter);
        JsonArray order = new JsonArray();
        for (String key : favoriteOrder) order.add(key);
        root.add("favoriteOrder", order);
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

    public static Path exchangePath() {
        return CONFIG_PATH.resolveSibling("irisv-settings.json");
    }

    private static final List<String> PRIVATE_STATE = List.of("recipeFavorites", "savedRecipeBookmarks",
            "favoriteOrder", "favoriteFilter", "recipeCategory", "recipePage", "recipeHighlightSearchMode");

    public String exportSettings() {
        JsonObject settings = JsonParser.parseString(toJson()).getAsJsonObject();
        PRIVATE_STATE.forEach(settings::remove);
        JsonObject document = new JsonObject();
        document.addProperty("format", "irisv-settings");
        document.addProperty("version", 1);
        document.add("settings", settings);
        return GSON.toJson(document);
    }

    ConfigOptions mergeSettings(String json) {
        JsonObject document = JsonParser.parseString(json).getAsJsonObject();
        if (!document.has("format") || !"irisv-settings".equals(document.get("format").getAsString())
                || !document.has("version") || !"1".equals(document.get("version").getAsString())
                || !document.has("settings") || !document.get("settings").isJsonObject()) {
            throw new IllegalArgumentException("Unsupported settings format");
        }
        JsonObject merged = JsonParser.parseString(toJson()).getAsJsonObject();
        JsonObject incoming = document.getAsJsonObject("settings");
        if (incoming.size() == 0) throw new IllegalArgumentException("Empty settings");
        for (var entry : incoming.entrySet()) {
            String key = entry.getKey();
            if (PRIVATE_STATE.contains(key)) continue;
            JsonElement expected = merged.get(key);
            JsonElement value = entry.getValue();
            if (expected == null) throw new IllegalArgumentException("Unknown setting: " + key);
            boolean valid;
            if (expected.isJsonArray()) {
                valid = value.isJsonArray() && value.getAsJsonArray().size() <= 1024;
                if (valid) for (JsonElement element : value.getAsJsonArray()) {
                    valid &= element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
                }
            } else {
                valid = value.isJsonPrimitive() && (expected.getAsJsonPrimitive().isBoolean()
                        ? value.getAsJsonPrimitive().isBoolean() : value.getAsJsonPrimitive().isString());
            }
            if (!valid) throw new IllegalArgumentException("Invalid setting: " + key);
            merged.add(key, value);
        }
        ConfigOptions result = fromJson(GSON.toJson(merged));
        JsonObject normalized = JsonParser.parseString(result.toJson()).getAsJsonObject();
        for (String key : incoming.keySet()) {
            if (!PRIVATE_STATE.contains(key) && !normalized.get(key).equals(merged.get(key))) {
                throw new IllegalArgumentException("Invalid setting value: " + key);
            }
        }
        return result;
    }

    public void exportSettingsFile() throws IOException {
        writeAtomic(exchangePath(), exportSettings());
    }

    public static void importSettingsFile() throws IOException {
        if (Files.size(exchangePath()) > 262144) throw new IOException("Settings file too large");
        ConfigOptions current = getInstance();
        ConfigOptions next = current.mergeSettings(Files.readString(exchangePath()));
        writeAtomic(CONFIG_PATH.resolveSibling("options.before-import.json"), current.toJson());
        writeAtomic(CONFIG_PATH, next.toJson());
        INSTANCE = next;
    }

    private static void writeAtomic(Path path, String text) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "irisv-", ".tmp");
        try {
            Files.writeString(temporary, text);
            try {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
        JsonElement value = root.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
    }

    private static int readInt(JsonObject root, String key, int fallback) {
        JsonElement value = root.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : fallback;
    }

    private static String readString(JsonObject root, String key, String fallback) {
        JsonElement value = root.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    private static List<String> readStringList(JsonObject root, String key) {
        List<String> values = new ArrayList<>();
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonArray()) return values;
        for (JsonElement element : value.getAsJsonArray()) {
            if (element.isJsonPrimitive()) {
                values.add(element.getAsString());
            }
        }
        return values;
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
