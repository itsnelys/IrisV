package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor;

import java.util.List;

public class InteractionTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.hasProperty(BlockStateProperties.LEVEL_COMPOSTER) ||
                state.hasProperty(BlockStateProperties.LEVEL_CAULDRON) ||
                state.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES) ||
                state.hasProperty(BlockStateProperties.BITES) ||
                state.hasProperty(BlockStateProperties.CANDLES) ||
                state.hasProperty(BlockStateProperties.PICKLES);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        // On récupère le state directement depuis l'accessor
        BlockState state = accessor.state();

        // 1. Composteur
        if (state.hasProperty(BlockStateProperties.LEVEL_COMPOSTER)) {
            int levelComp = state.getValue(BlockStateProperties.LEVEL_COMPOSTER);
            info.add("Composter: §2" + levelComp + "/8");
        }

        // 2. Chaudron (Eau, Poudre de neige, lave)
        if (state.hasProperty(BlockStateProperties.LEVEL_CAULDRON)) {
            info.add("Level: §b" + state.getValue(BlockStateProperties.LEVEL_CAULDRON) + "/3");
        }

        // 3. Ancre de réapparition
        if (state.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)) {
            info.add("Charges: §d" + state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES) + "/4");
        }

        // 4. Gâteau (Bites / Parts mangées)
        if (state.hasProperty(BlockStateProperties.BITES)) {
            info.add("Bites: §f" + state.getValue(BlockStateProperties.BITES) + "/6");
        }

        // 5. Bougies
        if (state.hasProperty(BlockStateProperties.CANDLES)) {
            info.add("Candles: §f" + state.getValue(BlockStateProperties.CANDLES));
        }

        // 6. Cornichons de mer
        if (state.hasProperty(BlockStateProperties.PICKLES)) {
            info.add("Pickles: §f" + state.getValue(BlockStateProperties.PICKLES));
        }
    }
}