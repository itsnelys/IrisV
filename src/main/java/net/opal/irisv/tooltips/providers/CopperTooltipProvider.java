package net.opal.irisv.tooltips.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.network.ClientDataCache;
import net.opal.irisv.tooltips.helpers.TooltipCopperHelper;

import java.util.List;

public class CopperTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        String id = state.getBlock().getDescriptionId();
        // On s'active pour le cuivre MAIS on ignore si c'est un minerai (ore)
        return id.contains("copper") && !id.contains("ore");
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        // Unification : récupération du cache (même si peu utilisé ici)
        CompoundTag data = ClientDataCache.get(pos);

        var copper = TooltipCopperHelper.getInfo(level, state, pos);

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