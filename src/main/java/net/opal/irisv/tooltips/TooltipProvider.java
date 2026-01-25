package net.opal.irisv.tooltips;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.ArrayList;
import java.util.List;

public class TooltipProvider {

    public static List<String> getAllData(Level level, BlockPos pos, BlockState state) {
        List<String> info = new ArrayList<>();
        BlockEntity be = level.getBlockEntity(pos);

        // On boucle sur TOUS les providers (Vanilla + Moddés)
        for (IBlockTooltipProvider provider : TooltipProviderRegistry.getProviders()) {
            try {
                if (provider.isApplicable(state, be)) {
                    provider.addTooltip(info, state, level, pos, be);
                }
            } catch (Exception e) {
                System.err.println("IrisV Tooltip Error in " + provider.getClass().getSimpleName() + " : " + e.getMessage());
            }
        }

        return info;
    }
}