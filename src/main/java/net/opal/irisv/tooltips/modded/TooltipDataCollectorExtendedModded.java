package net.opal.irisv.tooltips.modded;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public class TooltipDataCollectorExtendedModded {

    public static void addModdedData(List<String> info, BlockState state, BlockEntity be) {
        if (be == null) return;

        // Récupération des données persistantes (NBT)
        var data = be.getPersistentData();

        // --- SECTION : MELTING SUPPORT (Ton bloc custom) ---
        if (data.contains("melting_support_current_fuel")) {
            double fuelValue = data.getDouble("melting_support_current_fuel");
            processMeltingSupport(info, fuelValue);
        }
    }

    private static void processMeltingSupport(List<String> info, double fuelValue) {
        if (fuelValue > 0) {
            int totalSeconds = (int) (fuelValue / 20);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            String timeStr = (minutes > 0) ? String.format("%dm %ds", minutes, seconds) : seconds + "s";
            int percent = (int) ((fuelValue / 20000.0) * 100);

            info.add("Fuel: §6" + timeStr + " §7(" + percent + "%)");
        } else {
            info.add("Fuel: §cNo Fuel");
        }
    }
}