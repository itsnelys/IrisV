package net.opal.irisv.tooltips.providers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor;

import java.util.List;

public class BeehiveTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // S'active si c'est une ruche ou si le bloc a une propriété de miel (ruches moddées)
        return be instanceof BeehiveBlockEntity || state.hasProperty(BlockStateProperties.LEVEL_HONEY);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        // 1. Récupération des données depuis l'accessor
        BlockState state = accessor.state();
        CompoundTag data = accessor.serverData();

        // 2. Gestion des Abeilles (via le NBT "Bees")
        // En Multi, serverData contient les données synchronisées du serveur (UpdateTag)
        if (data != null && data.contains("Bees", 9)) { // 9 = TAG_List
            int bees = data.getList("Bees", 10).size(); // 10 = TAG_Compound
            info.add("Bees: §e" + bees);
        }
        // Fallback si on est en solo ou si la BlockEntity est accessible directement
        else if (accessor.blockEntity() instanceof BeehiveBlockEntity hive) {
            info.add("Bees: §e" + hive.getOccupantCount());
        }

        // 3. Gestion du Miel (via BlockState)
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