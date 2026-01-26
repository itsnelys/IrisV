package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.commun.utils.StorageUtils;
import net.opal.irisv.network.ClientDataCache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return be instanceof Container ||
                (be != null && be.getLevel() != null &&
                        be.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), state, be, null) != null);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        Map<String, ItemStack> combinedItems = new LinkedHashMap<>();

        // 1. Détermination du MASTER
        BlockPos masterPos = StorageUtils.getActualTarget(accessor.level(), accessor.pos(), accessor.state());

        // 2. Récupération des données serveur
        CompoundTag data = accessor.serverData();

        // Si l'accessor est vide (cas de la partie 2 moddée), on force la récupération du cache au MasterPos
        if ((data == null || data.isEmpty()) && !masterPos.equals(accessor.pos())) {
            data = ClientDataCache.get(masterPos);
        }

        // 3. Traitement des données (Recursion profonde pour les mods)
        if (data != null && !data.isEmpty()) {
            findAndMerge(data, combinedItems, accessor);
        }

        // 4. FALLBACK : Lecture directe si le serveur n'a rien envoyé (Ex: Solo ou Moddé non sync)
        if (combinedItems.isEmpty()) {
            // On tente de lire le Master déterminé par StorageUtils
            BlockEntity targetBE = accessor.level().getBlockEntity(masterPos);
            if (targetBE != null) {
                readDirectlyFromBE(accessor, masterPos, targetBE, combinedItems);
            }
        }

        if (!combinedItems.isEmpty()) {
            accessor.setPreviewItems(new ArrayList<>(combinedItems.values()));
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

    private void findAndMerge(CompoundTag tag, Map<String, ItemStack> combinedItems, IBlockAccessor accessor) {
        if (tag == null) return;

        // "Items" est le tag standard
        if (tag.contains("Items", Tag.TAG_LIST)) {
            mergeList(tag.getList("Items", Tag.TAG_COMPOUND), combinedItems, accessor);
        }

        // Récursion profonde pour les noms de tags spécifiques aux mods (storageContents, inventory, etc.)
        for (String key : tag.getAllKeys()) {
            if (key.equals("Items")) continue;
            if (tag.contains(key, Tag.TAG_COMPOUND)) {
                findAndMerge(tag.getCompound(key), combinedItems, accessor);
            } else if (tag.contains(key, Tag.TAG_LIST)) {
                mergeList(tag.getList(key, Tag.TAG_COMPOUND), combinedItems, accessor);
            }
        }
    }

    private void mergeList(ListTag tagList, Map<String, ItemStack> combinedItems, IBlockAccessor accessor) {
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            if (itemTag.contains("id") || itemTag.contains("item")) {
                ItemStack stack = ItemStack.parseOptional(accessor.level().registryAccess(), itemTag);
                if (!stack.isEmpty()) {
                    mergeStack(combinedItems, stack);
                }
            }
        }
    }

    private void mergeStack(Map<String, ItemStack> combinedItems, ItemStack stack) {
        String key = stack.getItem().toString() + stack.getComponents().hashCode();
        if (combinedItems.containsKey(key)) {
            combinedItems.get(key).grow(stack.getCount());
        } else {
            combinedItems.put(key, stack.copy());
        }
    }
}