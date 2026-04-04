package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor; // Utilisation de ton nouvel accessor
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class NoteblockTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.hasProperty(BlockStateProperties.NOTE) || state.is(Blocks.CALIBRATED_SCULK_SENSOR);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            BlockState state = accessor.state();

            // 1. Gestion du Note Block (via BlockState)
            if (state.hasProperty(BlockStateProperties.NOTE)) {
                int note = state.getValue(BlockStateProperties.NOTE);
                info.add("Note: §e" + note);

                // Récupération de l'instrument (déterminé par le bloc en dessous)
                if (state.hasProperty(BlockStateProperties.NOTEBLOCK_INSTRUMENT)) {
                    String instrument = state.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT).getSerializedName();
                    // Mise en majuscule de la première lettre pour faire plus propre
                    info.add("Instrument: §7" + instrument.substring(0, 1).toUpperCase() + instrument.substring(1));
                }
            }
        }
    }
}