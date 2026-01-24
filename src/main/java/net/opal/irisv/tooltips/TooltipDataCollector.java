package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.opal.irisv.commun.function.FunctionBlockFallbacks;

import java.util.ArrayList;
import java.util.List;

public class TooltipDataCollector {

    // AJOUT de stateInfo ici
    // On ajoute modId pour le passer au ModColorManager plus tard
    public record BlockInfo(String name, String modName, String modId, ItemStack icon, List<ItemStack> requiredTools, List<String> stateInfo) {}

    public static BlockInfo collect(Minecraft mc, BlockPos pos, BlockState state, FluidState fluidState) {
        ResourceLocation id;
        String name;
        ItemStack iconStack;
        String modName;
        String modId; // Nouvelle variable

        // --- CAS DES FLUIDES ---
        if (!(mc.player.isUnderWater() || mc.player.isInLava()) && !fluidState.isEmpty()) {
            var fluid = fluidState.getType();
            id = BuiltInRegistries.FLUID.getKey(fluid);
            modId = id.getNamespace(); // Récupère "minecraft", "eden", etc.
            modName = capitalize(modId);

            String baseName = Component.translatable(fluid.getFluidType().getDescriptionId()).getString();
            name = fluidState.isSource() ? baseName + " (Source)" : baseName + " (Flowing)";

            var bucketItem = fluid.getBucket();
            iconStack = (bucketItem != null && !bucketItem.equals(Items.AIR)) ? new ItemStack(bucketItem) : new ItemStack(Items.BARRIER);

            return new BlockInfo(name, modName, modId, iconStack, new ArrayList<>(), new ArrayList<>());
        }

        // --- CAS DES BLOCS ---
        Block block = state.getBlock();
        id = BuiltInRegistries.BLOCK.getKey(block);
        modId = id.getNamespace(); // Récupère le namespace (ex: "twilightforest")
        modName = capitalize(modId);

        iconStack = block.getCloneItemStack(mc.level, pos, state, false, mc.player);

        if (iconStack.isEmpty()) {
            Item fallbackItem = FunctionBlockFallbacks.STRUCTURE_BLOCK_FALLBACKS.get(block);
            iconStack = (fallbackItem != null) ? new ItemStack(fallbackItem) : new ItemStack(Items.BARRIER);
        }

        name = iconStack.getHoverName().getString();

        // APPEL de la classe d'extension (assure-toi que getAllData accepte bien level et pos)
        List<String> states = TooltipDataCollectorExtended.getAllData(mc.level, pos, state);

        // Retourne le record avec le modId inclus
        return new BlockInfo(name, modName, modId, iconStack, getRequiredTools(state), states);
    }

    // Petite méthode utilitaire pour la capitalisation si tu ne l'as pas déjà
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static List<ItemStack> getRequiredTools(BlockState state) {
        List<ItemStack> list = new ArrayList<>();
        boolean req = state.requiresCorrectToolForDrops();

        if (state.getDestroySpeed(null, null) < 0) {
            list.add(new ItemStack(Items.BARRIER));
            return list;
        }

        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) list.add(new ItemStack(Items.DIAMOND_PICKAXE));
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) list.add(new ItemStack(Items.IRON_PICKAXE));
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) list.add(new ItemStack(Items.STONE_PICKAXE));
            else list.add(new ItemStack(Items.WOODEN_PICKAXE));
        }

        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) list.add(new ItemStack(Items.DIAMOND_SHOVEL));
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) list.add(new ItemStack(Items.IRON_SHOVEL));
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) list.add(new ItemStack(Items.STONE_SHOVEL));
            else list.add(new ItemStack(Items.WOODEN_SHOVEL));
        }

        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) list.add(new ItemStack(Items.DIAMOND_AXE));
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) list.add(new ItemStack(Items.IRON_AXE));
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) list.add(new ItemStack(Items.STONE_AXE));
            else list.add(new ItemStack(Items.WOODEN_AXE));
        }

        if (state.is(BlockTags.MINEABLE_WITH_HOE) || state.is(BlockTags.CROPS) ||
                state.is(BlockTags.SMALL_FLOWERS) || state.is(BlockTags.FLOWERS)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) list.add(new ItemStack(Items.DIAMOND_HOE));
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) list.add(new ItemStack(Items.IRON_HOE));
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) list.add(new ItemStack(Items.STONE_HOE));
            else list.add(new ItemStack(Items.WOODEN_HOE));
        }

        if (state.is(Blocks.COBWEB)) {
            list.add(new ItemStack(Items.SHEARS));
            list.add(new ItemStack(Items.WOODEN_SWORD));
        }
        if (state.is(BlockTags.LEAVES)) {
            list.add(new ItemStack(Items.SHEARS));
        }

        if (req && list.isEmpty()) {
            list.add(new ItemStack(Items.MACE));
        }

        return list;
    }
}