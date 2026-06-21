package net.opal.irisv.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.opal.irisv.network.server.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerDataSender {
    private static final Map<UUID, BlockPos> LAST_POS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_DATA_HASH = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % 5 != 0) return;

        HitResult hit = player.pick(5.0D, 0.0F, false);
        UUID uuid = player.getUUID();

        if (hit instanceof BlockHitResult blockHit) {
            // Toute la logique de traitement est déportée ici
            ServerDataSenderContainer.handleBlockLook(player, blockHit.getBlockPos());
        } else {
            // Nettoyage si le joueur ne regarde plus de bloc
            LAST_POS.remove(uuid);
            LAST_DATA_HASH.remove(uuid);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        LAST_POS.remove(uuid);
        LAST_DATA_HASH.remove(uuid);
    }

    public static boolean shouldUpdate(UUID uuid, BlockPos masterPos, int hash) {
        Integer lastHash = LAST_DATA_HASH.get(uuid);
        BlockPos lastMasterPos = LAST_POS.get(uuid);
        return !masterPos.equals(lastMasterPos) || lastHash == null || hash != lastHash;
    }

    public static void updateCache(UUID uuid, BlockPos pos, int hash) {
        LAST_POS.put(uuid, pos);
        LAST_DATA_HASH.put(uuid, hash);
    }

    public static void sendRawData(ServerPlayer player, BlockPos pos, CompoundTag nbt) {
        player.connection.send(new ClientboundCustomPayloadPacket(new BlockDataPayload(pos, nbt)));
    }
}
