package net.opal.irisv.tooltips.helpers;

import java.util.HashMap;
import java.util.Map;

public class TooltipColorManager {
    private static final Map<String, Integer> MANUAL_COLORS = new HashMap<>();

    static {
        // --- CONFIGURATION MANUELLE ---
        MANUAL_COLORS.put("minecraft", 0x5555FF);      // Bleu
        MANUAL_COLORS.put("create", 0xFFB43C);         // Doré
        MANUAL_COLORS.put("twilightforest", 0x00D413); // Vert
        MANUAL_COLORS.put("botania", 0x00FF00);        // Vert Flashy
        MANUAL_COLORS.put("mekanism", 0x1ED7FF);       // Cyan
        MANUAL_COLORS.put("eden", 0xFF55FF);           // Rose Eden
    }

    public static int getModColor(String modId) {
        if (modId == null) return 0xFFAA00; // Orange par défaut si erreur

        String id = modId.toLowerCase();

        // 1. Priorité au manuel
        if (MANUAL_COLORS.containsKey(id)) {
            return MANUAL_COLORS.get(id);
        }

        // 2. Fallback automatique par HashCode (Zéro orange générique)
        return generateHashColor(id);
    }

    private static int generateHashColor(String id) {
        int hash = id.hashCode();

        // On force des valeurs de luminosité pour que ce soit lisible sur fond sombre
        int r = Math.max(130, (hash & 0xFF0000) >> 16);
        int g = Math.max(130, (hash & 0x00FF00) >> 8);
        int b = Math.max(130, (hash & 0x0000FF));

        return (r << 16) | (g << 8) | b;
    }
}
