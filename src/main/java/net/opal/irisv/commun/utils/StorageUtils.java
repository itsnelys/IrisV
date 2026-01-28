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
        // 1. SOPHISTICATED STORAGE (Priorité)
        for (net.minecraft.world.level.block.state.properties.Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("is_left")) {
                boolean isLeft = (Boolean) state.getValue(prop);
                if (isLeft) return pos; // C'est le Master

                // Si c'est la partie droite, le Master est à "gauche" selon le Facing
                if (state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
                    Direction facing = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
                    return pos.relative(facing.getCounterClockWise());
                }
            }
        }

        // 2. VANILLA
        if (state.hasProperty(ChestBlock.TYPE)) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type == ChestType.RIGHT && state.hasProperty(ChestBlock.FACING)) {
                return pos.relative(state.getValue(ChestBlock.FACING).getCounterClockWise());
            }
        }

        // 3. FALLBACK GÉNÉRIQUE (Règle du X/Z min)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos nPos = pos.relative(dir);
            if (level.getBlockState(nPos).getBlock() == state.getBlock()) {
                if (nPos.getX() < pos.getX() || nPos.getZ() < pos.getZ()) return nPos;
            }
        }
        return pos;
    }
}