package net.opal.irisv.indicators.providers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.opal.irisv.api.IIndicator;
import net.opal.irisv.option.ConfigOptions;

public class ToolDurabilityIndicator implements IIndicator {

    @Override
    public boolean isVisible() {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) return false;

        ConfigOptions config = ConfigOptions.getInstance();
        if (!config.enableIndicators || !config.enableToolDurabilityIndicator) return false;

        return player.getMainHandItem().isDamageableItem() || player.getOffhandItem().isDamageableItem();
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, float partialTick) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        gui.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int currentY = y;
        currentY = renderDurability(gui, mc, player.getMainHandItem(), x, currentY, partialTick);
        renderDurability(gui, mc, player.getOffhandItem(), x, currentY, partialTick);

        gui.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public int getHeight() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;

        int height = 0;
        if (player.getMainHandItem().isDamageableItem()) height += 18;
        if (player.getOffhandItem().isDamageableItem()) height += 18;
        return height;
    }

    @Override
    public int getWidth() {
        return 16;
    }

    private static float getDurabilityRatio(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) return 1.0F;
        return (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage();
    }

    private static int renderDurability(GuiGraphics gui, Minecraft mc, ItemStack stack, int x, int y, float partialTick) {
        if (!stack.isDamageableItem()) return y;

        float ratio = getDurabilityRatio(stack);
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        boolean shouldPulse = ratio <= 0.10F;

        if (shouldPulse && mc.player != null) {
            float speed = ratio <= 0.05F ? 1.2F : 0.6F;
            float time = (mc.player.tickCount + partialTick) * speed;
            float alphaPulse = 0.5F + (float) Math.sin(time) * 0.35F;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alphaPulse);
        }

        gui.renderItem(stack, x, y);
        gui.renderItemDecorations(mc.font, stack, x, y);
        gui.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (!ConfigOptions.getInstance().showDurabilityDetails) return y + 18;

        String text = String.valueOf(remaining);
        float scale = remaining > 999 ? 0.55F : 0.65F;
        float textWidth = mc.font.width(text) * scale;
        gui.pose().pushPose();
        gui.pose().translate(x + 16 - textWidth, y + 9, 200);
        gui.pose().scale(scale, scale, 1.0F);
        gui.drawString(mc.font, text, 0, 0, 0xFFFFFFFF, true);
        gui.pose().popPose();

        return y + 18;
    }
}
