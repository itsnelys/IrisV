package net.opal.irisv.option;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigOptionsTest {
    @Test void perScreenHudSettingsRoundTripIndependently() {
        ConfigOptions config = ConfigOptions.fromJson("{}");
        assertEquals(java.util.List.of(), config.disabledRecipeHudCategories);
        config.disabledRecipeHudCategories.add("chest");
        config.disabledRecipeHudCategories.add("furnace");
        ConfigOptions restored = ConfigOptions.fromJson(config.toJson());
        assertEquals(java.util.List.of("chest", "furnace"), restored.disabledRecipeHudCategories);
        assertFalse(restored.disabledRecipeHudCategories.contains("crafting"));
        restored.disabledRecipeHudCategories.remove("chest");
        assertEquals(java.util.List.of("furnace"), ConfigOptions.fromJson(restored.toJson()).disabledRecipeHudCategories);
    }
    @Test void recipeAvailabilitySettingPersists() {
        ConfigOptions config = ConfigOptions.fromJson("{}");
        assertEquals(true, config.recipeAvailability);
        config.recipeAvailability = false;
        assertFalse(ConfigOptions.fromJson(config.toJson()).recipeAvailability);
    }
    @Test
    void favoriteOrderPersistsWithoutChangingBookmarks() {
        ConfigOptions options = ConfigOptions.fromJson("{}");
        assertEquals(java.util.List.of(), options.favoriteOrder);
        options.favoriteOrder = java.util.List.of("item:minecraft:stone|{}", "recipe:minecraft:oak_planks|{}");
        options.recipeFavorites = java.util.List.of("minecraft:stone|{}");
        ConfigOptions restored = ConfigOptions.fromJson(options.toJson());
        assertEquals(options.favoriteOrder, restored.favoriteOrder);
        assertEquals(options.recipeFavorites, restored.recipeFavorites);
        assertEquals(java.util.List.of(), restored.savedRecipeBookmarks);
    }

    @Test
    void inventorySearchDefaultsAndToggleRoundTrip() {
        assertEquals(true, ConfigOptions.fromJson("{}").inventorySearchHighlight);
        ConfigOptions options = new ConfigOptions();
        options.inventorySearchHighlight = false;
        assertFalse(ConfigOptions.fromJson(options.toJson()).inventorySearchHighlight);
        options.inventorySearchHighlight = true;
        assertEquals(true, ConfigOptions.fromJson(options.toJson()).inventorySearchHighlight);
    }

    @Test
    void migratesLegacyNumericValues() {
        ConfigOptions options = ConfigOptions.fromJson("""
                {"indicatorPosition":1,"themeIndex":6,"tooltipPosition":4}
                """);

        assertEquals(ConfigOptions.IndicatorPosition.LEFT, options.indicatorPosition);
        assertEquals(ConfigOptions.Theme.VALHALLA, options.theme);
        assertEquals(ConfigOptions.TooltipPosition.BOTTOM_RIGHT, options.tooltipPosition);
    }

    @Test
    void invalidEnumValuesFallBackToDefaults() {
        ConfigOptions options = ConfigOptions.fromJson("""
                {"indicatorPosition":"SIDEWAYS","theme":"UNKNOWN","tooltipPosition":99}
                """);

        assertEquals(ConfigOptions.IndicatorPosition.RIGHT, options.indicatorPosition);
        assertEquals(ConfigOptions.Theme.DARKNESS, options.theme);
        assertEquals(ConfigOptions.TooltipPosition.TOP_CENTER, options.tooltipPosition);
    }

    @Test
    void writesNamedValuesInsteadOfMagicNumbers() {
        ConfigOptions options = new ConfigOptions();
        String json = options.toJson();

        assertFalse(json.contains("themeIndex"));
        assertEquals(ConfigOptions.Theme.DARKNESS, ConfigOptions.fromJson(json).theme);
    }

    @Test
    void rejectsMalformedJsonSoLoadCanRestoreDefaults() {
        assertThrows(RuntimeException.class, () -> ConfigOptions.fromJson("{broken"));
    }

    @Test
    void recipeBookmarksRoundTripWithoutReplacingItemFavorites() {
        ConfigOptions options = new ConfigOptions();
        options.recipeFavorites = java.util.List.of("minecraft:oak_planks|{}");
        options.savedRecipeBookmarks = java.util.List.of("{\"key\":\"minecraft:oak_planks\",\"id\":\"minecraft:oak_planks\",\"output\":{\"id\":\"minecraft:oak_planks\",\"count\":4}}");
        options.favoriteFilter = "RECIPE";
        ConfigOptions restored = ConfigOptions.fromJson(options.toJson());
        assertEquals(options.recipeFavorites, restored.recipeFavorites);
        assertEquals(options.savedRecipeBookmarks, restored.savedRecipeBookmarks);
        assertEquals("RECIPE", restored.favoriteFilter);
    }

    @Test
    void olderConfigsKeepItemFavoritesAndStartWithAllFavoritesVisible() {
        ConfigOptions restored = ConfigOptions.fromJson("{\"recipeFavorites\":[\"minecraft:stone|{}\"]}");
        assertEquals(java.util.List.of("minecraft:stone|{}"), restored.recipeFavorites);
        assertEquals(java.util.List.of(), restored.savedRecipeBookmarks);
        assertEquals("GLOBAL", restored.favoriteFilter);
    }
}
