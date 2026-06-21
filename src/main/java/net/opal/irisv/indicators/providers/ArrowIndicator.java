package net.opal.irisv.indicators.providers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.opal.irisv.api.IIndicator;
import net.opal.irisv.option.ConfigOptions;

public class ArrowIndicator implements IIndicator {

    @Override
    public boolean isVisible() {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || !ConfigOptions.getInstance().enableIndicators) return false;

        // Visible en permanence si le joueur possède au moins une flèche
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() instanceof ArrowItem) return true;
        }

        // Ou si le joueur tient une arme (cas de l'arc Infinity sans flèches en inventaire)
        return player.getMainHandItem().getItem() instanceof ProjectileWeaponItem ||
                player.getOffhandItem().getItem() instanceof ProjectileWeaponItem;
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, float partialTick) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null) return;

        // 1. DÉTERMINER LA MUNITION PRIORITAIRE POUR L'ICÔNE
        ItemStack nextArrow = ProjectileWeaponItem.getHeldProjectile(player, stack -> stack.getItem() instanceof ArrowItem);

        if (nextArrow.isEmpty()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof ArrowItem) {
                    nextArrow = stack;
                    break;
                }
            }
        }

        ItemStack iconStack = nextArrow.isEmpty() ? new ItemStack(Items.ARROW) : nextArrow.copy();

        // 2. LOGIQUE D'INFINITÉ (CREATIF VS SURVIE)
        ItemStack weapon = player.getMainHandItem().getItem() instanceof ProjectileWeaponItem
                ? player.getMainHandItem() : player.getOffhandItem();

        boolean canBeInfinite = false;

        // CAS A : Le mode Créatif rend TOUT infini
        if (player.getAbilities().instabuild) {
            canBeInfinite = true;
        }
        // CAS B : Arc avec enchantement Infinity (Survie)
        else if (weapon.getItem() instanceof net.minecraft.world.item.BowItem) {
            var enchantmentLookup = mc.level.holderLookup(Registries.ENCHANTMENT);
            Holder<Enchantment> infinityHolder = enchantmentLookup.getOrThrow(Enchantments.INFINITY);

            // Infinity ne marche QUE sur les flèches normales (Items.ARROW)
            if (EnchantmentHelper.getEnchantmentsForCrafting(weapon).getLevel(infinityHolder) > 0 && iconStack.is(Items.ARROW)) {
                canBeInfinite = true;
            }
        }

        // 3. CALCUL DU COMPTEUR TOTAL
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ArrowItem) {
                total += stack.getCount();
            }
        }

        if (total <= 0 && !canBeInfinite) return;

        // 4. RENDU
        gui.flush();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Dessin de l'icône de l'item
        gui.renderItem(iconStack, x, y);

        String label = canBeInfinite ? "∞" : String.valueOf(total);

        // Ajustement pour les très grands nombres (>999) pour éviter le débordement
        if (!canBeInfinite && total > 999) {
            gui.pose().pushPose();
            float scale = 0.75f; // Réduction à 75% de la taille
            float textWidth = mc.font.width(label) * scale;

            // On déplace le texte pour qu'il reste centré en bas de l'icône 16x16
            gui.pose().translate(x + 16 - textWidth, y + 10, 200);
            gui.pose().scale(scale, scale, 1.0f);
            gui.drawString(mc.font, label, 0, 0, 0xFFFFFF, true);
            gui.pose().popPose();
        } else {
            // Rendu standard superposé (0-999 et ∞)
            gui.renderItemDecorations(mc.font, iconStack, x, y, label);
        }

        gui.flush();
    }

    @Override public int getHeight() { return 16; }
    @Override public int getWidth() { return 16; }
}
