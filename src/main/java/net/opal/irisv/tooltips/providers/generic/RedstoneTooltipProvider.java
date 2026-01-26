package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor;

import java.util.List;

public class RedstoneTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // 1. On exclut les blocs qui ne doivent JAMAIS afficher de status redstone
        if (state.is(net.minecraft.world.level.block.Blocks.LECTERN) ||
                state.getBlock() instanceof net.minecraft.world.level.block.AbstractSkullBlock) {
            return false;
        }

        // 2. Ta logique de détection habituelle
        return state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER) ||
                state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED) ||
                state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DELAY) ||
                state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.MODE_COMPARATOR);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        BlockState state = accessor.state();

        // 1. Puissance (Fils de redstone, Plaques de pression, etc.)
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

        // 3. État Alimenté (Leviers, Lampes, Boutons, Rails)
        if (state.hasProperty(BlockStateProperties.POWERED)) {
            boolean isPowered = state.getValue(BlockStateProperties.POWERED);
            info.add("Status: " + (isPowered ? "§aON" : "§cOFF"));
        }

        // 4. Mode du Comparateur (Soustraction vs Comparaison)
        if (state.hasProperty(BlockStateProperties.MODE_COMPARATOR)) {
            ComparatorMode mode = state.getValue(BlockStateProperties.MODE_COMPARATOR);
            String modeName = (mode == ComparatorMode.SUBTRACT) ? "Subtraction" : "Comparison";
            info.add("Mode: §e" + modeName);
        }
    }
}