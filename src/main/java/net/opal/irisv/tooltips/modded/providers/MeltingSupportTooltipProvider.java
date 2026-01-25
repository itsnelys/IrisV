package net.opal.irisv.tooltips.modded.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.List;

public class MeltingSupportTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // On s'active si le bloc possède l'entité et la clé NBT spécifique
        return be != null && be.getPersistentData().contains("melting_support_current_fuel");
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        CompoundTag data = net.opal.irisv.network.ClientDataCache.get(pos);
        double fuelValue = data.getDouble("melting_support_current_fuel");

        if (fuelValue > 0) {
            int totalSeconds = (int) (fuelValue / 20);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            String timeStr = (minutes > 0) ? String.format("%dm %ds", minutes, seconds) : seconds + "s";

            // Calcul du pourcentage (basé sur ton max de 20000)
            int percent = (int) Math.min(100, (fuelValue / 20000.0) * 100);

            // Petit ajout visuel : une barre de progression simple
            String progressBar = getProgressBar(percent);

            info.add("Fuel: §6" + timeStr + " §7(" + percent + "%)");
            info.add(progressBar);
        } else {
            info.add("Fuel: §cNo Fuel");
        }
    }

    private String getProgressBar(int percent) {
        int bars = percent / 10; // 10 segments
        return "§8[" + "§6:".repeat(bars) + "§7:".repeat(10 - bars) + "§8]";
    }
}