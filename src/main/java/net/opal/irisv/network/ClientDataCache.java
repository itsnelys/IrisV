package net.opal.irisv.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDataCache {
    private static final Map<BlockPos, CompoundTag> CACHE = new ConcurrentHashMap<>();

    public static void update(BlockPos pos, CompoundTag tag) {
        // On vide si le cache est trop gros (comme Jade)
        if (CACHE.size() > 100) CACHE.clear();
        CACHE.put(pos, tag);
    }

    public static CompoundTag get(BlockPos pos) {
        return CACHE.getOrDefault(pos, new CompoundTag());
    }

    public static void handleData(final BlockDataPayload payload, IPayloadContext context) {
        // enqueueWork remplace le workHandler().execute()
        context.enqueueWork(() -> {
            update(payload.pos(), payload.tag());
        });
    }
}