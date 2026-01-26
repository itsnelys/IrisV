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
        // Récupération des données simplifiée via l'accessor
        BlockState state = accessor.state();
        FluidState fluid = state.getFluidState();

        // On remplace ClientDataCache par le serverData de l'accessor
        CompoundTag data = accessor.serverData();

        if (fluid.isSource()) {
            info.add("§bType: §fSource");
        } else {
            // Niveau de 1 à 8 (où 8 est la source, 1 le plus bas)
            int levelValue = fluid.getAmount();
            info.add("§bFlow: §fLevel " + levelValue + "/8");
        }

        // Détection de la température (Lave, Fluides moddés)
        if (fluid.getFluidType().getTemperature() >= 1000) {
            info.add("§cState: §lExtremely Hot");
        }

        // Exemple : Si un fluide moddé a des données NBT spéciales (pureté, etc.)
        if (data != null && data.contains("FluidExtraInfo")) {
            info.add("§dInfo: §f" + data.getString("FluidExtraInfo"));
        }
    }
}