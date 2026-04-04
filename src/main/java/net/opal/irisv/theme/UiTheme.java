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
            "§a✔", "§c✘", "§e!",
            0xFF0A141E, 0xFF00FBFF, 0xFFFFD700, 0xFFFF4545, 0xFF708090, 0xFFFFFFFF,
            0x66050C14, 0x8833A0FF, 0x2200050A, 0x22FFFFFF, "§b", "§1", "§f",
            0x33A0FF
    );

    // --- 1. ELDER (Parchemin) ---
    public static final UiTheme ELDER = new UiTheme("Elder",
            0xEE2D281F, 0xFFA18055, 0xFFE6D29D,
            0xC5B38F, "§6", "§c", 0xD4C29F, 0xBC5B3F, 0xE6D29D, "§7", 0xE6D29D, "§6", "§6§o",
            "§a✔", "§c✘", "§e!", // Tes symboles
            0xFF1A1612, 0xFF967D32, 0xFF7D5A14, 0xFF5A1414, 0xFF4A4A4A, 0xFFE6D29D,
            0xAA2D281F, 0xFFA18055, 0x44000000, 0x22FFFFFF, "§6", "§8", "§e", 0xFFA18055
    );

    // --- 2. ABYSS (Obsidienne) ---
    public static final UiTheme ABYSS = new UiTheme("Abyss",
            0xDD0A050A, 0xFF4D004D, 0xFFD18BFF,
            0xA37EB5, "§d", "§5", 0xD18BFF, 0x8A2BE2, 0xD18BFF, "§d", 0xD18BFF, "§d", "§5§o",
            "§a✔", "§c✘", "§e!", // Tes symboles
            0xFF0A000A, 0xFF8A2BE2, 0xFF4B0082, 0xFF2E001F, 0xFF483D8B, 0xFFD18BFF,
            0xCC0A050A, 0xFF4D004D, 0x66000000, 0x11FF00FF, "§d", "§5", "§f", 0xFF8A2BE2
    );

    // --- 3. FOREST (Druidique) ---
    public static final UiTheme FOREST = new UiTheme("Forest",
            0xDD0D160D, 0xFF3D553D, 0xFFA4C4A4,
            0x8BA38B, "§2", "§a", 0xA4C4A4, 0x556B2F, 0xA4C4A4, "§2", 0xA4C4A4, "§2", "§a§o",
            "§a✔", "§c✘", "§e!", // Tes symboles
            0xFF0D0D0D, 0xFF2E8B57, 0xFF556B2F, 0xFF8B4513, 0xFF708090, 0xFFA4C4A4,
            0xCC0D160D, 0xFF3D553D, 0x33001A00, 0x1100FF00, "§a", "§2", "§f", 0xFF2E8B57
    );

    // --- 4. CRIMSON (Vampirique) ---
    public static final UiTheme CRIMSON = new UiTheme("Crimson",
            0xEE1A0000, 0xFF800000, 0xFFFF3333,
            0xA04040, "§c", "§4", 0xFF6666, 0x8B0000, 0xFF3333, "§c", 0xFF3333, "§c", "§4§o",
            "§a✔", "§c✘", "§e!", // Tes symboles
            0xFF120000, 0xFF8B0000, 0xFF4A0000, 0xFF2A0000, 0xFF363636, 0xFFFF3333,
            0xBB1A0000, 0xFF800000, 0x77000000, 0x22FF0000, "§c", "§4", "§f", 0xFF8B0000
    );

    // --- 5. VALHALLA (Or & Marbre) ---
    public static final UiTheme VALHALLA = new UiTheme("Valhalla",
            0xEE080B1A, 0xFFFFD700, 0xFFF5E050, // Fond Bleu Nuit profond, Bordure Or vif
            0xE0C060, "§e", "§6", 0xFFD700, 0xC0A030, 0xF5E050, "§e", 0xF5E050, "§e", "§e§o",
            "§a✔", "§c✘", "§e!", // Tes symboles
            0xFF050712, 0xFFDAA520, 0xFFB8860B, 0xFF8B4513, 0xFF708090, 0xFFF5E050,
            0xDD080B1A, 0xFFFFD700, 0x44000000, 0x22FFFFFF, "§e", "§6", "§f", 0xFFFFD700
    );

    /**
     * Récupère le thème actuel basé sur l'index de configuration.
     */
    /**
     * Récupère le thème actuel basé sur l'index de configuration.
     */
    public static UiTheme getCurrent() {
        ConfigOptions config = ConfigOptions.getInstance();

        // On liste tous les thèmes dans l'ordre de ton menu
        UiTheme[] themes = {
                DARKNESS, // 0
                FROST,    // 1
                ELDER,    // 2
                ABYSS,    // 3
                FOREST,   // 4
                CRIMSON,  // 5
                VALHALLA  // 6
        };

        // Garde-fou : On vérifie que l'index est bien dans les limites du tableau
        if (config.themeIndex >= 0 && config.themeIndex < themes.length) {
            return themes[config.themeIndex];
        }

        // Par défaut, si l'index est bizarre, on retourne le thème Darkness
        return DARKNESS;
    }
}