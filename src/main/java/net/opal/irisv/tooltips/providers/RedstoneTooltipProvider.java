package net.opal.irisv.tooltips.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.network.ClientDataCache;

import java.util.List;

public class RedstoneTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.hasProperty(BlockStateProperties.POWER) ||
                state.hasProperty(BlockStateProperties.POWERED) ||
                state.hasProperty(BlockStateProperties.DELAY) ||
                state.hasProperty(BlockStateProperties.MODE_COMPARATOR);
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        // Optionnel pour la Redstone, mais bien pour l'unification du code
        CompoundTag data = ClientDataCache.get(pos);

        // 1. Puissance (Fils, Plaques, etc.)
        if (state.hasProperty(BlockStateProperties.POWER)) {
            int power = state.getValue(BlockStateProperties.POWER);
            String color = power > 0 ? "§c" : "§7";
            info.add("Power: " + color + power);
        }

        // 2. Délai (Répéteurs)
        if (state.hasProperty(BlockStateProperties.DELAY)) {
            int delay = state.getValue(BlockStateProperties.DELAY);
            info.add("Delay: §f" + delay + " ticks");
        }

        // 3. État ON/OFF (Leviers, Lampes, Boutons)
        if (state.hasProperty(BlockStateProperties.POWERED)) {
            boolean isPowered = state.getValue(BlockStateProperties.POWERED);
            info.add("Status: " + (isPowered ? "§aON" : "§cOFF"));
        }

        // 4. Mode du Comparateur
        if (state.hasProperty(BlockStateProperties.MODE_COMPARATOR)) {
            ComparatorMode mode = state.getValue(BlockStateProperties.MODE_COMPARATOR);
            String modeName = (mode == ComparatorMode.SUBTRACT) ? "Subtraction" : "Comparison";
            info.add("Mode: §e" + modeName);
        }
    }
}