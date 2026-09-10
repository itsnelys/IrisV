package net.opal.irisv.recip;

import java.text.Normalizer;
import java.util.Locale;

public final class WikiSearch {
    private WikiSearch() {}
    public static boolean matches(String query, String text) {
        String normalized = normalize(text);
        for (String token : normalize(query).trim().split("\\s+")) {
            if (!normalized.contains(token)) return false;
        }
        return true;
    }
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
