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

            // 1. RECHERCHE DU VOISIN
            BlockEntity targetBE = player.level().getBlockEntity(pos);
            BlockEntity neighborBE = getNeighborChest(player, pos, state);





// 2. DÉTERMINATION DU MASTER UNIQUE (Ton système de Pivot stable)
            BlockEntity masterBE;
            if (neighborBE != null) {
                BlockPos posA = pos;
                BlockPos posB = neighborBE.getBlockPos();

                // On élit le Master selon les coordonnées pour avoir un point fixe (Pivot)
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

            // Log de stabilité pour vérifier que le Master ne change pas quand on bouge la souris
            System.out.println("[IRIS-DEBUG] Pointé: " + pos + " | Master Officiel: " + masterPos);

            // 3. CALCUL DU HASH (Basé sur le Master stable élu)
            int currentHash = calculateGlobalHash(player, masterPos, player.level().getBlockState(masterPos), masterBE, neighborBE);

            if (shouldUpdate(uuid, masterPos, currentHash)) {
                // 1. On récupère les données du bloc pointé
                CompoundTag data = targetBE.saveWithFullMetadata(player.level().registryAccess());

                // --- NETTOYAGE AGRESSIF (Anti-doublon Master) ---
                // On supprime TOUTES les clés d'inventaire connues pour repartir de zéro
                data.remove("Items");      // Vanilla / Common
                data.remove("inventory");  // Sophisticated / Forge
                data.remove("Inventory");  // Divers mods
                data.remove("storage");    // Divers mods

                ListTag combined = new ListTag();
                IItemHandler handlerTarget = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, state, targetBE, null);

                // --- LOGIQUE DE FUSION ---
                if (neighborBE != null && state.hasProperty(ChestBlock.TYPE)) {
                    // CAS VANILLA DOUBLE
                    ChestType type = state.getValue(ChestBlock.TYPE);
                    if (type != ChestType.SINGLE) {
                        IItemHandler handlerNeighbor = player.level().getCapability(Capabilities.ItemHandler.BLOCK, neighborBE.getBlockPos(), null, neighborBE, null);

                        if (handlerTarget != null && handlerTarget.getSlots() >= 54) {
                            // Si le handler est déjà fusionné (mod de compatibilité)
                            addItemsToList(handlerTarget, combined, player);
                        } else if (handlerTarget != null && handlerNeighbor != null) {
                            // Fusion manuelle GAUCHE + DROITE
                            boolean isRight = type == ChestType.RIGHT;
                            IItemHandler first = isRight ? handlerNeighbor : handlerTarget;
                            IItemHandler second = isRight ? handlerTarget : handlerNeighbor;
                            addItemsToList(first, combined, player);
                            addItemsToList(second, combined, player);
                        }
                    } else {
                        if (handlerTarget != null) addItemsToList(handlerTarget, combined, player);
                    }
                }
                else if (neighborBE != null && handlerTarget != null) {
                    // CAS MODDÉ DOUBLE
                    IItemHandler handlerNeighbor = player.level().getCapability(Capabilities.ItemHandler.BLOCK, neighborBE.getBlockPos(), null, neighborBE, null);
                    if (handlerTarget == handlerNeighbor || handlerTarget.getSlots() >= 54) {
                        addItemsToList(handlerTarget, combined, player);
                    } else {
                        addItemsToList(handlerTarget, combined, player);
                    }
                }
                else if (handlerTarget != null) {
                    // BLOC ISOLÉ (On recrée un tag propre comme tu as fait)
                    CompoundTag cleanData = new CompoundTag();
                    if (data.contains("id")) cleanData.put("id", data.get("id"));
                    if (data.contains("CustomName")) cleanData.put("CustomName", data.get("CustomName"));

                    addItemsToList(handlerTarget, combined, player);
                    data = cleanData;
                }

                // 3. Injection finale de la liste UNIQUE
                data.put("Items", combined);

                // --- 4. ENVOI SYNCHRONISÉ (La correction est ici) ---
                // On envoie au bloc que l'on regarde actuellement
                sendRawData(player, pos, data);

                // Si c'est un double coffre, on force l'envoi au voisin AUSSI
                // pour que les deux moitiés soient identiques côté client.
                if (neighborBE != null) {
                    sendRawData(player, neighborBE.getBlockPos(), data);
                }

                // On met à jour le cache uniquement APRÈS avoir envoyé aux deux
                updateCache(uuid, masterPos, currentHash);
            }
        } else {
            LAST_POS.remove(player.getUUID());
            LAST_DATA_HASH.remove(player.getUUID());
        }
    }

    private static void addItemsToList(IItemHandler handler, ListTag list, ServerPlayer player) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = (CompoundTag) stack.save(player.level().registryAccess());
                // INDISPENSABLE pour Chiseled Bookshelf et le tri
                itemTag.putByte("Slot", (byte) i);
                list.add(itemTag);
            }
        }
    }

    private static BlockEntity getNeighborChest(ServerPlayer player, BlockPos pos, BlockState state) {
        // 1. VANILLA : Correction de la détection
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos nPos = pos.relative(dir);
                BlockState nState = player.level().getBlockState(nPos);

                // On vérifie si le voisin est un coffre et s'il est de l'autre type (LEFT si on est RIGHT, etc.)
                if (nState.is(state.getBlock()) && nState.hasProperty(ChestBlock.TYPE)) {
                    ChestType nType = nState.getValue(ChestBlock.TYPE);
                    if (nType != ChestType.SINGLE && nType != type) {
                        return player.level().getBlockEntity(nPos);
                    }
                }
            }
        }

        // 2. MODS (Sophisticated Storage / Capabilities)
        BlockEntity currentBE = player.level().getBlockEntity(pos);
        if (currentBE == null) return null;

        var currentHandler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, state, currentBE, null);
        if (currentHandler != null) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos nPos = pos.relative(dir);
                BlockEntity nBE = player.level().getBlockEntity(nPos);
                if (nBE != null) {
                    var nHandler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, nPos, player.level().getBlockState(nPos), nBE, null);
                    if (nHandler != null && nHandler == currentHandler) return nBE;
                }
            }
        }
        return null;
    }

    private static int calculateGlobalHash(ServerPlayer player, BlockPos pos, BlockState state, BlockEntity be, BlockEntity neighbor) {
        int h1 = getBEInventoryHash(player, pos, state, be);
        int h2 = 0;

        if (neighbor != null) {
            // On ne hash le voisin que si c'est un VRAI double coffre Vanilla
            if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                h2 = getBEInventoryHash(player, neighbor.getBlockPos(), neighbor.getBlockState(), neighbor);
            }
            // Pour les mods, si c'est le même inventaire, le hash h1 suffit déjà !
        }

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