package net.opal.irisv.tooltips.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.network.ClientDataCache;

import java.util.List;

public class BeehiveTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return be instanceof BeehiveBlockEntity || state.hasProperty(BlockStateProperties.LEVEL_HONEY);
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        // 1. Récupération des données synchronisées
        CompoundTag data = ClientDataCache.get(pos);

        // Fallback pour le mode solo
        if (data.isEmpty() && be != null) {
            data = be.saveWithFullMetadata(level.registryAccess());
        }

        // 2. Gestion des Abeilles (via le NBT "Bees")
        // Dans une ruche, les abeilles sont stockées dans une liste NBT nommée "Bees"
        if (data.contains("Bees")) {
            int bees = data.getList("Bees", 10).size(); // 10 = Type CompoundTag
            info.add("Bees: §e" + bees);
        } else if (be instanceof BeehiveBlockEntity hive) {
            // Sécurité si le paquet n'est pas encore arrivé
            info.add("Bees: §e" + hive.getOccupantCount());
        }

        // 3. Gestion du Miel (via BlockState - pas besoin de cache car le State est synchro par Minecraft)
        if (state.hasProperty(BlockStateProperties.LEVEL_HONEY)) {
            int honeyLevel = state.getValue(BlockStateProperties.LEVEL_HONEY);
            String color = (honeyLevel >= 5) ? "§6" : "§f";
            info.add("Honey: " + color + honeyLevel + "/5");

            if (honeyLevel >= 5) {
                info.add("§aReady to harvest!");
            }
        }
    }
}