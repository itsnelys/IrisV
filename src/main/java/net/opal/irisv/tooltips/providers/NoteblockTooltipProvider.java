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

public class NoteblockTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.hasProperty(BlockStateProperties.NOTE) || state.is(Blocks.CALIBRATED_SCULK_SENSOR);
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        // 1. Récupération du cache (Optionnel ici, mais bon pour la cohérence)
        CompoundTag data = ClientDataCache.get(pos);

        // 2. Gestion du Note Block (via BlockState)
        if (state.hasProperty(BlockStateProperties.NOTE)) {
            int note = state.getValue(BlockStateProperties.NOTE);
            info.add("Note: §e" + note);

            if (state.hasProperty(BlockStateProperties.NOTEBLOCK_INSTRUMENT)) {
                String instrument = state.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT).getSerializedName();
                info.add("Instrument: §7" + instrument.substring(0, 1).toUpperCase() + instrument.substring(1));
            }
        }

        // 3. Cas du Calibrated Sculk Sensor (si tu veux afficher sa fréquence)
        if (state.is(Blocks.CALIBRATED_SCULK_SENSOR)) {
            // Si tu avais des données spécifiques en NBT, tu les lirais dans 'data' ici
            info.add("§8(Calibrated Sensor)");
        }
    }
}