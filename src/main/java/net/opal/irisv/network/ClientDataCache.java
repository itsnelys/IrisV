package net.opal.irisv.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDataCache {
    private static final Map<BlockPos, CompoundTag> CACHE = new ConcurrentHashMap<>();

    public static void update(BlockPos pos, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            remove(pos); // Utilise la méthode remove ci-dessous
        } else {
            if (CACHE.size() > 100) CACHE.clear();
            CACHE.put(pos, tag);
        }
    }

    // --- AJOUTE CETTE MÉTHODE ---
    public static void remove(BlockPos pos) {
        if (pos != null) {
            CACHE.remove(pos);
        }
    }

    public static CompoundTag get(BlockPos pos) {
        return CACHE.getOrDefault(pos, new CompoundTag());
    }

    public static void handleData(final BlockDataPayload payload, IPayloadContext context) {
        update(payload.pos(), payload.tag());
    }
}