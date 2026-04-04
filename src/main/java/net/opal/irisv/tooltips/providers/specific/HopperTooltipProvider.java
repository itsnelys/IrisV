package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class HopperTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // Uniquement pour les entonnoirs
        return state.getBlock() instanceof HopperBlock;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            // 1. Gestion de l'état Redstone (Hopper Lock)
            // La propriété ENABLED est fausse quand le hopper est alimenté par redstone
            if (accessor.state().hasProperty(BlockStateProperties.ENABLED)) {
                if (!accessor.state().getValue(BlockStateProperties.ENABLED)) {
                    info.add("§cVerrouillé (Redstone)");
                }
            }
        }
    }
}