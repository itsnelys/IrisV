package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.commun.utils.StorageUtils;
import net.opal.irisv.network.ClientDataCache;
import net.opal.irisv.option.ConfigOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryTooltipProvider implements IBlockTooltipProvider {

    public boolean isApplicable(BlockState state, BlockEntity be) {


        // Logique standard pour le reste des inventaires
        return state != null && (state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof CrafterBlock) ||
                be instanceof Container ||
                (be != null && be.getLevel() != null &&
                        be.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), state, be, null) != null);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            Map<String, ItemStack> combinedItems = new LinkedHashMap<>();
            BlockPos masterPos = StorageUtils.getActualTarget(accessor.level(), accessor.pos(), accessor.state());
            CompoundTag data = accessor.serverData();

            if ((data == null || data.isEmpty()) && !masterPos.equals(accessor.pos())) {
                data = ClientDataCache.get(masterPos);
            }

            if (data != null && !data.isEmpty()) {
                if (data.contains("Items", Tag.TAG_LIST)) {
                    if (usesExactSlotPreview(accessor.state())) {
                        accessor.setPreviewItems(readSlots(data.getList("Items", Tag.TAG_COMPOUND), accessor, expectedSlotCount(accessor.state())));
                        return;
                    }
                    mergeList(data.getList("Items", Tag.TAG_COMPOUND), combinedItems, accessor);
                } else {
                    findAndMerge(data, combinedItems, accessor);
                }
            }

            if (combinedItems.isEmpty()) {
                BlockEntity targetBE = accessor.level().getBlockEntity(masterPos);
                if (targetBE != null) {
                    readDirectlyFromBE(accessor, masterPos, targetBE, combinedItems);
                }
            }

            if (!combinedItems.isEmpty()) {
                accessor.setPreviewItems(new ArrayList<>(combinedItems.values()));
            }
        }
    }

    private void findAndMerge(CompoundTag tag, Map<String, ItemStack> combinedItems, IBlockAccessor accessor) {
        if (tag == null) return;
        String[] commonKeys = {"Items", "inventory", "Inventory", "storageContents"};
        for (String key : commonKeys) {
            if (tag.contains(key, Tag.TAG_LIST)) {
                mergeList(tag.getList(key, Tag.TAG_COMPOUND), combinedItems, accessor);
                return;
            }
        }

        for (String key : tag.getAllKeys()) {
            if (tag.contains(key, Tag.TAG_COMPOUND)) {
                CompoundTag subTag = tag.getCompound(key);
                for (String subKey : commonKeys) {
                    if (subTag.contains(subKey, Tag.TAG_LIST)) {
                        mergeList(subTag.getList(subKey, Tag.TAG_COMPOUND), combinedItems, accessor);
                        return;
                    }
                }
            }
        }
    }

    private void readDirectlyFromBE(IBlockAccessor accessor, BlockPos pos, BlockEntity be, Map<String, ItemStack> combinedItems) {
        var handler = accessor.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, accessor.level().getBlockState(pos), be, null);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    mergeStack(combinedItems, stack);
                }
            }
        }
    }

    private void mergeList(ListTag tagList, Map<String, ItemStack> combinedItems, IBlockAccessor accessor) {
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            if (itemTag.contains("id") || itemTag.contains("item")) {
                // parseOptional va recréer l'item mais limiter le count à 99
                ItemStack stack = ItemStack.parseOptional(accessor.level().registryAccess(), itemTag);

                if (!stack.isEmpty()) {
                    // TRUC : On écrase le count bridé par notre valeur "count" stockée en NBT
                    if (itemTag.contains("count")) {
                        stack.setCount(itemTag.getInt("count"));
                    }
                    mergeStack(combinedItems, stack);
                }
            }
        }
    }

    private List<ItemStack> readSlots(ListTag tagList, IBlockAccessor accessor, int slotCount) {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < slotCount; i++) slots.add(ItemStack.EMPTY);
        boolean hasItem = false;

        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot < 0 || slot >= slotCount || !(itemTag.contains("id") || itemTag.contains("item"))) continue;

            ItemStack stack = ItemStack.parseOptional(accessor.level().registryAccess(), itemTag);
            if (!stack.isEmpty()) {
                if (itemTag.contains("count")) stack.setCount(itemTag.getInt("count"));
                slots.set(slot, stack);
                hasItem = true;
            }
        }

        return hasItem ? slots : List.of();
    }

    private boolean usesExactSlotPreview(BlockState state) {
        return state != null && (state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof CrafterBlock);
    }

    private int expectedSlotCount(BlockState state) {
        if (state != null && state.getBlock() instanceof CampfireBlock) return 4;
        if (state != null && state.getBlock() instanceof CrafterBlock) return 9;
        return 0;
    }

    private void mergeStack(Map<String, ItemStack> combinedItems, ItemStack stack) {
        // On utilise l'ID de l'item + ses composants pour le regroupement
        String key = stack.getItem().toString() + stack.getComponents().hashCode();
        if (combinedItems.containsKey(key)) {
            // .grow() fonctionne avec des int, donc il acceptera les gros chiffres
            combinedItems.get(key).grow(stack.getCount());
        } else {
            combinedItems.put(key, stack.copy());
        }
    }
}
