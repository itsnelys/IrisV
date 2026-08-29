package net.opal.irisv.indicators.providers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.opal.irisv.api.IIndicator;
import net.opal.irisv.option.ConfigOptions;

public class ArmorIndicator implements IIndicator {

    @Override
    public boolean isVisible() {
        var player = Minecraft.getInstance().player;
        return ConfigOptions.getInstance().enableIndicators &&
                player != null && !player.isSpectator();
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, float partialTick) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean showDetails = ConfigOptions.getInstance().showDurabilityDetails;

        int currentY = y;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = player.getInventory().armor.get(3 - i);

            if (!stack.isEmpty()) {
                float percent = stack.isDamageableItem() ?
                        (float) (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage() : 1.0f;

                if (percent <= 0.10f) {
                    // --- LE BATCH ISOLE (PRO) ---

                    // 1. On vide ce qui attend déjà dans le buffer principal pour être "frais"
                    gui.flush();

                    // 2. On change l'état du Shader juste pour notre mini-batch
                    float speed = percent <= 0.05F ? 1.2F : 0.6F;
                    float time = (player.tickCount + partialTick) * speed;
                    float alphaPulse = 0.5f + (float) Math.sin(time) * 0.35f;
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alphaPulse);

                    // 3. On dessine l'item et ses barres
                    // Elles vont s'accumuler dans le buffer avec l'alpha actuel
                    gui.renderItem(stack, x, currentY);
                    gui.renderItemDecorations(Minecraft.getInstance().font, stack, x, currentY);
                    if (showDetails) renderDurability(gui, stack, x, currentY);

                    // 4. ON FORCE LE BATCH MAINTENANT
                    // C'est ici que l'item part vers la carte graphique AVEC l'alpha
                    gui.flush();

                    // 5. Reset immédiat pour que l'itération suivante ou l'XP bar ne voit RIEN
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                } else {
                    // Item normal : on s'assure d'être en opaque (Sécurité)
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    gui.renderItem(stack, x, currentY);
                    gui.renderItemDecorations(Minecraft.getInstance().font, stack, x, currentY);
                    if (showDetails) renderDurability(gui, stack, x, currentY);
                    // On flush aussi par principe pour séparer les items entre eux
                    gui.flush();
                }
            }
            currentY += 18;
        }

        // Garde-fou final pour tout le HUD qui suit
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public int getHeight() { return 4 * 18; }
    @Override
    public int getWidth() { return 16; }

    private void renderDurability(GuiGraphics gui, ItemStack stack, int x, int y) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) return;

        var font = Minecraft.getInstance().font;
        int remaining = stack.getMaxDamage() - stack.getDamageValue();

        String text = String.valueOf(remaining);
        float scale = remaining > 999 ? 0.55F : 0.65F;
        float textWidth = font.width(text) * scale;
        gui.pose().pushPose();
        gui.pose().translate(x + 16 - textWidth, y + 9, 200);
        gui.pose().scale(scale, scale, 1.0F);
        gui.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        gui.pose().popPose();
    }
}
