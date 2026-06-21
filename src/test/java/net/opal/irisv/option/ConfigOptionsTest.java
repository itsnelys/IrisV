package net.opal.irisv.option;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigOptionsTest {
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
}
