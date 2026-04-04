package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class EnchantmentTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.ENCHANTING_TABLE) || state.is(Blocks.BOOKSHELF) || state.getBlock().getDescriptionId().contains("bookshelf");
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            BlockState state = accessor.state();
            Level level = accessor.level();
            BlockPos pos = accessor.pos();

            // 1. CASE 1: Enchanting Table (Global Calculation)
            if (state.is(Blocks.ENCHANTING_TABLE)) {
                float totalPower = calculateEnchantPower(level, pos);
                info.add("Enchanting Power: §b" + formatPower(totalPower));

                if (totalPower < 15.0F) {
                    // Simple progress message
                    info.add("§8(" + formatPower(totalPower) + "/15 for full potential)");
                } else if (totalPower == 15.0F) {
                    // Exactly 15
                    info.add("§aFull potential reached");
                } else {
                    // More than 15
                    info.add("§dBonus power §7(+" + formatPower(totalPower - 15.0F) + ")");
                }
            }
            // 2. CAS 2 : Une Bibliothèque (Calcul individuel)
            else {
                float power = state.getEnchantPowerBonus(level, pos);
                if (power > 0) {
                    info.add("Ench Power: §f+" + formatPower(power));
                }
            }
        }
    }
    private float calculateEnchantPower(Level level, BlockPos tablePos) {
        float totalPower = 0;
        // Scan du périmètre 5x5 autour de la table pour trouver les bibliothèques
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                if (dz != 0 || dx != 0) {
                    // Vérifie s'il y a de l'air entre la table et la bibliothèque (requis pour que ça marche)
                    if (level.isEmptyBlock(tablePos.offset(dx, 0, dz)) && level.isEmptyBlock(tablePos.offset(dx, 1, dz))) {
                        totalPower += level.getBlockState(tablePos.offset(dx * 2, 0, dz * 2)).getEnchantPowerBonus(level, tablePos.offset(dx * 2, 0, dz * 2));
                        totalPower += level.getBlockState(tablePos.offset(dx * 2, 1, dz * 2)).getEnchantPowerBonus(level, tablePos.offset(dx * 2, 1, dz * 2));

                        if (dx != 0 && dz != 0) {
                            totalPower += level.getBlockState(tablePos.offset(dx * 2, 0, dz)).getEnchantPowerBonus(level, tablePos.offset(dx * 2, 0, dz));
                            totalPower += level.getBlockState(tablePos.offset(dx * 2, 1, dz)).getEnchantPowerBonus(level, tablePos.offset(dx * 2, 1, dz));
                            totalPower += level.getBlockState(tablePos.offset(dx, 0, dz * 2)).getEnchantPowerBonus(level, tablePos.offset(dx, 0, dz * 2));
                            totalPower += level.getBlockState(tablePos.offset(dx, 1, dz * 2)).getEnchantPowerBonus(level, tablePos.offset(dx, 1, dz * 2));
                        }
                    }
                }
            }
        }
        return totalPower;
    }

    private String formatPower(float power) {
        // Supprime le .0 si le nombre est entier pour un affichage plus propre
        return (power == (int)power) ? String.valueOf((int)power) : String.format("%.1f", power);
    }
}