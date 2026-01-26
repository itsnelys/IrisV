package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        Block block = state.getBlock();
        // On regroupe tous les blocs qui utilisent une structure "Items" simple
        return block instanceof CrafterBlock ||
                block instanceof DispenserBlock ||
                block instanceof DropperBlock;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        CompoundTag nbt = accessor.serverData();

        // --- LOGIQUE D'INVENTAIRE (Preview) ---
        if (nbt == null || !nbt.contains("Items", 9)) return;

        ListTag list = nbt.getList("Items", 10);
        Map<String, ItemStack> combinedItems = new LinkedHashMap<>();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);

            ItemStack.parse(accessor.level().registryAccess(), itemTag).ifPresent(stack -> {
                if (!stack.isEmpty()) {
                    // Fusion des stacks pour gagner de la place dans le tooltip
                    String key = stack.getItem().toString() + stack.getComponents().hashCode();

                    if (combinedItems.containsKey(key)) {
                        combinedItems.get(key).grow(stack.getCount());
                    } else {
                        combinedItems.put(key, stack.copy());
                    }
                }
            });
        }

        if (!combinedItems.isEmpty()) {
            accessor.setPreviewItems(new ArrayList<>(combinedItems.values()));
        }
    }
}