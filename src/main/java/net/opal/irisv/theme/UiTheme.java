package net.opal.irisv.theme;

import net.opal.irisv.option.ConfigOptions;

public record UiTheme(
        String name,

        // --- [TOOLTIP MAIN] ---
        int tooltip_backgroundColor,
        int tooltip_borderColor,
        int tooltip_titleColor,

        // --- [BLOCK PREVIEW & STATE] ---
        int block_listTextColor,
        String block_countNormalFormat,
        String block_countOverstackFormat,
        int block_countNormalHex,
        int block_countOverstackHex,
        int block_extraInfoColor,
        String block_extraInfoFormat,
        int block_stateTextColor,
        String block_stateTextFormat,
        String block_stateItemFormat,    // Nouveau : Format pour les items au sol

        // --- [TOOLS STATUS] ---
        String status_ok,
        String status_error,
        String status_warning,

        // --- [PROGRESS BAR] ---
        int progress_bar_empty,
        int progress_bar_ready,
        int progress_bar_warn,
        int progress_bar_error,
        int progress_bar_creative,
        int progress_bar_finish_white,

        // --- [GUI MENU] ---
        int gui_barColor,
        int gui_lineColor,
        int gui_bgOverlay,
        int gui_separatorLine,
        String gui_onColor,
        String gui_offColor,
        String gui_valColor,

        // --- [MISC] ---
        int mod_name_color
) {

    // --- THÈME : DARKNESS (Classique) ---
    public static final UiTheme DARKNESS = new UiTheme(
            "Darkness",
            0xAA000000, 0xCC333333, 0xFFFFFF,
            0xAAAAAA, "§7", "§6", 0xFFFFFF, 0xFFAA00, 0xFFFFFF, "§8", 0xFFFFFF, "§7", "§e§o",
            "§a✔", "§c✘", "§e!",
            0xFF1A1A1A, 0xFF50C878, 0xFFFFD700, 0xFFFF4545, 0xFF708090, 0xFFFFFF,
            0x440A0A10, 0x33A0C0FF, 0x15000000, 0x1AFFFFFF, "§b", "§7", "§f",
            0xFFFFFF
    );

    // --- THÈME : FROST (Interface Polaire & Cyan) ---
    public static final UiTheme FROST = new UiTheme(
            "Frost",
            0xCC050C18, 0xFF33A0FF, 0xCEEFFF,
            0xA0CADB, "§b", "§3", 0xA0E0FF, 0x00CED1, 0xCEEFFF, "§b", 0xCEEFFF, "§b", "§b§o",
            "§b✔", "§c✘", "§e!",
            0xFF0A141E, 0xFF00FBFF, 0xFFFFD700, 0xFFFF4545, 0xFF708090, 0xFFFFFFFF,
            0x66050C14, 0x8833A0FF, 0x2200050A, 0x22FFFFFF, "§b", "§1", "§f",
            0x33A0FF
    );

    /**
     * Récupère le thème actuel basé sur l'index de configuration.
     */
    public static UiTheme getCurrent() {
        ConfigOptions config = ConfigOptions.getInstance();
        return (config.themeIndex == 1) ? FROST : DARKNESS;
    }
}