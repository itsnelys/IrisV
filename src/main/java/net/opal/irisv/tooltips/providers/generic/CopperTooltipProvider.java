package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.tooltips.helpers.TooltipCopperHelper;

import java.util.List;

public class CopperTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        String id = state.getBlock().getDescriptionId();
        // On s'active pour le cuivre MAIS on ignore si c'est un minerai (ore)
        return id.contains("copper") && !id.contains("ore") && !id.contains("raw") && !id.contains("chest") && !id.contains("barrel");
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            // Extraction des données de l'accessor
            // Plus besoin de ClientDataCache car le cuivre est géré par les BlockStates
            var copper = TooltipCopperHelper.getInfo(accessor.level(), accessor.state(), accessor.pos());

            if (copper != null) {
                info.add("Oxidation: " + copper.color() + copper.percent() + "% ");

                if (copper.isWaxed()) {
                    info.add("§6STATUS: §eWAXED §7(Protected)");
                } else if (copper.percent() < 95) {
                    info.add("§8Status: §7Oxidizing...");
                }
            }
        }
    }
}