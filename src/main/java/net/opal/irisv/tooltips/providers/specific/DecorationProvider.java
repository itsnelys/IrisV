package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.item.ItemStack;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IEntityTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class DecorationProvider implements IEntityTooltipProvider {
    @Override
    public boolean isApplicable(Entity entity) {
        return entity instanceof ItemFrame || entity instanceof Painting;
    }

    @Override
    public void addTooltip(List<String> tooltip, Entity entity, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            if (entity instanceof ItemFrame frame) {
                ItemStack stack = frame.getItem();
                if (!stack.isEmpty()) {
                    // On affiche le nom de l'item dans le tooltip
                    accessor.getPreviewItems().add(stack);
                }
            } else if (entity instanceof Painting painting) {
                var variantHolder = painting.getVariant();
                var variant = variantHolder.value();

                // 1. Nom du motif (ex: Kebab, Alban...)
                String variantName = variantHolder.getRegisteredName();
                // On nettoie le nom si nécessaire (ex: "minecraft:kebab" -> "Kebab")
                if (variantName.contains(":")) {
                    variantName = variantName.split(":")[1];
                }
                tooltip.add("§7Motif: §e" + capitalize(variantName));

                // 2. Taille du tableau (Largeur x Hauteur)
                int width = variant.width();
                int height = variant.height();
                tooltip.add("§7Taille: §b" + width + "x" + height);
            }
        }
    }

    // Petite méthode utilitaire pour rendre ça propre
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}