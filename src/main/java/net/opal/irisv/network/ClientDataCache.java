package net.opal.irisv.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDataCache {
    private static final Map<BlockPos, CompoundTag> CACHE = new ConcurrentHashMap<>();

    public static void update(BlockPos pos, CompoundTag tag) {
        // Si le tag est vide (disque retiré), on nettoie la position
        if (tag == null || tag.isEmpty()) {
            CACHE.remove(pos);
        } else {
            // Nettoyage de sécurité si le joueur explore beaucoup
            if (CACHE.size() > 100) CACHE.clear();
            CACHE.put(pos, tag);
        }
    }

    public static CompoundTag get(BlockPos pos) {
        return CACHE.getOrDefault(pos, new CompoundTag());
    }

    public static void handleData(final BlockDataPayload payload, IPayloadContext context) {
        // Mise à jour immédiate hors du thread principal pour éviter tout lag réseau
        update(payload.pos(), payload.tag());
    }
}