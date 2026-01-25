package net.opal.irisv.tooltips;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.tooltips.modded.TooltipsRequiredToolsModded;

import java.util.ArrayList;
import java.util.List;

public class TooltipsRequiredTools {
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

        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
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

        // --- APPEL DE LA CLASSE MODDÉE ---
        TooltipsRequiredToolsModded.getRequiredToolsModded(state, list);

        if (req && list.isEmpty()) {
            list.add(new ItemStack(Items.MACE));
        }

        return list;
    }
}