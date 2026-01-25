package net.opal.irisv.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerDataSender {
    private static final Map<UUID, BlockPos> LAST_POS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // On vérifie toutes les 4 ticks pour ne pas surcharger le processeur
            if (player.level().getGameTime() % 4 != 0) return;

            HitResult hit = player.pick(5.0D, 0.0F, false);
            if (hit instanceof BlockHitResult blockHit) {
                BlockPos pos = blockHit.getBlockPos();

                // Si le joueur regarde déjà ce bloc, on ne renvoie pas le paquet inutilement
                if (pos.equals(LAST_POS.get(player.getUUID()))) return;

                BlockEntity be = player.level().getBlockEntity(pos);
                if (be != null) {
                    // 1. On récupère les registries du niveau actuel
                    var registryAccess = player.level().registryAccess();

                    // 2. On crée le payload en passant le NBT sauvegardé avec les registries
                    BlockDataPayload payload = new BlockDataPayload(
                            pos,
                            be.saveWithFullMetadata(registryAccess) // Correction : nécessite l'argument ici
                    );

                    // 3. On enveloppe le tout dans le paquet de Minecraft 1.21
                    var packet = new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(payload);

                    // 4. On envoie
                    player.connection.send(packet);

                    LAST_POS.put(player.getUUID(), pos);
                }
            } else {
                LAST_POS.remove(player.getUUID());
            }
        }
    }
}