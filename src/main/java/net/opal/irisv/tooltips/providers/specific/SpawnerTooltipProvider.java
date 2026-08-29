package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import javax.annotation.Nullable;
import java.util.List;

public class SpawnerTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, @Nullable BlockEntity be) {
        return state != null && state.getBlock() instanceof SpawnerBlock;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (!ConfigOptions.getInstance().advancedTooltips || !accessor.player().getAbilities().instabuild) return;

        CompoundTag data = accessor.serverData();
        if (data == null || data.isEmpty()) return;

        String mobId = getMobId(data);
        info.add("§dMob: §f" + mobName(mobId));

        int delay = data.getShort("Delay");
        if (delay >= 0) {
            info.add("§bDélai: §f" + formatTicks(delay));
        }

        int minDelay = data.getShort("MinSpawnDelay");
        int maxDelay = data.getShort("MaxSpawnDelay");
        if (minDelay > 0 || maxDelay > 0) {
            info.add("§8Cycle: §7" + formatTicks(minDelay) + " - " + formatTicks(maxDelay));
        }

        int spawnCount = data.getShort("SpawnCount");
        if (spawnCount > 0) {
            info.add("§8Spawn count: §7" + spawnCount);
        }
    }

    private static String getMobId(CompoundTag data) {
        if (!data.contains("SpawnData")) return "";

        CompoundTag spawnData = data.getCompound("SpawnData");
        if (spawnData.contains("entity")) {
            return spawnData.getCompound("entity").getString("id");
        }
        return spawnData.getString("id");
    }

    private static String mobName(String id) {
        if (id == null || id.isBlank()) return "Inconnu";

        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return id;

        var entityType = BuiltInRegistries.ENTITY_TYPE.get(location);
        if (entityType != null) {
            return Component.translatable(entityType.getDescriptionId()).getString();
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

    private static String formatTicks(int ticks) {
        if (ticks < 0) return "-";
        float seconds = ticks / 20.0F;
        if (seconds >= 60.0F) {
            int minutes = (int) (seconds / 60.0F);
            int remainingSeconds = (int) (seconds % 60.0F);
            return minutes + "m " + remainingSeconds + "s";
        }
        return String.format(java.util.Locale.ROOT, "%.1fs", seconds);
    }
}
