package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class EnderFrameTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.END_PORTAL_FRAME);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            // Récupération de l'état du bloc via l'accessor
            BlockState state = accessor.state();

            if (state.hasProperty(BlockStateProperties.EYE)) {
                boolean hasEye = state.getValue(BlockStateProperties.EYE);

                // Affichage propre avec couleurs
                String status = hasEye ? "§aInserted" : "§7Empty";
                info.add("End Eye: " + status);

                if (!hasEye) {
                    info.add("§8(Empty)");
                }
            }
        }
    }
}