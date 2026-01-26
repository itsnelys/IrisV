package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.List;

public class StructureBlockTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.STRUCTURE_BLOCK);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        CompoundTag nbt = accessor.serverData();
        if (nbt == null || !nbt.contains("name")) return;

        String structureName = nbt.getString("name");

        if (structureName.isEmpty()) {
            info.add("§8(Empty)");
            return;
        }

        if (Screen.hasControlDown()) {
            info.add("§bStructure Name:");
            info.add("§7" + structureName);

            // On peut aussi afficher le mode (SAVE, LOAD, CORNER, DATA)
            if (nbt.contains("mode")) {
                info.add("§8Mode: §7" + nbt.getString("mode"));
            }
        } else {
            info.add("§8Hold §f[CTRL] §8for details");
        }
    }
}