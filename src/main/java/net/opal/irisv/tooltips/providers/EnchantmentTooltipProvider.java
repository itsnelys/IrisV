package net.opal.irisv.tooltips.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.network.ClientDataCache;

import java.util.List;

public class EnchantmentTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // Version plus sûre pour 1.21
        return state.is(Blocks.ENCHANTING_TABLE) || state.is(Blocks.BOOKSHELF) || state.getBlock().getDescriptionId().contains("bookshelf");
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        // 1. Récupération du cache (Même si peu utilisé ici, c'est pour l'unification)
        CompoundTag data = ClientDataCache.get(pos);

        // 2. CAS 1 : La Table d'enchantement
        if (state.is(Blocks.ENCHANTING_TABLE)) {
            float totalPower = calculateEnchantPower(level, pos);
            info.add("Enchant Power: §b" + formatPower(totalPower));

            if (totalPower < 15) {
                info.add("§8(Max is 15)");
            } else if (totalPower > 15) {
                info.add("§d(Bonus Power!)");
            }
        }
        // 3. CAS 2 : Une Bibliothèque
        else {
            // On utilise level et pos pour avoir le vrai bonus (important pour les mods)
            float power = state.getEnchantPowerBonus(level, pos);
            if (power > 0) {
                info.add("Ench Power: §f+" + formatPower(power));
            }
        }
    }

    private float calculateEnchantPower(Level level, BlockPos tablePos) {
        float totalPower = 0;
        // Le calcul reste local car le client voit les blocs alentours
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                if (dz != 0 || dx != 0) {
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
        return (power == (int)power) ? String.valueOf((int)power) : String.format("%.1f", power);
    }
}