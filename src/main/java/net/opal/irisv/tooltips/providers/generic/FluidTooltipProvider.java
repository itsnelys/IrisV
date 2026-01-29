package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor; // Utilisation du nouveau nom

import java.util.List;

public class FluidTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // S'active si le bloc contient un fluide (Source ou Coulant)
        return !state.getFluidState().isEmpty();
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        FluidState fluid = accessor.state().getFluidState();
        if (fluid.isEmpty()) return;

        // 1. Gestion intelligente Source vs Flow
        if (fluid.isSource()) {
            info.add("§bType: §3Source Block");
        } else {
            int level = fluid.getAmount();
            // On affiche le pourcentage de remplissage du bloc pour plus de clarté
            int percentage = (level * 100) / 8;
            info.add("§bFlowing: §f" + percentage + "%");
        }

        // 2. Propriétés physiques (Température / Densité)
        int temp = fluid.getFluidType().getTemperature();
        if (temp >= 1000) {
            info.add("§cState: §lExtremely Hot");
        } else if (temp <= 280) { // Environ 7°C
            info.add("§bState: §lFreezing");
        }

        // 3. Données NBT (Server Data)
        CompoundTag data = accessor.serverData();
        if (data != null && data.contains("FluidExtraInfo")) {
            info.add("§dInfo: §f" + data.getString("FluidExtraInfo"));
        }
    }
}