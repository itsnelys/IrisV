package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.opal.irisv.commun.function.FunctionBlockFallbacks;

import java.util.List;

import static net.opal.irisv.tooltips.TooltipsRequiredTools.getRequiredTools;

public class TooltipData {

    public record BlockInfo(String name, String modName, String modId, ItemStack icon, List<ItemStack> requiredTools, List<String> stateInfo) {}

    public static BlockInfo collect(Minecraft mc, BlockPos pos, BlockState state, FluidState fluidState) {
        ResourceLocation id;
        String name;
        ItemStack iconStack;
        String modName;
        String modId;

        // --- CAS DES FLUIDES ---
        // On vérifie si on regarde un fluide (et qu'on n'est pas déjà dedans pour éviter le spam visuel)
        if (!(mc.player.isUnderWater() || mc.player.isInLava()) && !fluidState.isEmpty()) {
            var fluid = fluidState.getType();
            id = BuiltInRegistries.FLUID.getKey(fluid);
            modId = id.getNamespace();
            modName = capitalize(modId);

            // Nom simple (ex: "Water"). Le FluidTooltipProvider ajoutera "Source" ou "Level" en dessous.
            name = Component.translatable(fluid.getFluidType().getDescriptionId()).getString();

            // Icône : On essaie de récupérer le seau correspondant, sinon une barrière
            var bucketItem = fluid.getBucket();
            iconStack = (bucketItem != null && !bucketItem.equals(Items.AIR)) ? new ItemStack(bucketItem) : new ItemStack(Items.BARRIER);
        }
        // --- CAS DES BLOCS ---
        else {
            Block block = state.getBlock();
            id = BuiltInRegistries.BLOCK.getKey(block);
            modId = id.getNamespace();
            modName = capitalize(modId);

            // Récupération de l'icône (Pick Block)
            iconStack = block.getCloneItemStack(mc.level, pos, state, false, mc.player);

            // Fallback si le bloc n'a pas d'item (ex: portails, blocs techniques)
            if (iconStack.isEmpty()) {
                Item fallbackItem = FunctionBlockFallbacks.STRUCTURE_BLOCK_FALLBACKS.get(block);
                iconStack = (fallbackItem != null) ? new ItemStack(fallbackItem) : new ItemStack(Items.BARRIER);
            }

            name = iconStack.getHoverName().getString();
        }

        // --- RÉCUPÉRATION DES DONNÉES DÉTAILLÉES (via les Providers) ---
        // Cette ligne appelle ton nouveau système modulaire (Agriculture, Redstone, Fluides, etc.)
        List<String> states = TooltipProvider.getAllData(mc.level, pos, state);

        return new BlockInfo(
                name,
                modName,
                modId,
                iconStack,
                getRequiredTools(state),
                states
        );
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}