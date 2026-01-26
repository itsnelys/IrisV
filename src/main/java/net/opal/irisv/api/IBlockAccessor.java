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

/**
 * Accesseur de données pour les tooltips de blocs.
 * Gère l'icône, le titre personnalisé et la liste d'items pour la preview.
 */
public record IBlockAccessor(
        Level level,
        Player player,
        BlockPos pos,
        BlockState state,
        @Nullable BlockEntity blockEntity,
        CompoundTag serverData,
        HitResult hit,
        ItemStack[] iconContainer,          // Conteneur pour l'icône d'override
        List<ItemStack>[] inventoryContainer, // Conteneur pour la liste des items (Preview)
        String[] titleContainer             // Conteneur pour le titre d'override
) {

    // --- GESTION DU TITRE ---

    /**
     * Remplace le nom du bloc par un titre personnalisé.
     */
    public void setTitleOverride(String title) {
        if (titleContainer.length > 0) {
            titleContainer[0] = title;
        }
    }

    public @Nullable String getTitleOverride() {
        return (titleContainer.length > 0) ? titleContainer[0] : null;
    }

    // --- GESTION DE L'ICÔNE ---

    /**
     * Remplace l'icône du bloc affichée à gauche.
     */
    public void setIcon(ItemStack stack) {
        if (iconContainer.length > 0) {
            iconContainer[0] = stack;
        }
    }

    public ItemStack getIcon() {
        return (iconContainer.length > 0) ? iconContainer[0] : ItemStack.EMPTY;
    }

    // --- GESTION DE LA PREVIEW D'INVENTAIRE ---

    /**
     * Définit la liste des items à afficher dans la preview (mode liste ou grille).
     */
    public void setPreviewItems(List<ItemStack> items) {
        if (inventoryContainer.length > 0) {
            inventoryContainer[0] = items;
        }
    }

    public List<ItemStack> getPreviewItems() {
        if (inventoryContainer.length > 0 && inventoryContainer[0] != null) {
            return inventoryContainer[0];
        }
        return new ArrayList<>();
    }
}