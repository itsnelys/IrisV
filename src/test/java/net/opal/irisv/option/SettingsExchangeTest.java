package net.opal.irisv.option;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingsExchangeTest {
    @Test void exportsSettingsWithoutFavoritesOrNavigationState() {
        ConfigOptions source = new ConfigOptions();
        source.recipeFavorites.add("private-item");
        source.savedRecipeBookmarks.add("private-recipe");
        source.favoriteOrder.add("private-order");
        String exported = source.exportSettings();
        assertFalse(exported.contains("private-"));
        var settings = JsonParser.parseString(exported).getAsJsonObject().getAsJsonObject("settings");
        assertFalse(settings.has("recipePage"));
        assertFalse(settings.has("favoriteFilter"));
        assertTrue(settings.has("theme"));
    }

    @Test void importsSettingsWhileKeepingAllFavoritesAndCurrentState() {
        ConfigOptions source = new ConfigOptions();
        source.theme = ConfigOptions.Theme.CRIMSON;
        source.disabledRecipeHudCategories.add("chest");
        ConfigOptions current = new ConfigOptions();
        current.recipeFavorites.add("diamond");
        current.savedRecipeBookmarks.add("recipe-json");
        current.favoriteOrder.add("item:diamond");
        current.favoriteFilter = "ITEM";
        current.recipePage = 7;
        ConfigOptions merged = current.mergeSettings(source.exportSettings());
        assertEquals(ConfigOptions.Theme.CRIMSON, merged.theme);
        assertEquals(List.of("chest"), merged.disabledRecipeHudCategories);
        assertEquals(current.recipeFavorites, merged.recipeFavorites);
        assertEquals(current.savedRecipeBookmarks, merged.savedRecipeBookmarks);
        assertEquals(current.favoriteOrder, merged.favoriteOrder);
        assertEquals("ITEM", merged.favoriteFilter);
        assertEquals(7, merged.recipePage);
        assertEquals(ConfigOptions.Theme.DARKNESS, current.theme);
    }

    @Test void hostileFavoriteFieldsCannotOverwriteLocalFavorites() {
        ConfigOptions current = new ConfigOptions();
        current.recipeFavorites = new ArrayList<>(List.of("keep"));
        ConfigOptions merged = current.mergeSettings(document("\"recipeFavorites\":[],\"savedRecipeBookmarks\":[],\"favoriteOrder\":[],\"theme\":\"FROST\""));
        assertEquals(List.of("keep"), merged.recipeFavorites);
    }

    @Test void partialImportPreservesUnspecifiedOptions() {
        ConfigOptions current = new ConfigOptions();
        current.compactMode = true;
        assertTrue(current.mergeSettings(document("\"theme\":\"FROST\"")).compactMode);
    }

    @Test void rejectsInvalidTypesValuesAndVersionsWithoutMutation() {
        ConfigOptions current = new ConfigOptions();
        for (String fields : List.of("\"theme\":\"TYPO\"", "\"compactMode\":\"true\"",
                "\"disabledRecipeHudCategories\":[false]", "\"unknown\":true")) {
            assertThrows(RuntimeException.class, () -> current.mergeSettings(document(fields)));
        }
        assertThrows(RuntimeException.class, () -> current.mergeSettings("{}"));
        assertThrows(RuntimeException.class, () -> current.mergeSettings("broken"));
        assertThrows(RuntimeException.class, () -> current.mergeSettings(document("\"compactMode\":true").replace("\"version\":1", "\"version\":2")));
        assertEquals(ConfigOptions.Theme.DARKNESS, current.theme);
        assertFalse(current.compactMode);
    }

    private static String document(String settings) {
        return "{\"format\":\"irisv-settings\",\"version\":1,\"settings\":{" + settings + "}}";
    }
}
