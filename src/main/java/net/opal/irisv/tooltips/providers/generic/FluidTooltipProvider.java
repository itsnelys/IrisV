package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.network.ClientDataCache;

import java.util.List;

public class FluidTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // S'active si le bloc contient un fluide (Source ou Coulant)
        return !state.getFluidState().isEmpty();
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        // Unification : récupération du cache (utile pour des fluides moddés avec NBT)
        CompoundTag data = ClientDataCache.get(pos);

        FluidState fluid = state.getFluidState();

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
    }
}