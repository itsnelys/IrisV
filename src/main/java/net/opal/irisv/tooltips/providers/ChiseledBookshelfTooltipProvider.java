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

        // Si aucune donnée serveur, on peut quand même indiquer que c'est une bibliothèque
        if (data == null || !data.contains("Items", 9)) {
            // Optionnel : info.add("§8(Vide)");
            return;
        }

        ListTag tagList = data.getList("Items", 10);
        List<ItemStack> allBooks = new ArrayList<>();

        for (int i = 0; i < tagList.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(accessor.level().registryAccess(), tagList.getCompound(i));
            if (!stack.isEmpty()) allBooks.add(stack);
        }

        boolean isLookingAtSlot = false;
        if (accessor.hit() instanceof BlockHitResult hit) {
            Optional<Integer> hitSlot = getHitSlot(hit, accessor.state());

            if (hitSlot.isPresent()) {
                isLookingAtSlot = true; // On regarde un slot précis
                int slot = hitSlot.get();
                ItemStack bookStack = getItemInSlot(tagList, slot, accessor);

                if (!bookStack.isEmpty()) {
                    accessor.setIcon(bookStack);
                    addBookDetails(info, bookStack, accessor);
                    // On vide la preview pour ne pas encombrer quand on focus un livre
                    accessor.setPreviewItems(new ArrayList<>());
                } else {
                    // AJOUT : Si on regarde un slot spécifique mais qu'il est vide
                    info.add("§8(Empty)");
                    accessor.setPreviewItems(new ArrayList<>());
                }
            }
        }

        // Si on ne regarde pas de slot spécifique (ex: côté du bloc), on montre tout
        if (!isLookingAtSlot) {
            if (allBooks.isEmpty()) {
                info.add("§8(Empty)");
            } else {
                accessor.setPreviewItems(allBooks);
            }
        }
    }

    private ItemStack getItemInSlot(ListTag tagList, int slot, IBlockAccessor accessor) {
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            if (itemTag.getByte("Slot") == slot) {
                return ItemStack.parseOptional(accessor.level().registryAccess(), itemTag);
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
        if (lines.size() > 1) {
            String color = (stack.is(Items.WRITTEN_BOOK) || stack.is(Items.WRITABLE_BOOK)) ? "§3§o" : "§e§o";
            info.add(color + lines.get(1).getString());
        }
    }

    private Optional<Integer> getHitSlot(BlockHitResult hit, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (hit.getDirection() != facing) return Optional.empty();

        double x = hit.getLocation().x - hit.getBlockPos().getX();
        double y = hit.getLocation().y - hit.getBlockPos().getY();
        double z = hit.getLocation().z - hit.getBlockPos().getZ();

        Vec2 rel = switch (facing) {
            case NORTH -> new Vec2(1.0F - (float)x, (float)y);
            case SOUTH -> new Vec2((float)x, (float)y);
            case WEST -> new Vec2((float)z, (float)y);
            case EAST -> new Vec2(1.0F - (float)z, (float)y);
            default -> Vec2.ZERO;
        };

        // Calcul des lignes/colonnes selon le mapping des bibliothèques sculptées
        int row = rel.y >= 0.5F ? 0 : 1;
        int col = rel.x < 0.375F ? 0 : (rel.x < 0.6875F ? 1 : 2);
        return Optional.of(col + row * 3);
    }
}