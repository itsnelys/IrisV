package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.network.chat.Component;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.commun.function.FunctionBlockFallbacks;

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

    public static BlockInfo collect(Minecraft mc, BlockPos pos, BlockState state, FluidState fluid, List<String> extraInfo, IBlockAccessor accessor) {
        ResourceLocation id;
        String name;
        ItemStack iconStack;
        String modName;
        String modId;

        // --- GESTION DES FLUIDES ---
        if (!(mc.player.isUnderWater() || mc.player.isInLava()) && !fluid.isEmpty()) {
            var f = fluid.getType();
            id = BuiltInRegistries.FLUID.getKey(f);
            modId = id.getNamespace();
            modName = capitalize(modId);
            name = Component.translatable(f.getFluidType().getDescriptionId()).getString();
            var bucketItem = f.getBucket();

            // Filtre : Si pas de bucket, on met AIR au lieu de BARRIER
            iconStack = (bucketItem != null && !bucketItem.equals(Items.AIR)) ? new ItemStack(bucketItem) : ItemStack.EMPTY;
        }
        // --- GESTION DES BLOCS ---
        else {
            Block block = state.getBlock();
            id = BuiltInRegistries.BLOCK.getKey(block);
            modId = id.getNamespace();
            modName = capitalize(modId);

            iconStack = accessor.getIcon();

            if (iconStack.isEmpty()) {
                iconStack = block.getCloneItemStack(mc.level, pos, state);
            }

            // Fallback ultime
            if (iconStack.isEmpty()) {
                Item fallbackItem = FunctionBlockFallbacks.STRUCTURE_BLOCK_FALLBACKS.get(block);
                iconStack = (fallbackItem != null) ? new ItemStack(fallbackItem) : ItemStack.EMPTY;
            }

            // --- FILTRE ANTI-BARRIÈRE / VOID ---
            // Si l'icône finale est un item "interdit", on vide l'icône pour ne rien afficher
            if (isForbidden(iconStack)) {
                iconStack = ItemStack.EMPTY;
            }

            String titleOverride = accessor.getTitleOverride();
            name = (titleOverride != null) ? titleOverride : iconStack.getHoverName().getString();
        }

        return new BlockInfo(
                name,
                modName,
                modId,
                iconStack,
                getRequiredTools(state),
                extraInfo
        );
    }

    /**
     * Teste si un item est interdit (Barrière ou Structure Void)
     */
    public static boolean isForbidden(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        Item item = stack.getItem();
        return item == Items.BARRIER || item == Items.STRUCTURE_VOID || item == Items.AIR;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
