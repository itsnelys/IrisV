package net.opal.irisv.tooltips;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.opal.irisv.tooltips.helpers.TooltipCopperHelper;
import net.opal.irisv.tooltips.modded.TooltipDataCollectorExtendedModded;

import java.util.ArrayList;
import java.util.List;

public class TooltipDataCollectorExtended {

    public static List<String> getAllData(Level level, BlockPos pos, BlockState state) {
        List<String> info = new ArrayList<>();
        BlockEntity be = level.getBlockEntity(pos);

        addStateData(level, pos, state, info);
        TooltipDataCollectorExtendedModded.addModdedData(info, state, be);

        // 1. REALISTIC COPPER LOGIC
        var copper = TooltipCopperHelper.getInfo(level, state, pos);
        if (copper != null) {
            info.add("Oxidation: " + copper.color() + copper.percent() + "% ");
            if (copper.isWaxed()) {
                info.add("§6STATUS: §eWAXED §7(Protected)");
            } else if (copper.percent() < 95) {
                info.add("§8Status: §7Oxidizing...");
            }
        }

        // 2. BLOCK ENTITIES (Advanced Data)
        if (be != null) {
            if (be instanceof JukeboxBlockEntity jukebox) {
                ItemStack recordStack = jukebox.getItem(0);
                if (!recordStack.isEmpty()) {
                    info.add("Disc: §e" + recordStack.getHoverName().getString());
                    if (jukebox.getSongPlayer() != null && jukebox.getSongPlayer().isPlaying()) {
                        info.add("Status: §aNow Playing...");
                        long seconds = jukebox.getSongPlayer().getTicksSinceSongStarted() / 20;
                        info.add("Time: §7" + (seconds / 60) + "m " + (seconds % 60) + "s");
                    } else {
                        info.add("Status: §7Stopped");
                    }
                } else if (state.hasProperty(BlockStateProperties.HAS_RECORD) && state.getValue(BlockStateProperties.HAS_RECORD)) {
                    info.add("Status: §aNow Playing...");
                }
            }
            else if (be instanceof SignBlockEntity sign) {
                String text = sign.getFrontText().getMessage(0, false).getString();
                if (!text.isEmpty()) info.add("Text: §f\"" + text + "...\"");
            }
            else if (be instanceof BeehiveBlockEntity hive) {
                int bees = hive.getOccupantCount();
                info.add("Bees: §e" + bees);
            }
        }
        return info;
    }

    private static void addStateData(Level level, BlockPos pos, BlockState state, List<String> info) {
        // --- Redstone & Power ---
        if (state.hasProperty(BlockStateProperties.POWER)) info.add("Power: §f" + state.getValue(BlockStateProperties.POWER));
        if (state.hasProperty(BlockStateProperties.DELAY)) info.add("Delay: §f" + state.getValue(BlockStateProperties.DELAY));
        if (state.hasProperty(BlockStateProperties.POWERED)) {
            info.add("Status: " + (state.getValue(BlockStateProperties.POWERED) ? "§aON" : "§cOFF"));
        }
        if (state.hasProperty(BlockStateProperties.MODE_COMPARATOR)) {
            info.add("Mode: §f" + (state.getValue(BlockStateProperties.MODE_COMPARATOR) == ComparatorMode.SUBTRACT ? "Subtraction" : "Comparison"));
        }

        // --- End Portal ---
        if (state.hasProperty(BlockStateProperties.EYE)) {
            info.add("End Eye: " + (state.getValue(BlockStateProperties.EYE) ? "§aInserted" : "§7Empty"));
        }

// --- Agriculture & Growth ---
        Block block = state.getBlock();

// A. PLANTE VERTICALES (Bambou, Kelp, Lianes, Canne à sucre, Cactus)
        boolean isVertical = state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING) ||
                state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) ||
                state.is(Blocks.TWISTING_VINES) || state.is(Blocks.TWISTING_VINES_PLANT) ||
                state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT) ||
                state.is(Blocks.VINE) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS);

        if (isVertical) {
            int totalHeight = getTotalHeight(level, pos, block);
            info.add("Total Height: §e" + totalHeight + " blocks");

            // Cas spécifique : Bambou
            if (state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)) {
                if (state.hasProperty(BlockStateProperties.STAGE)) {
                    String status = state.getValue(BlockStateProperties.STAGE) == 1 ? "§cMax Height (Mature)" : "§bGrowing...";
                    info.add("Status: " + status);
                }
            }
            // Cas spécifique : Kelp & Lianes du Nether (Potentiel basé sur l'AGE 25)
            else if (isComplexPlant(block)) {
                BlockPos headPos = findHeadBlock(level, pos, state);
                BlockState headState = level.getBlockState(headPos);
                if (headState.hasProperty(BlockStateProperties.AGE_25)) {
                    int age = headState.getValue(BlockStateProperties.AGE_25);
                    info.add(age >= 25 ? "Growth: §cMax" : "Potential: §b+" + (25 - age) + " blocks");
                }
            }
            // Cas spécifique : Canne à sucre & Cactus (Max 3)
            else if (state.hasProperty(BlockStateProperties.AGE_15)) {
                if (totalHeight >= 3) info.add("Growth: §cMax Height");
            }
        }

