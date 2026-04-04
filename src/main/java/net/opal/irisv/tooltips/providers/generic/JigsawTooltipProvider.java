package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class JigsawTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.JIGSAW);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            CompoundTag nbt = accessor.serverData();
            if (nbt == null) return;

            if (Screen.hasControlDown()) {
                String name = nbt.getString("name");
                String target = nbt.getString("target");
                String pool = nbt.getString("pool");

                info.add("§bJigsaw Data:");

                // On affiche "empty" si le champ est vide pour rester propre
                info.add("§7Name: §f" + (name.isEmpty() ? "minecraft:empty" : name));
                info.add("§7Target: §f" + (target.isEmpty() ? "minecraft:empty" : target));
                info.add("§7Pool: §8" + (pool.isEmpty() ? "minecraft:empty" : pool));

                if (nbt.contains("joint")) {
                    info.add("§7Joint: §e" + nbt.getString("joint").toUpperCase());
                }
            } else {
                // Uniquement le message d'aide si CTRL n'est pas pressé
                info.add("§8Hold §f[CTRL] §8for details");
            }
        }
    }
}