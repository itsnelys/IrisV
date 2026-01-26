package net.opal.irisv.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerDataSender {
    private static final Map<UUID, BlockPos> LAST_POS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_DATA_HASH = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % 2 != 0) return;

        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = player.level().getBlockState(pos);
            UUID uuid = player.getUUID();

            if (LAST_POS.containsKey(uuid)) {
                sendRawData(player, LAST_POS.get(uuid), new CompoundTag());
                LAST_POS.remove(uuid);
                LAST_DATA_HASH.remove(uuid);
            }

            if (state.is(Blocks.ENDER_CHEST)) {
                handleEnderChest(player, uuid, pos);
                return;
            }

            // 1. RECHERCHE DU VOISIN (Basée sur le bloc, pas sur les items)
            BlockEntity targetBE = player.level().getBlockEntity(pos);
            BlockEntity neighborBE = getNeighborChest(player, pos, state);

            // 2. DÉTERMINATION DU MASTER (Source des données)
            // Si on regarde un bloc sans BE (esclave moddé), on utilise le voisin
            BlockEntity masterBE = (targetBE != null) ? targetBE : neighborBE;
            if (masterBE == null) return;

            BlockPos masterPos = masterBE.getBlockPos();

            // 3. CALCUL DU HASH (Stable : basé sur le Master)
            int currentHash = calculateGlobalHash(player, masterPos, player.level().getBlockState(masterPos), masterBE, neighborBE);

            // 4. MISE À JOUR (On compare masterPos pour éviter les sauts au lancement)
            if (shouldUpdate(uuid, masterPos, currentHash)) {
                CompoundTag data = masterBE.saveWithFullMetadata(player.level().registryAccess());

                // Fusion Double Coffre
                if (neighborBE != null) {
                    CompoundTag neighborData = neighborBE.saveWithFullMetadata(player.level().registryAccess());
                    ListTag combined = new ListTag();
                    ListTag first = data.getList("Items", 10);
                    ListTag second = neighborData.getList("Items", 10);

                    // Respect de l'ordre Vanilla
                    if (state.hasProperty(ChestBlock.TYPE)) {
                        boolean isRight = state.getValue(ChestBlock.TYPE) == ChestType.RIGHT;
                        combined.addAll(isRight ? first : second);
                        combined.addAll(isRight ? second : first);
                    } else {
                        combined.addAll(first);
                        combined.addAll(second);
                    }
                    data.put("Items", combined);
                }

                // SYNC : On envoie à la position regardée (pour le tooltip)
                sendRawData(player, pos, data);

                // Si on est sur l'esclave, on envoie aussi au master pour peupler le cache client
                if (!pos.equals(masterPos)) {
                    sendRawData(player, masterPos, data);
                }

                updateCache(uuid, masterPos, currentHash);
            }
        } else {
            LAST_POS.remove(player.getUUID());
            LAST_DATA_HASH.remove(player.getUUID());
        }
    }

    private static BlockEntity getNeighborChest(ServerPlayer player, BlockPos pos, BlockState state) {
        // 1. VANILLA : On garde la logique ChestType (Indispensable pour les doubles coffres normaux)
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction facing = state.getValue(ChestBlock.FACING);
            Direction side = (state.getValue(ChestBlock.TYPE) == ChestType.LEFT) ? facing.getClockWise() : facing.getCounterClockWise();
            return player.level().getBlockEntity(pos.relative(side));
        }

        // 2. MODS : Jade utilise l'identité des instances
        BlockEntity currentBE = player.level().getBlockEntity(pos);
        if (currentBE == null) return null;

        var currentHandler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, state, currentBE, null);
        if (currentHandler == null) return null;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos nPos = pos.relative(dir);
            BlockEntity nBE = player.level().getBlockEntity(nPos);
            if (nBE != null) {
                var nHandler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, nPos, player.level().getBlockState(nPos), nBE, null);
                // SI LES DEUX HANDLERS SONT LE MÊME OBJET EN MÉMOIRE = Double coffre moddé
                if (nHandler == currentHandler) return nBE;
            }
        }
        return null;
    }

    private static int calculateGlobalHash(ServerPlayer player, BlockPos pos, BlockState state, BlockEntity be, BlockEntity neighbor) {
        int h = state.hashCode();
        h += getBEInventoryHash(player, pos, state, be);
        if (neighbor != null) {
            h += getBEInventoryHash(player, neighbor.getBlockPos(), neighbor.getBlockState(), neighbor) * 31;
        }
        return h;
    }

    private static int getBEInventoryHash(ServerPlayer player, BlockPos pos, BlockState state, BlockEntity be) {
        if (be == null) return 0;
        IItemHandler handler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, state, be, null);
        if (handler != null) {
            int h = 0;
            for (int i = 0; i < handler.getSlots(); i++) {
                var s = handler.getStackInSlot(i);
                if (!s.isEmpty()) h += i + s.getItem().hashCode() + s.getCount() + s.getComponents().hashCode();
            }
            return h;
        }
        return (be instanceof Container c) ? getContainerHash(c) : 0;
    }

    private static void handleEnderChest(ServerPlayer player, UUID uuid, BlockPos pos) {
        var inv = player.getEnderChestInventory();
        int hash = getContainerHash(inv);
        if (shouldUpdate(uuid, pos, hash)) {
            CompoundTag nbt = new CompoundTag();
            nbt.put("Items", inv.createTag(player.level().registryAccess()));
            sendRawData(player, pos, nbt);
            updateCache(uuid, pos, hash);
        }
    }

    private static int getContainerHash(Container c) {
        int h = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            var s = c.getItem(i);
            if (!s.isEmpty()) h += i + s.getItem().hashCode() + s.getCount() + s.getComponents().hashCode();
        }
        return h;
    }

    private static boolean shouldUpdate(UUID uuid, BlockPos masterPos, int hash) {
        Integer lastHash = LAST_DATA_HASH.get(uuid);
        BlockPos lastMasterPos = LAST_POS.get(uuid);
        return !masterPos.equals(lastMasterPos) || lastHash == null || hash != lastHash;
    }

    private static void updateCache(UUID uuid, BlockPos pos, int hash) {
        LAST_POS.put(uuid, pos);
        LAST_DATA_HASH.put(uuid, hash);
    }

    private static void sendRawData(ServerPlayer player, BlockPos pos, CompoundTag nbt) {
        player.connection.send(new ClientboundCustomPayloadPacket(new BlockDataPayload(pos, nbt)));
    }
}