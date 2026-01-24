package net.opal.irisv.commun.function;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

public class FunctionBlockFallbacks {
    public static final Map<Block, Item> STRUCTURE_BLOCK_FALLBACKS = new HashMap<>();

    static {
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.TALL_SEAGRASS, Items.SEAGRASS);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.KELP_PLANT, Items.KELP);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.PISTON_HEAD, Items.PISTON);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.MOVING_PISTON, Items.PISTON);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.BUBBLE_COLUMN, Items.WATER_BUCKET);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.FIRE, Items.FLINT_AND_STEEL);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.SOUL_FIRE, Items.FLINT_AND_STEEL);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.STRUCTURE_VOID, Items.BARRIER);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.STRUCTURE_BLOCK, Items.STRUCTURE_BLOCK);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.JIGSAW, Items.JIGSAW);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.BARRIER, Items.BARRIER);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.LIGHT, Items.LIGHT);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.COMMAND_BLOCK, Items.COMMAND_BLOCK);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.REPEATING_COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.CHAIN_COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.NETHER_PORTAL, Items.OBSIDIAN);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.END_PORTAL, Items.END_PORTAL_FRAME);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.END_GATEWAY, Items.END_CRYSTAL);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.REDSTONE_WIRE, Items.REDSTONE);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.TRIPWIRE, Items.STRING);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.POWDER_SNOW, Items.POWDER_SNOW_BUCKET);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.CAVE_AIR, Items.BARRIER);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.VOID_AIR, Items.BARRIER);
        STRUCTURE_BLOCK_FALLBACKS.put(Blocks.AIR, Items.BARRIER);
    }
}