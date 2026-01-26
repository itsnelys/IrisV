package net.opal.irisv.tooltips.providers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnderChestTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.ENDER_CHEST);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        // 1. On récupère les données envoyées par le ServerDataSender
        CompoundTag data = accessor.serverData();

        // Si pas de données ou pas de liste d'items, on ne fait rien
        if (data == null || !data.contains("Items", 9)) { // 9 = ListTag
            return;
        }

        ListTag list = data.getList("Items", 10); // 10 = CompoundTag (chaque item)
        if (list.isEmpty()) return;

        Map<String, ItemStack> combinedItems = new LinkedHashMap<>();

        // 2. Lecture du NBT reçu du serveur
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);

            // On reconstruit l'item à partir du NBT (Syntaxe 1.21)
            ItemStack.parse(accessor.level().registryAccess(), itemTag).ifPresent(stack -> {
                if (!stack.isEmpty()) {
                    // Clé de fusion (Item + Composants)
                    String key = stack.getItem().toString() + stack.getComponents().hashCode();

                    if (combinedItems.containsKey(key)) {
                        combinedItems.get(key).grow(stack.getCount());
                    } else {
                        combinedItems.put(key, stack.copy());
                    }
                }
            });
        }

        // 3. Envoi à la preview
        if (!combinedItems.isEmpty()) {
            // Optionnel : on ajoute un petit titre dans le tooltip
            accessor.setPreviewItems(new ArrayList<>(combinedItems.values()));
        }
    }
}