package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand; // Import requis
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IEntityTooltipProvider;

import java.util.ArrayList;
import java.util.List;

public class LivingEntityProvider implements IEntityTooltipProvider {

    @Override
    public boolean isApplicable(Entity entity) {
        // S'applique aux entités vivantes (joueurs, mobs, armor stands)
        return entity instanceof LivingEntity;
    }

    @Override
    public void addTooltip(List<String> tooltip, Entity entity, IBlockAccessor accessor) {
        LivingEntity living = (LivingEntity) entity;

        // --- 1. GESTION DE L'ICÔNE ET DU TITRE ---
        if (living instanceof Player player) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
            accessor.setIcon(head);
            accessor.setTitleOverride("§b" + player.getScoreboardName());
        } else {
            accessor.setIcon(living.getPickResult());
        }

        // --- 2. SANTÉ (COEURS) ---
        // On n'affiche PAS de coeurs si c'est un Armor Stand
        if (living.getHealth() > 0 && !(living instanceof ArmorStand)) {
            float health = living.getHealth();
            float maxHealth = living.getMaxHealth();
            // Le tag "hp_render:" est traité par TooltipOverlayRenderer
            tooltip.add("hp_render:" + health + "/" + maxHealth);
        }

        // --- 3. ÉQUIPEMENT (PREVIEW) ---
        // On garde cette partie active pour que l'Armor Stand affiche ses items
        List<ItemStack> preview = new ArrayList<>();

        if (!living.getMainHandItem().isEmpty()) preview.add(living.getMainHandItem().copy());
        if (!living.getOffhandItem().isEmpty()) preview.add(living.getOffhandItem().copy());

        for (ItemStack armor : living.getArmorSlots()) {
            if (!armor.isEmpty()) {
                preview.add(armor.copy());
            }
        }

        if (!preview.isEmpty()) {
            accessor.setPreviewItems(preview);
        }

        // --- 4. MODES DE JEU (JOUEURS UNIQUEMENT) ---
        if (living instanceof Player p) {
            if (p.isCreative()) {
                tooltip.add("§7Mode: §dCréatif");
            } else if (p.isSpectator()) {
                tooltip.add("§7Mode: §8Spectateur");
            }
        }
    }
}