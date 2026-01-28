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

            if (state.is(Blocks.ENDER_CHEST)) {
                handleEnderChest(player, uuid, pos);
                return;
            }

            BlockEntity targetBE = player.level().getBlockEntity(pos);
            BlockEntity neighborBE = getNeighborChest(player, pos, state);

            // Détermination du Master Unique (Pivot stable)
            BlockEntity masterBE;
            if (neighborBE != null) {
                BlockPos posA = pos;
                BlockPos posB = neighborBE.getBlockPos();
                if (posA.getX() < posB.getX() || (posA.getX() == posB.getX() && posA.getZ() < posB.getZ())) {
                    masterBE = targetBE;
                } else {
                    masterBE = neighborBE;
                }
            } else {
                masterBE = targetBE;
            }

            if (masterBE == null) return;
            BlockPos masterPos = masterBE.getBlockPos();

            int currentHash = calculateGlobalHash(player, masterPos, player.level().getBlockState(masterPos), masterBE, neighborBE);

            if (shouldUpdate(uuid, masterPos, currentHash)) {
                CompoundTag data = targetBE.saveWithFullMetadata(player.level().registryAccess());

                // Nettoyage des inventaires existants pour éviter les conflits
                data.remove("Items");
                data.remove("inventory");
                data.remove("Inventory");
                data.remove("storage");

                ListTag combined = new ListTag();
                IItemHandler handlerTarget = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, state, targetBE, null);

                // --- LOGIQUE DE FUSION ET RÉCUPÉRATION ---
                if (neighborBE != null && state.hasProperty(ChestBlock.TYPE)) {
                    ChestType type = state.getValue(ChestBlock.TYPE);
                    if (type != ChestType.SINGLE) {
                        IItemHandler handlerNeighbor = player.level().getCapability(Capabilities.ItemHandler.BLOCK, neighborBE.getBlockPos(), null, neighborBE, null);
                        if (handlerTarget != null && handlerTarget.getSlots() >= 54) {
                            addItemsToList(handlerTarget, combined, player);
                        } else if (handlerTarget != null && handlerNeighbor != null) {
                            boolean isRight = type == ChestType.RIGHT;
                            IItemHandler first = isRight ? handlerNeighbor : handlerTarget;
                            IItemHandler second = isRight ? handlerTarget : handlerNeighbor;
                            addItemsToList(first, combined, player);
                            addItemsToList(second, combined, player, first.getSlots()); // Offset pour les slots
                        }
                    } else {
                        if (handlerTarget != null) addItemsToList(handlerTarget, combined, player);
                    }
                } else if (handlerTarget != null) {
                    addItemsToList(handlerTarget, combined, player);
                }

                data.put("Items", combined);
                sendRawData(player, pos, data);

                if (neighborBE != null) {
                    sendRawData(player, neighborBE.getBlockPos(), data);
                }

                updateCache(uuid, masterPos, currentHash);
            }
        } else {
            LAST_POS.remove(player.getUUID());
            LAST_DATA_HASH.remove(player.getUUID());
        }
    }

    // Méthode de base sans offset
    private static void addItemsToList(IItemHandler handler, ListTag list, ServerPlayer player) {
        addItemsToList(handler, list, player, 0);
    }

    // Méthode avec support du tag "Slot" et "count" (Correction crash + bibliothèque)
    private static void addItemsToList(IItemHandler handler, ListTag list, ServerPlayer player, int slotOffset) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int realCount = stack.getCount();

                // On force le count à 1 pour la validation MC 1.21.4 (Évite le crash)
                ItemStack safetyStack = stack.copy();
                safetyStack.setCount(1);

                CompoundTag itemTag = (CompoundTag) safetyStack.save(player.level().registryAccess());

                // On injecte le vrai nombre et le numéro de slot (Nécessaire pour Hit & Name)
                itemTag.putInt("count", realCount);
                itemTag.putInt("Slot", i + slotOffset);

                list.add(itemTag);
            }
        }
    }

    private static void handleEnderChest(ServerPlayer player, UUID uuid, BlockPos pos) {
        var inv = player.getEnderChestInventory();
        int hash = getContainerHash(inv);
        if (shouldUpdate(uuid, pos, hash)) {
            CompoundTag nbt = new CompoundTag();
            ListTag list = new ListTag();

            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    ItemStack safety = stack.copy();
                    int count = safety.getCount();
                    safety.setCount(1);
                    CompoundTag tag = (CompoundTag) safety.save(player.level().registryAccess());
                    tag.putInt("count", count);
                    tag.putInt("Slot", i);
                    list.add(tag);
                }
            }

            nbt.put("Items", list);
            sendRawData(player, pos, nbt);
            updateCache(uuid, pos, hash);
        }
    }

    private static BlockEntity getNeighborChest(ServerPlayer player, BlockPos pos, BlockState state) {
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos nPos = pos.relative(dir);
                BlockState nState = player.level().getBlockState(nPos);
                if (nState.is(state.getBlock()) && nState.hasProperty(ChestBlock.TYPE)) {
                    if (nState.getValue(ChestBlock.TYPE) != ChestType.SINGLE && nState.getValue(ChestBlock.TYPE) != type) {
                        return player.level().getBlockEntity(nPos);
                    }
                }
            }
        }
        return null;
    }

    private static int calculateGlobalHash(ServerPlayer player, BlockPos pos, BlockState state, BlockEntity be, BlockEntity neighbor) {
        int h1 = getBEInventoryHash(player, pos, state, be);
        int h2 = (neighbor != null && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
                ? getBEInventoryHash(player, neighbor.getBlockPos(), neighbor.getBlockState(), neighbor) : 0;
        return (h1 + h2) ^ state.getBlock().hashCode();
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