package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class AgricultureTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return isVerticalPlant(state) || hasAgeProperty(state) || state.hasProperty(BlockStateProperties.MOISTURE);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            // Extraction des données de l'accessor
            BlockState state = accessor.state();
            Level level = accessor.level();
            BlockPos pos = accessor.pos();
            Block block = state.getBlock();

            // 1. LOGIQUE VERTICALE (Hauteur & Potentiel)
            if (isVerticalPlant(state)) {
                int totalHeight = getTotalHeight(level, pos, block);
                info.add("Total Height: §e" + totalHeight + " blocks");

                if (state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS)) {
                    if (totalHeight >= 3) info.add("Growth: §cMax Height");
                    else info.add("Potential: §b+" + (3 - totalHeight) + " blocks");
                } else if (isComplexPlant(block)) {
                    BlockPos headPos = findHeadBlock(level, pos, state);
                    BlockState headState = level.getBlockState(headPos);
                    if (headState.hasProperty(BlockStateProperties.AGE_25)) {
                        int age = headState.getValue(BlockStateProperties.AGE_25);
                        info.add(age >= 25 ? "Growth: §cMax" : "Potential: §b+" + (25 - age) + " blocks");
                    }
                }
            }
            // 2. LOGIQUE CLASSIQUE (Pourcentages %)
            else {
                checkAge(state, info);
            }

            // 3. HUMIDITÉ DU SOL
            if (state.hasProperty(BlockStateProperties.MOISTURE)) {
                int m = state.getValue(BlockStateProperties.MOISTURE);
                int percent = (int) ((m / 7.0f) * 100);
                String color = percent > 0 ? "§b" : "§6";
                info.add("Moisture: " + color + percent + "%");
            }
        }
    }
    // --- MÉTHODES UTILITAIRES (Inchangées, mais utilisent les bons types) ---

    private boolean isVerticalPlant(BlockState state) {
        return state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING) ||
                state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) ||
                state.is(Blocks.TWISTING_VINES) || state.is(Blocks.TWISTING_VINES_PLANT) ||
                state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT) ||
                state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT) ||
                state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS) || state.is(Blocks.VINE);
    }

    private boolean hasAgeProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equalsIgnoreCase("age")) return true;
        }
        return false;
    }

    private void checkAge(BlockState state, List<String> info) {
        Block block = state.getBlock();
        if (isComplexPlant(block) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS)) return;

        if (ConfigOptions.getInstance().advancedTooltips) {
            for (Property<?> property : state.getProperties()) {
                if (property instanceof IntegerProperty ageProp && property.getName().equalsIgnoreCase("age")) {
                    int currentAge = state.getValue(ageProp);
                    int maxAge = ageProp.getPossibleValues().stream().mapToInt(v -> v).max().orElse(0);
                    if (maxAge > 0) {
                        int percent = (int) ((currentAge / (float) maxAge) * 100);
                        info.add("Growth: §f" + percent + "%");
                    }
                    return;
                }
            }
        }
    }

    private int getTotalHeight(Level level, BlockPos pos, Block targetBlock) {
        int total = 1;
        boolean isComplex = isComplexPlant(targetBlock);

        BlockPos scanUp = pos.above();
        while (isSamePlant(level.getBlockState(scanUp), targetBlock, isComplex)) {
            total++;
            scanUp = scanUp.above();
            if (total > 64) break;
        }

        BlockPos scanDown = pos.below();
        while (isSamePlant(level.getBlockState(scanDown), targetBlock, isComplex)) {
            total++;
            scanDown = scanDown.below();
            if (total > 64) break;
        }
        return total;
    }

    private boolean isSamePlant(BlockState currentState, Block target, boolean isComplex) {
        if (target == Blocks.BAMBOO || target == Blocks.BAMBOO_SAPLING) {
            return currentState.is(Blocks.BAMBOO) || currentState.is(Blocks.BAMBOO_SAPLING);
        }
        if (isComplex) {
            if (target == Blocks.KELP || target == Blocks.KELP_PLANT) return currentState.is(Blocks.KELP) || currentState.is(Blocks.KELP_PLANT);
            if (target == Blocks.TWISTING_VINES || target == Blocks.TWISTING_VINES_PLANT) return currentState.is(Blocks.TWISTING_VINES) || currentState.is(Blocks.TWISTING_VINES_PLANT);
            if (target == Blocks.WEEPING_VINES || target == Blocks.WEEPING_VINES_PLANT) return currentState.is(Blocks.WEEPING_VINES) || currentState.is(Blocks.WEEPING_VINES_PLANT);
            if (target == Blocks.CAVE_VINES || target == Blocks.CAVE_VINES_PLANT) return currentState.is(Blocks.CAVE_VINES) || currentState.is(Blocks.CAVE_VINES_PLANT);
        }
        return currentState.is(target);
    }

    private BlockPos findHeadBlock(Level level, BlockPos pos, BlockState state) {
        BlockPos scanPos = pos;
        if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.TWISTING_VINES) || state.is(Blocks.TWISTING_VINES_PLANT)) {
            for (int i = 0; i < 26; i++) {
                if (isSamePlant(level.getBlockState(scanPos.above()), state.getBlock(), true)) scanPos = scanPos.above();
                else break;
            }
        } else if (state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT) || state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)) {
            for (int i = 0; i < 26; i++) {
                if (isSamePlant(level.getBlockState(scanPos.below()), state.getBlock(), true)) scanPos = scanPos.below();
                else break;
            }
        }
        return scanPos;
    }

    private boolean isComplexPlant(Block block) {
        return block == Blocks.KELP || block == Blocks.KELP_PLANT ||
                block == Blocks.TWISTING_VINES || block == Blocks.TWISTING_VINES_PLANT ||
                block == Blocks.WEEPING_VINES || block == Blocks.WEEPING_VINES_PLANT ||
                block == Blocks.CAVE_VINES || block == Blocks.CAVE_VINES_PLANT;
    }
}