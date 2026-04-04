package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.ArrayList;
import java.util.List;

public class ComposterTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // On vérifie si le bloc possède la propriété de niveau de compostage
        return state.hasProperty(BlockStateProperties.LEVEL_COMPOSTER);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            BlockState state = accessor.state();
            int level = state.getValue(BlockStateProperties.LEVEL_COMPOSTER);

            // Le niveau 8 signifie que le compost est prêt (couche de surface blanche)
            if (level >= 8) {
                info.add("§aPrêt à récolter");
                List<ItemStack> preview = new ArrayList<>();
                preview.add(new ItemStack(Items.BONE_MEAL));
                accessor.setPreviewItems(preview);

            } else {
                // Calcul du pourcentage (0 à 7 couches avant récolte)
                int percentage = (level * 100) / 7;
                info.add("§7Compost: " + " §f" + percentage + "%");
            }
        }
    }
}