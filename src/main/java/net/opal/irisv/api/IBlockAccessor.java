package net.opal.irisv.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class IBlockAccessor {
    private final Level level;
    private final Player player;
    private final BlockPos pos;
    private final BlockState state;
    private final BlockEntity blockEntity;
    private final CompoundTag serverData;
    private final HitResult hit;
    private ItemStack icon = ItemStack.EMPTY;
    private List<ItemStack> previewItems = new ArrayList<>();
    private String titleOverride;

    public IBlockAccessor(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable BlockState state,
            @Nullable BlockEntity blockEntity,
            @Nullable CompoundTag serverData,
            @Nullable HitResult hit
    ) {
        this.level = level;
        this.player = player;
        this.pos = pos;
        this.state = state;
        this.blockEntity = blockEntity;
        this.serverData = serverData;
        this.hit = hit;
    }

    public Level level() { return level; }
    public Player player() { return player; }
    public BlockPos pos() { return pos; }
    public @Nullable BlockState state() { return state; }
    public @Nullable BlockEntity blockEntity() { return blockEntity; }
    public @Nullable CompoundTag serverData() { return serverData; }
    public @Nullable HitResult hit() { return hit; }

    public void setTitleOverride(String title) { titleOverride = title; }
    public @Nullable String getTitleOverride() { return titleOverride; }

    public void setIcon(ItemStack stack) { icon = stack == null ? ItemStack.EMPTY : stack; }
    public ItemStack getIcon() { return icon; }

    public void setPreviewItems(List<ItemStack> items) {
        previewItems = items == null ? new ArrayList<>() : items;
    }

    public List<ItemStack> getPreviewItems() { return previewItems; }
}
