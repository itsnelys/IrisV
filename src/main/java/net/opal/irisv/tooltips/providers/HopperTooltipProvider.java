package net.opal.irisv.tooltips.providers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HopperTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // Uniquement pour les entonnoirs
        return state.getBlock() instanceof HopperBlock;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        // 1. Gestion de l'état Redstone (Hopper Lock)
        // La propriété ENABLED est fausse quand le hopper est alimenté par redstone
        if (accessor.state().hasProperty(BlockStateProperties.ENABLED)) {
            if (!accessor.state().getValue(BlockStateProperties.ENABLED)) {
                info.add("§cVerrouillé (Redstone)");
            }
        }
    }
}