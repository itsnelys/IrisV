package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChestTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL)
                || state.getBlock() instanceof ShulkerBoxBlock;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        BlockState state = accessor.state();
        BlockPos pos = accessor.pos();
        Map<String, ItemStack> combinedItems = new LinkedHashMap<>();

        BlockPos leftPos = pos;
        BlockPos rightPos = null;

        // Détermination stricte des deux moitiés
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction facing = state.getValue(ChestBlock.FACING);
            if (state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) {
                leftPos = pos.relative(facing.getCounterClockWise());
                rightPos = pos;
            } else {
                leftPos = pos;
                rightPos = pos.relative(facing.getClockWise());
            }
        }

        // --- RÉCUPÉRATION FLUIDE ---
        // On prend la gauche (Slots 1-27)
        CompoundTag leftData = (leftPos.equals(pos)) ? accessor.serverData() : net.opal.irisv.network.ClientDataCache.get(leftPos);
        if (leftData != null) processItems(leftData.getList("Items", 10), combinedItems, accessor);

        // On prend la droite (Slots 28-54)
        if (rightPos != null) {
            CompoundTag rightData = (rightPos.equals(pos)) ? accessor.serverData() : net.opal.irisv.network.ClientDataCache.get(rightPos);
            if (rightData != null) processItems(rightData.getList("Items", 10), combinedItems, accessor);
        }

        if (!combinedItems.isEmpty()) {
            accessor.setPreviewItems(new ArrayList<>(combinedItems.values()));
        }
    }

    private void processItems(ListTag tagList, Map<String, ItemStack> combinedItems, IBlockAccessor accessor) {
        for (int i = 0; i < tagList.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(accessor.level().registryAccess(), tagList.getCompound(i));
            if (!stack.isEmpty()) {
                // Fusion par clé unique (Item + NBT/Components)
                String key = stack.getItem().toString() + stack.getComponents().hashCode();
                if (combinedItems.containsKey(key)) {
                    combinedItems.get(key).grow(stack.getCount());
                } else {
                    combinedItems.put(key, stack.copy());
                }
            }
        }
    }
}