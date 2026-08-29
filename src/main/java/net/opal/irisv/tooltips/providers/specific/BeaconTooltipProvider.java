package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import javax.annotation.Nullable;
import java.util.List;

public class BeaconTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, @Nullable BlockEntity be) {
        return state != null && state.getBlock() instanceof BeaconBlock;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (!ConfigOptions.getInstance().advancedTooltips) return;

        CompoundTag data = accessor.serverData();
        if (data == null || data.isEmpty()) return;

        int levels = data.getInt("Levels");
        info.add("§bPyramide: §f" + levels + "§7/4");
        info.add("§dPrimaire: §f" + effectName(data.getString("primary_effect")));
        info.add("§dSecondaire: §f" + effectName(data.getString("secondary_effect")));
    }

    private static String effectName(String id) {
        if (id == null || id.isBlank()) return "Aucun";

        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return id;

        var effect = BuiltInRegistries.MOB_EFFECT.get(location);
        if (effect != null) {
            return Component.translatable(effect.getDescriptionId()).getString();
        }

        String path = location.getPath().replace('_', ' ');
        StringBuilder name = new StringBuilder();
        for (String part : path.split(" ")) {
            if (part.isEmpty()) continue;
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return name.isEmpty() ? id : name.toString();
    }
}
