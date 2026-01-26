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

        // 2. Récupération des données réseau
        CompoundTag nbt = accessor.serverData();
        if (nbt == null || !nbt.contains("Items", 9)) return;

        ListTag list = nbt.getList("Items", 10);
        Map<String, ItemStack> combinedItems = new LinkedHashMap<>();

        // 3. Lecture et fusion des items (5 slots max pour un hopper)
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);

            ItemStack.parse(accessor.level().registryAccess(), itemTag).ifPresent(stack -> {
                if (!stack.isEmpty()) {
                    String key = stack.getItem().toString() + stack.getComponents().hashCode();
                    if (combinedItems.containsKey(key)) {
                        combinedItems.get(key).grow(stack.getCount());
                    } else {
                        combinedItems.put(key, stack.copy());
                    }
                }
            });
        }

        // 4. Envoi à la preview d'inventaire
        if (!combinedItems.isEmpty()) {
            accessor.setPreviewItems(new ArrayList<>(combinedItems.values()));
        }
    }
}