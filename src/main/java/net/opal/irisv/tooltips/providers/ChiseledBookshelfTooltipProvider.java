package net.opal.irisv.tooltips.providers;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChiseledBookshelfTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.CHISELED_BOOKSHELF);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        CompoundTag data = accessor.serverData();

        // On vérifie "Items" (format standard) ou nos données injectées
        if (data == null || !data.contains("Items", 9)) {
            return;
        }

        ListTag tagList = data.getList("Items", 10);
        List<ItemStack> allBooks = new ArrayList<>();

        // Pré-chargement de tous les livres pour la preview globale
        for (int i = 0; i < tagList.size(); i++) {
            ItemStack stack = parseSmartStack(tagList.getCompound(i), accessor);
            if (!stack.isEmpty()) allBooks.add(stack);
        }

        boolean isLookingAtSlot = false;
        if (accessor.hit() instanceof BlockHitResult hit) {
            // Seule la face avant permet de voir les livres
            Optional<Integer> hitSlot = getHitSlot(hit, accessor.state());

            if (hitSlot.isPresent()) {
                isLookingAtSlot = true;
                int slot = hitSlot.get();
                ItemStack bookStack = getItemInSlot(tagList, slot, accessor);

                if (!bookStack.isEmpty()) {
                    accessor.setIcon(bookStack);
                    addBookDetails(info, bookStack, accessor);

                    // On vide la preview pour focus sur le nom du livre
                    accessor.setPreviewItems(new ArrayList<>());
                } else {
                    info.add("§8(Empty)");
                    accessor.setPreviewItems(new ArrayList<>());
                }
            }
        }

        if (!isLookingAtSlot) {
            if (allBooks.isEmpty()) {
                info.add("§8(Empty)");
            } else {
                accessor.setPreviewItems(allBooks);
            }
        }
    }

    // Méthode cruciale pour lire le count et le slot correctement
    private ItemStack parseSmartStack(CompoundTag itemTag, IBlockAccessor accessor) {
        ItemStack stack = ItemStack.parseOptional(accessor.level().registryAccess(), itemTag);
        if (!stack.isEmpty() && itemTag.contains("count")) {
            stack.setCount(itemTag.getInt("count"));
        }
        return stack;
    }

    private ItemStack getItemInSlot(ListTag tagList, int slot, IBlockAccessor accessor) {
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            // On vérifie le tag "Slot" que le ServerDataSender doit envoyer
            if (itemTag.contains("Slot") && itemTag.getInt("Slot") == slot) {
                return parseSmartStack(itemTag, accessor);
            }
        }
        return ItemStack.EMPTY;
    }

    private void addBookDetails(List<String> info, ItemStack stack, IBlockAccessor accessor) {
        var lines = stack.getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.of(accessor.level()),
                accessor.player(),
                TooltipFlag.Default.NORMAL
        );
        // Si c'est un livre enchanté, on affiche le premier enchantement
        if (lines.size() > 1) {
            info.add("§e" + lines.get(1).getString());
        }
    }

    private Optional<Integer> getHitSlot(BlockHitResult hit, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        // Si on ne regarde pas la face avant, on ne renvoie pas de slot
        if (hit.getDirection() != facing) return Optional.empty();

        double x = hit.getLocation().x - hit.getBlockPos().getX();
        double y = hit.getLocation().y - hit.getBlockPos().getY();
        double z = hit.getLocation().z - hit.getBlockPos().getZ();

        // Mapping des coordonnées UV vers les slots 0-5
        Vec2 rel = switch (facing) {
            case NORTH -> new Vec2(1.0F - (float)x, (float)y);
            case SOUTH -> new Vec2((float)x, (float)y);
            case WEST -> new Vec2((float)z, (float)y);
            case EAST -> new Vec2(1.0F - (float)z, (float)y);
            default -> Vec2.ZERO;
        };

        int row = rel.y >= 0.5F ? 0 : 1;
        int col = rel.x < 0.375F ? 0 : (rel.x < 0.6875F ? 1 : 2);
        return Optional.of(col + row * 3);
    }
}