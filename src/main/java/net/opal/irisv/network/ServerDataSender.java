package net.opal.irisv.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerDataSender {
    private static final Map<UUID, BlockPos> LAST_POS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_DATA_HASH = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // --- OPTIMISATION 1 : RÉACTIVITÉ MAXIMALE ---
        // On supprime le % 2 pour vérifier à CHAQUE tick (20 fois par seconde)
        // C'est nécessaire pour que le changement d'item soit instantané à l'écran.

        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = player.level().getBlockState(pos);
            UUID uuid = player.getUUID();

            // --- CAS 1 : ENDER CHEST ---
            if (state.is(Blocks.ENDER_CHEST)) {
                var enderInventory = player.getEnderChestInventory();
                int currentHash = getContainerHash(enderInventory);
                if (shouldUpdate(uuid, pos, currentHash)) {
                    CompoundTag enderData = new CompoundTag();
                    enderData.put("Items", enderInventory.createTag(player.level().registryAccess()));
                    sendRawData(player, pos, enderData);
                    updateCache(uuid, pos, currentHash);
                }
                return;
            }

            // --- CAS 2 : AUTRES BLOCS (Coffres, Barils, etc.) ---
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be == null) return;

            // Calcul du hash global (Position + Contenu)
            int currentHash = getFullStateHash(state, be);

            // --- OPTIMISATION 2 : DÉTECTION BIDIRECTIONNELLE ---
            // Si c'est un coffre double, on inclut le hash du voisin dans le calcul
            // de mise à jour. Comme ça, si le contenu du voisin change, on renvoie tout.
            BlockPos neighborPos = null;
            BlockEntity neighborBE = null;

            if (state.hasProperty(ChestBlock.TYPE)) {
                ChestType type = state.getValue(ChestBlock.TYPE);
                if (type != ChestType.SINGLE) {
                    Direction facing = state.getValue(ChestBlock.FACING);
                    Direction side = (type == ChestType.LEFT) ? facing.getClockWise() : facing.getCounterClockWise();
                    neighborPos = pos.relative(side);
                    neighborBE = player.level().getBlockEntity(neighborPos);
                    if (neighborBE instanceof Container container) {
                        currentHash += getContainerHash(container) * 31; // On mélange le hash du voisin
                    }
                }
            }

            // Si le hash global (Moi + Voisin) a changé
            if (shouldUpdate(uuid, pos, currentHash)) {
                // Envoi du bloc visé
                sendRawData(player, pos, be.saveWithFullMetadata(player.level().registryAccess()));

                // Envoi immédiat du voisin s'il existe
                if (neighborPos != null && neighborBE != null) {
                    sendRawData(player, neighborPos, neighborBE.saveWithFullMetadata(player.level().registryAccess()));
                }

                updateCache(uuid, pos, currentHash);
            }
        } else {
            LAST_POS.remove(player.getUUID());
            LAST_DATA_HASH.remove(player.getUUID());
        }
    }

    private static int getFullStateHash(BlockState state, BlockEntity be) {
        int hash = state.hashCode();
        if (be instanceof Container container) {
            hash += getContainerHash(container);
        } else {
            hash += getTechnicalBlockHash(be);
        }
        return hash;
    }

    private static int getTechnicalBlockHash(BlockEntity be) {
        if (be instanceof CommandBlockEntity cmd) {
            return cmd.getCommandBlock().getCommand().hashCode();
        } else if (be instanceof StructureBlockEntity struct) {
            return struct.getStructureName().hashCode() + struct.getMode().hashCode();
        } else if (be instanceof JigsawBlockEntity jigsaw) {
            return jigsaw.getName().hashCode() + jigsaw.getTarget().hashCode() + jigsaw.getPool().hashCode();
        } else if (be instanceof SignBlockEntity sign) {
            return sign.getText(true).getMessage(0, false).getString().hashCode();
        }
        return 0;
    }

    private static boolean shouldUpdate(UUID uuid, BlockPos pos, int currentHash) {
        // Met à jour si la position a changé OU si le contenu (Hash) a changé
        return !pos.equals(LAST_POS.get(uuid)) || currentHash != LAST_DATA_HASH.getOrDefault(uuid, 0);
    }

    private static void updateCache(UUID uuid, BlockPos pos, int hash) {
        LAST_POS.put(uuid, pos);
        LAST_DATA_HASH.put(uuid, hash);
    }

    private static int getContainerHash(Container container) {
        int hash = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);
            if (!stack.isEmpty()) {
                // On inclut tout ce qui peut changer visuellement (quantité, nbt)
                hash += i + stack.getItem().hashCode() + stack.getCount() + stack.getComponents().hashCode();
            }
        }
        return hash;
    }

    private static void sendRawData(ServerPlayer player, BlockPos pos, CompoundTag nbt) {
        BlockDataPayload payload = new BlockDataPayload(pos, nbt);
        player.connection.send(new ClientboundCustomPayloadPacket(payload));
    }
}