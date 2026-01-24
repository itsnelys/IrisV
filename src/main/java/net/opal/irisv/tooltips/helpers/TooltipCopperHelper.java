package net.opal.irisv.tooltips.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TooltipCopperHelper {

    public record CopperInfo(String label, int percent, String color, boolean isWaxed) {}

    public static CopperInfo getInfo(Level level, BlockState state, BlockPos pos) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (!id.contains("copper")) return null;

        boolean isWaxed = id.contains("waxed");

        // 1. Détermination du stade de base
        int stage = 0;
        String label = "Normal";
        String color = "§a";

        if (id.contains("oxidized")) { stage = 3; label = "Oxidized"; color = "§2"; }
        else if (id.contains("weathered")) { stage = 2; label = "Weathered"; color = "§e"; }
        else if (id.contains("exposed")) { stage = 1; label = "Exposed"; color = "§6"; }

        // 2. CALCUL RÉALISTE (Position + Temps)
        // Chaque bloc a un décalage unique basé sur sa position (Seed)
        long posSeed = (long) pos.getX() * 3121231L ^ (long) pos.getZ() * 1161231L ^ (long) pos.getY();

        // On récupère le temps du monde (Game Time)
        // Diviser par 1000 rend la progression très lente (environ 1% toutes les 50 secondes)
        long timeOffset = (level.getGameTime() + posSeed) / 1000L;

        // On crée une progression de 0 à 24 à l'intérieur du stade
        int internalProgress = (int) (timeOffset % 25);

        // Résultat final : (Stade * 25) + progression
        int finalPercent = (stage * 25) + internalProgress;

        // Si le bloc est ciré ou oxydé au max, on stabilise le pourcentage
        if (isWaxed || stage == 3) {
            // Un bloc oxydé reste entre 95 et 100%
            if (stage == 3) finalPercent = 95 + (int)(posSeed % 6);
        }

        return new CopperInfo(label, finalPercent, color, isWaxed);
    }
}