package net.opal.irisv.api;

import net.minecraft.nbt.CompoundTag;

/**
 * Utilitaire pour extraire facilement les données de cuisson
 * à partir du CompoundTag synchronisé par le serveur.
 */
public record IFurnaceAccessor(CompoundTag data) {

    // --- COMBUSTIBLE (FUEL) ---

    public int getBurnTime() {
        return data.getInt("BurnTime");
    }

    public int getBurnDuration() {
        return data.getInt("BurnDuration");
    }

    public boolean isBurning() {
        return getBurnTime() > 0;
    }

    /**
     * @return Valeur entre 0.0 et 1.0 représentant le fuel restant dans le cycle actuel.
     */
    public float getBurnProgress() {
        int time = getBurnTime();
        int duration = getBurnDuration();
        if (time <= 0) return 0f;
        // Fallback à 200 (charbon) si la durée totale est absente
        return Math.min(1.0f, (float) time / (duration > 0 ? duration : 200));
    }

    // --- CUISSON (COOKING) ---

    public int getCookTime() {
        return data.getInt("CookTime");
    }

    public int getCookTimeTotal() {
        return data.getInt("CookTimeTotal");
    }

    /**
     * @return Valeur entre 0.0 et 1.0 représentant la progression de la flèche.
     */
    public float getCookProgress() {
        int time = getCookTime();
        int total = getCookTimeTotal();
        if (total <= 0 || time <= 0) return 0f;
        return Math.min(1.0f, (float) time / total);
    }
}