// B. CULTURES AU SOL (Blé, Carottes, etc.)
        else if (state.hasProperty(BlockStateProperties.AGE_7)) {
            info.add("Growth: §f" + (int)(state.getValue(BlockStateProperties.AGE_7) / 7f * 100) + "%");
        }
        else if (state.hasProperty(BlockStateProperties.AGE_5)) {
            info.add("Growth: §f" + (int)(state.getValue(BlockStateProperties.AGE_5) / 5f * 100) + "%");
        }
        else if (state.hasProperty(BlockStateProperties.AGE_3)) {
            info.add("Growth: §f" + (int)(state.getValue(BlockStateProperties.AGE_3) / 3f * 100) + "%");
        }

        if (state.hasProperty(BlockStateProperties.MOISTURE)) {
            int m = state.getValue(BlockStateProperties.MOISTURE);
            info.add("Moisture: §b" + (int)((m / 7.0f) * 100) + "%");
        }

        // --- Levels & Charges ---
        if (state.hasProperty(BlockStateProperties.LEVEL_COMPOSTER)) info.add("Composter: §2" + state.getValue(BlockStateProperties.LEVEL_COMPOSTER) + "/8");
        if (state.hasProperty(BlockStateProperties.LEVEL_HONEY)) info.add("Honey: §6" + state.getValue(BlockStateProperties.LEVEL_HONEY) + "/5");
        if (state.hasProperty(BlockStateProperties.LEVEL_CAULDRON)) info.add("Water Level: §b" + state.getValue(BlockStateProperties.LEVEL_CAULDRON) + "/3");
        if (state.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)) info.add("Charges: §d" + state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES) + "/4");

        // --- Interaction ---
        if (state.hasProperty(BlockStateProperties.BITES)) info.add("Bites: §f" + state.getValue(BlockStateProperties.BITES) + "/6");
        if (state.hasProperty(BlockStateProperties.CANDLES)) info.add("Candles: §f" + state.getValue(BlockStateProperties.CANDLES));
        if (state.hasProperty(BlockStateProperties.PICKLES)) info.add("Pickles: §f" + state.getValue(BlockStateProperties.PICKLES));

        // --- Chiseled Bookshelf ---
        if (state.hasProperty(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_0_OCCUPIED)) {
            int count = 0;
            for (int i = 0; i < 6; i++) {
                // Utilisation simplifiée si possible ou garder tes IFs pour la compatibilité
                if (isSlotOccupied(state, i)) count++;
            }
            info.add("Books: §f" + count + "/6");
        }

        // --- Note Block ---
        if (state.hasProperty(BlockStateProperties.NOTE)) {
            info.add("Note: §e" + state.getValue(BlockStateProperties.NOTE));
            if (state.hasProperty(BlockStateProperties.NOTEBLOCK_INSTRUMENT)) {
                info.add("Instrument: §7" + state.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT).getSerializedName());
            }
        }
        // --- Enchanting & Bookshelves ---
        if (state.is(Blocks.ENCHANTING_TABLE)) {
            // Affiche la puissance totale disponible pour la table
            float power = getEnchantingPower(level, pos);
            info.add("Ench Power: §e" + (int)power);
        }
        else if (state.is(Blocks.BOOKSHELF)) {
            info.add("Ench Power: §f1");
        }

        // --- Calibrated Sculk Sensor (Input Signal) ---
        if (state.is(Blocks.CALIBRATED_SCULK_SENSOR)) {
            // Récupère le signal redstone entrant (Input)
            int signal = level.getBestNeighborSignal(pos);
            info.add("Input: §d" + signal);
        }
    }
    /**
     * Calcule la puissance d'enchantement totale autour d'une position (Table d'enchantement).
     */
    private static float getEnchantingPower(Level level, BlockPos pos) {
        float power = 0;
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                if ((dz != 0 || dx != 0) && level.isEmptyBlock(pos.offset(dx, 0, dz)) && level.isEmptyBlock(pos.offset(dx, 1, dz))) {
                    power += getEnchantBlockPower(level, pos.offset(dx * 2, 0, dz * 2));
                    power += getEnchantBlockPower(level, pos.offset(dx * 2, 1, dz * 2));
                    if (dx != 0 && dz != 0) {
                        power += getEnchantBlockPower(level, pos.offset(dx * 2, 0, dz));
                        power += getEnchantBlockPower(level, pos.offset(dx * 2, 1, dz));
                        power += getEnchantBlockPower(level, pos.offset(dx, 0, dz * 2));
                        power += getEnchantBlockPower(level, pos.offset(dx, 1, dz * 2));
                    }
                }
            }
        }
        return power;
    }

    private static float getEnchantBlockPower(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.BOOKSHELF)) return 1.0f;
        // Ajoute ici la compatibilité avec d'autres blocs si nécessaire
        return 0;
    }
    // --- HELPER METHODS ---
    private static int countHeight(Level level, BlockPos pos, BlockState state) {
        int height = 1;
        BlockPos down = pos.below();
        while (level.getBlockState(down).is(state.getBlock())) {
            height++;
            down = down.below();
        }
        return height;
    }

    private static boolean isTopBlock(Level level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    private static boolean isSlotOccupied(BlockState state, int slot) {
        return switch (slot) {
            case 0 -> state.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_0_OCCUPIED);
            case 1 -> state.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_1_OCCUPIED);
            case 2 -> state.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_2_OCCUPIED);
            case 3 -> state.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_3_OCCUPIED);
            case 4 -> state.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_4_OCCUPIED);
            case 5 -> state.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_5_OCCUPIED);
            default -> false;
        };
    }

    /**
     * Calcule la hauteur totale d'une plante en scannant vers le haut et vers le bas.
     */
    private static int getTotalHeight(Level level, BlockPos pos, Block targetBlock) {
        int total = 1;
        boolean isComplex = isComplexPlant(targetBlock);

        // Scan vers le haut
        BlockPos scanUp = pos.above();
        while (isSamePlant(level.getBlockState(scanUp), targetBlock, isComplex)) {
            total++;
            scanUp = scanUp.above();
            if (total > 64) break; // Sécurité anti-boucle
        }

        // Scan vers le bas
        BlockPos scanDown = pos.below();
        while (isSamePlant(level.getBlockState(scanDown), targetBlock, isComplex)) {
            total++;
            scanDown = scanDown.below();
            if (total > 64) break; // Sécurité anti-boucle
        }

        return total;
    }

    /**
     * Vérifie si le bloc scanné appartient à la même famille de plante.
     */
    private static boolean isSamePlant(BlockState currentState, Block target, boolean isComplex) {
        if (target == Blocks.BAMBOO || target == Blocks.BAMBOO_SAPLING) {
            return currentState.is(Blocks.BAMBOO) || currentState.is(Blocks.BAMBOO_SAPLING);
        }
        if (isComplex) {
            if (target == Blocks.KELP || target == Blocks.KELP_PLANT)
                return currentState.is(Blocks.KELP) || currentState.is(Blocks.KELP_PLANT);
            if (target == Blocks.TWISTING_VINES || target == Blocks.TWISTING_VINES_PLANT)
                return currentState.is(Blocks.TWISTING_VINES) || currentState.is(Blocks.TWISTING_VINES_PLANT);
            if (target == Blocks.WEEPING_VINES || target == Blocks.WEEPING_VINES_PLANT)
                return currentState.is(Blocks.WEEPING_VINES) || currentState.is(Blocks.WEEPING_VINES_PLANT);
        }
        return currentState.is(target);
    }

    /**
     * Trouve le bloc de "tête" qui contient la propriété d'âge.
     */
    private static BlockPos findHeadBlock(Level level, BlockPos pos, BlockState state) {
        BlockPos scanPos = pos;
        // Kelp et Twisting poussent vers le HAUT
        if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.TWISTING_VINES) || state.is(Blocks.TWISTING_VINES_PLANT)) {
            for (int i = 0; i < 26; i++) {
                BlockState above = level.getBlockState(scanPos.above());
                if (isSamePlant(above, state.getBlock(), true)) scanPos = scanPos.above();
                else break;
            }
        }
        // Weeping poussent vers le BAS
        else if (state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT)) {
            for (int i = 0; i < 26; i++) {
                BlockState below = level.getBlockState(scanPos.below());
                if (isSamePlant(below, state.getBlock(), true)) scanPos = scanPos.below();
                else break;
            }
        }
        return scanPos;
    }

    private static boolean isComplexPlant(Block block) {
        return block == Blocks.KELP || block == Blocks.KELP_PLANT ||
                block == Blocks.TWISTING_VINES || block == Blocks.TWISTING_VINES_PLANT ||
                block == Blocks.WEEPING_VINES || block == Blocks.WEEPING_VINES_PLANT;
    }

}