package net.opal.irisv.tooltips.modded;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor; // Nouveau standard

import java.util.List;

public class MeltingSupportTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // On vérifie si l'entité de bloc existe
        return be != null;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        // Récupération du NBT synchronisé
        CompoundTag data = accessor.serverData();

        // On vérifie si la clé de fuel est présente dans les données reçues
        if (data != null && data.contains("melting_support_current_fuel")) {
            double fuelValue = data.getDouble("melting_support_current_fuel");

            if (fuelValue > 0) {
                // Conversion ticks -> temps (20 ticks = 1 seconde)
                int totalSeconds = (int) (fuelValue / 20);
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;

                String timeStr = (minutes > 0) ? String.format("%dm %ds", minutes, seconds) : seconds + "s";

                // Calcul du pourcentage (basé sur ton max de 20000)
                int percent = (int) Math.min(100, (fuelValue / 20000.0) * 100);

                // Barre de progression visuelle
                String progressBar = getProgressBar(percent);

                info.add("Fuel: §6" + timeStr + " §7(" + percent + "%)");
                info.add(progressBar);
            } else {
                info.add("Fuel: §cNo Fuel");
            }
        }
    }

    private String getProgressBar(int percent) {
        int segments = 10;
        int filled = percent / 10;
        // Utilisation de repeat pour construire la barre : [::::::....]
        return "§8[" + "§6:".repeat(filled) + "§7:".repeat(segments - filled) + "§8]";
    }
}