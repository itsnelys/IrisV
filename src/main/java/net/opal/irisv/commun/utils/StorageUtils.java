package net.opal.irisv.commun.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class StorageUtils {

    /**
     * Détermine la position "Maître".
     * CORRECTION : On se base sur la structure des blocs, pas sur leur contenu.
     */
    public static BlockPos getActualTarget(Level level, BlockPos pos, BlockState state) {
        // 1. Logique Vanilla (Priorité absolue)
        if (state.hasProperty(ChestBlock.TYPE)) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type == ChestType.RIGHT && state.hasProperty(ChestBlock.FACING)) {
                return pos.relative(state.getValue(ChestBlock.FACING).getCounterClockWise());
            }
            if (type == ChestType.LEFT) return pos;
        }

        // 2. Logique Modded / Connectée (Structurelle)
        // Si on est sur un bloc qui peut être double, on définit arbitrairement
        // une règle pour que le Master soit toujours le même (ex: celui avec le X ou Z le plus petit).
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos nPos = pos.relative(dir);
            BlockState nState = level.getBlockState(nPos);

            if (nState.getBlock() == state.getBlock()) {
                // Règle de stabilité : Le bloc avec les coordonnées les plus petites est le Maître
                // Cela permet de trouver le Master même si le coffre est VIDE ou au LANCEMENT de la map.
                if (nPos.getX() < pos.getX() || nPos.getZ() < pos.getZ()) {
                    return nPos;
                }
            }
        }

        return pos;
    }
}