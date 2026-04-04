package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class CommandBlockTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // S'applique aux 3 types : Normal, Chain, Repeating
        return state.getBlock() instanceof CommandBlock;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            CompoundTag nbt = accessor.serverData();
            if (nbt == null || !nbt.contains("Command")) return;

            String command = nbt.getString("Command");

            if (command.isEmpty()) {
                info.add("§8(Empty)");
                return;
            }

            // Vérification de la touche CTRL
            if (Screen.hasControlDown()) {
                info.add("§bCommand:");

                // Si la commande est trop longue, on peut la tronquer ou l'afficher en gris
                String displayCommand = command.length() > 50 ? command.substring(0, 47) + "..." : command;
                info.add("§7" + displayCommand);
            } else {
                // Message d'indication simple
                info.add("§8Hold §f[CTRL] §8for details");
            }
        }
    }
}