package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.commun.function.FunctionBlockFallbacks;
import net.opal.irisv.network.ClientDataCache; // Import important

import java.util.ArrayList;
import java.util.List;

import static net.opal.irisv.tooltips.TooltipsRequiredTools.getRequiredTools;

public class TooltipData {

    public record BlockInfo(
            String name,
            String modName,
            String modId,
            ItemStack icon,
            List<ItemStack> requiredTools,
            List<String> stateInfo
    ) {}

    /**
     * MAJ : Ajout du paramètre extraInfo (List<String>) pour recevoir les données des providers
     */
    public static BlockInfo collect(Minecraft mc, BlockPos pos, BlockState state, FluidState fluidState, List<String> extraInfo) {
        ResourceLocation id;
        String name;
        ItemStack iconStack;
        String modName;
        String modId;

        // --- GESTION DES FLUIDES ---
        if (!(mc.player.isUnderWater() || mc.player.isInLava()) && !fluidState.isEmpty()) {
            var fluid = fluidState.getType();
            id = BuiltInRegistries.FLUID.getKey(fluid);
            modId = id.getNamespace();
            modName = capitalize(modId);
            name = Component.translatable(fluid.getFluidType().getDescriptionId()).getString();
            var bucketItem = fluid.getBucket();
            iconStack = (bucketItem != null && !bucketItem.equals(Items.AIR)) ? new ItemStack(bucketItem) : new ItemStack(Items.BARRIER);
        }
        // --- GESTION DES BLOCS ---
        else {
            Block block = state.getBlock();
            id = BuiltInRegistries.BLOCK.getKey(block);
            modId = id.getNamespace();
            modName = capitalize(modId);
            iconStack = block.getCloneItemStack(mc.level, pos, state, false, mc.player);

            if (iconStack.isEmpty()) {
                Item fallbackItem = FunctionBlockFallbacks.STRUCTURE_BLOCK_FALLBACKS.get(block);
                iconStack = (fallbackItem != null) ? new ItemStack(fallbackItem) : new ItemStack(Items.BARRIER);
            }
            name = iconStack.getHoverName().getString();
        }

        // On retourne l'info avec la liste extraInfo complétée par l'Overlay
        return new BlockInfo(
                name,
                modName,
                modId,
                iconStack,
                getRequiredTools(state),
                extraInfo
        );
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}