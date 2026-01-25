package net.opal.irisv.tooltips.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.network.ClientDataCache;

import java.util.List;

public class EnderFrameTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.END_PORTAL_FRAME);
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        // Optionnel : On récupère le cache au cas où (cohérence de code)
        CompoundTag data = ClientDataCache.get(pos);

        if (state.hasProperty(BlockStateProperties.EYE)) {
            boolean hasEye = state.getValue(BlockStateProperties.EYE);

            // Affichage propre avec couleurs
            String status = hasEye ? "§aInserted" : "§7Empty";
            info.add("End Eye: " + status);

            if (!hasEye) {
                info.add("§8(Needs eye to activate)");
            }
        }
    }
}