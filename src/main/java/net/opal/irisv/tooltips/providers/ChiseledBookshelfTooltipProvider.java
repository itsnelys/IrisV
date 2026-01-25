package net.opal.irisv.tooltips.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.network.ClientDataCache;

import java.util.List;

public class ChiseledBookshelfTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.CHISELED_BOOKSHELF);
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        // 1. On récupère les données du cache (utile si on veut lire les items précis plus tard)
        CompoundTag data = ClientDataCache.get(pos);

        // Fallback Solo
        if (data.isEmpty() && be != null) {
            data = be.saveWithFullMetadata(level.registryAccess());
        }

        int count = 0;
        // 2. On utilise toujours le BlockState pour le compte rapide (car c'est synchro nativement)
        for (int i = 0; i < 6; i++) {
            if (isSlotOccupied(state, i)) {
                count++;
            }
        }

        // 3. Affichage
        String color = (count == 6) ? "§a" : (count == 0 ? "§7" : "§f");
        info.add("Books: " + color + count + "/6");

        if (count == 6) {
            info.add("§8(Full)");
        }

        // Note : Si tu voulais afficher le nom des livres enchantés,
        // tu devrais fouiller dans 'data.getList("Items", 10)'
    }

    private boolean isSlotOccupied(BlockState state, int slot) {
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
}