package net.opal.irisv.network.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.opal.irisv.network.ServerDataSender;
import net.opal.irisv.mixin.FurnaceBlockEntityAccessor;

import java.util.UUID;

public class ServerDataSenderContainer {

    public static void handleBlockLook(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        UUID uuid = player.getUUID();

        // 1. Logique Ender Chest
        if (state.is(Blocks.ENDER_CHEST)) {
            handleEnderChest(player, uuid, pos);
            return;
        }

        // 2. Récupération des entités de bloc
        BlockEntity targetBE = player.level().getBlockEntity(pos);
        if (targetBE == null) return; // Sécurité : On a besoin d'un BE pour la suite

        BlockEntity neighborBE = getNeighborChest(player, pos, state);

        // 3. Détermination du Master Unique (Pivot stable)
        BlockPos masterPos = getMasterPos(pos, neighborBE);

        // 4. Calcul du hash global pour vérifier les changements
        int currentHash = calculateGlobalHash(player, masterPos, player.level().getBlockState(masterPos), targetBE, neighborBE);

        if (ServerDataSender.shouldUpdate(uuid, masterPos, currentHash)) {
            CompoundTag data = createSafeData(player, targetBE);

            var fluidHandler = player.level().getCapability(Capabilities.FluidHandler.BLOCK, pos, state, targetBE, null);
            if (fluidHandler != null && fluidHandler.getTanks() > 0) {
                var fluidStack = fluidHandler.getFluidInTank(0);
                data.putLong("RealAmount", fluidStack.getAmount());
                data.putLong("RealCapacity", fluidHandler.getTankCapacity(0));
                if (!fluidStack.isEmpty()) {
                    data.putString("FluidName", BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString());
                }
            }

            if (targetBE instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
                FurnaceBlockEntityAccessor accessor = (FurnaceBlockEntityAccessor) furnace;
                data.putInt("BurnTime", accessor.irisv$getLitTimeRemaining());
                data.putInt("BurnDuration", accessor.irisv$getLitTotalTime());
                data.putInt("CookTime", accessor.irisv$getCookingTimer());
                data.putInt("CookTimeTotal", accessor.irisv$getCookingTotalTime());
            }

            // Fusion des inventaires (Simple ou Double coffre)
            ListTag combined = new ListTag();
            fillCombinedInventory(player, pos, state, targetBE, neighborBE, combined);
            data.put("Items", combined);

            // Envoi des données (Synchronisation Client/Serveur)
            ServerDataSender.sendRawData(player, pos, data);
            if (neighborBE != null) {
                ServerDataSender.sendRawData(player, neighborBE.getBlockPos(), data);
            }

            // Mise à jour du cache pour éviter d'envoyer le paquet en boucle
            ServerDataSender.updateCache(uuid, masterPos, currentHash);
        }
    }

// --- MÉTHODES UTILITAIRES INTERNES (Tes logiques extraites) ---

    private static BlockPos getMasterPos(BlockPos pos, BlockEntity neighbor) {
        if (neighbor == null) return pos;
        BlockPos nPos = neighbor.getBlockPos();
        if (pos.getX() < nPos.getX() || (pos.getX() == nPos.getX() && pos.getZ() < nPos.getZ())) {
            return pos;
        }
        return nPos;
    }

    private static CompoundTag createSafeData(ServerPlayer player, BlockEntity blockEntity) {
        CompoundTag source = blockEntity.saveWithFullMetadata(player.level().registryAccess());
        return ServerDataSanitizer.sanitize(source, player.hasPermissions(2));
    }

    private static void fillCombinedInventory(ServerPlayer player, BlockPos pos, BlockState state, BlockEntity target, BlockEntity neighbor, ListTag combined) {
        IItemHandler handlerTarget = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, state, target, null);

        if (neighbor != null && state.hasProperty(ChestBlock.TYPE)) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                IItemHandler handlerNeighbor = player.level().getCapability(Capabilities.ItemHandler.BLOCK, neighbor.getBlockPos(), null, neighbor, null);

                if (handlerTarget != null && handlerTarget.getSlots() >= 54) {
                    addItemsToList(handlerTarget, combined, player);
                } else if (handlerTarget != null && handlerNeighbor != null) {
                    boolean isRight = type == ChestType.RIGHT;
                    IItemHandler first = isRight ? handlerNeighbor : handlerTarget;
                    IItemHandler second = isRight ? handlerTarget : handlerNeighbor;
                    addItemsToList(first, combined, player);
                    addItemsToList(second, combined, player, first.getSlots());
                }
                return;
            }
        }

        if (handlerTarget != null) addItemsToList(handlerTarget, combined, player);
    }

    public static void addItemsToList(IItemHandler handler, ListTag list, ServerPlayer player) {
        addItemsToList(handler, list, player, 0);
    }

    public static void addItemsToList(IItemHandler handler, ListTag list, ServerPlayer player, int slotOffset) {
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

    public static void handleEnderChest(ServerPlayer player, UUID uuid, BlockPos pos) {
        var inv = player.getEnderChestInventory();
        int hash = getContainerHash(inv);
        if (ServerDataSender.shouldUpdate(uuid, pos, hash)) {
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
            ServerDataSender.sendRawData(player, pos, nbt);
            ServerDataSender.updateCache(uuid, pos, hash);
        }
    }

    public static BlockEntity getNeighborChest(ServerPlayer player, BlockPos pos, BlockState state) {
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

    public static int calculateGlobalHash(ServerPlayer player, BlockPos pos, BlockState state, BlockEntity be, BlockEntity neighbor) {
        int h1 = getBEInventoryHash(player, pos, state, be);
        int h2 = (neighbor != null && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
                ? getBEInventoryHash(player, neighbor.getBlockPos(), neighbor.getBlockState(), neighbor) : 0;
        return (h1 + h2) ^ state.getBlock().hashCode();
    }

    public static int getBEInventoryHash(ServerPlayer player, BlockPos pos, BlockState state, BlockEntity be) {
        if (be == null) return 0;

        int h = 0;

        // 1. Hash de l'inventaire via IItemHandler (Capacité Forge/NeoForge)
        IItemHandler handler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, state, be, null);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                var s = handler.getStackInSlot(i);
                if (!s.isEmpty()) {
                    // On combine l'index, l'item, la quantité et les composants (enchantements, etc.)
                    h += i + s.getItem().hashCode() + s.getCount() + s.getComponents().hashCode();
                }
            }
        }
        // Fallback pour les containers classiques si nécessaire
        else if (be instanceof net.minecraft.world.Container c) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                var s = c.getItem(i);
                if (!s.isEmpty()) h += i + s.getItem().hashCode() + s.getCount();
            }
        }

        // Include machine progress and other changing block entity state.
        h += be.saveWithFullMetadata(player.level().registryAccess()).hashCode();

        return h;
    }

    public static int getContainerHash(Container c) {
        int h = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            var s = c.getItem(i);
            if (!s.isEmpty()) h += i + s.getItem().hashCode() + s.getCount() + s.getComponents().hashCode();
        }
        return h;
    }
}
