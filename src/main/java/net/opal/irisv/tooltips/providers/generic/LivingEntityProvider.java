package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart; // Import indispensable
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IEntityTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.ArrayList;
import java.util.List;

public class LivingEntityProvider implements IEntityTooltipProvider {

    @Override
    public boolean isApplicable(Entity entity) {
        // On accepte les LivingEntity OU les parties de dragon
        return entity instanceof LivingEntity || entity instanceof EnderDragonPart;
    }

    @Override
    public void addTooltip(List<String> tooltip, Entity entity, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().enableEntityTooltip) {
            // --- FIX LOGIQUE DE DÉTECTION ---
            // Si l'entité pointée est une partie de dragon, on récupère le dragon lui-même
            LivingEntity living;
            if (entity instanceof EnderDragonPart part && part.parentMob instanceof LivingEntity parent) {
                living = parent;
            } else if (entity instanceof LivingEntity le) {
                living = le;
            } else {
                return; // Sécurité
            }

            // --- 1. GESTION DE L'ICÔNE ET DU TITRE ---
            if (living instanceof Player player) {
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
                accessor.setIcon(head);
                accessor.setTitleOverride("§b" + player.getScoreboardName());
            } else if (living instanceof EnderDragon) {
                accessor.setIcon(new ItemStack(Items.DRAGON_HEAD));
                accessor.setTitleOverride("§5Ender Dragon"); // Optionnel : force le nom en violet
            } else {
                accessor.setIcon(living.getPickResult());
            }

            // --- 2. SANTÉ ---
            if (living.getHealth() > 0 && !(living instanceof ArmorStand)) {
                float health = living.getHealth();
                float maxHealth = living.getMaxHealth();
                tooltip.add("hp_render:" + health + "/" + maxHealth);
            }

            // --- 3. ÉQUIPEMENT (PREVIEW) ---
            List<ItemStack> preview = new ArrayList<>();
            if (!living.getMainHandItem().isEmpty()) preview.add(living.getMainHandItem().copy());
            if (!living.getOffhandItem().isEmpty()) preview.add(living.getOffhandItem().copy());

            for (ItemStack armor : living.getArmorSlots()) {
                if (!armor.isEmpty()) preview.add(armor.copy());
            }

            if (!preview.isEmpty()) {
                accessor.setPreviewItems(preview);
            }

            // --- 4. MODES DE JEU ---
            if (living instanceof Player p) {
                if (p.isCreative()) tooltip.add("§7Mode: §dCréatif");
                else if (p.isSpectator()) tooltip.add("§7Mode: §8Spectateur");
            }
        }
    }
}