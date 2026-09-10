package net.opal.irisv.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoRenderer.class)
public abstract class TitleLogoMixin {
    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V", at = @At("HEAD"), cancellable = true)
    private void irisv$renderTitle(GuiGraphics gui, int width, float alpha, int y, CallbackInfo callback) {
        if (!(Minecraft.getInstance().screen instanceof TitleScreen screen)) return;
        int firstButton = screen.children().stream().filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast).filter(widget -> widget.visible && widget.getWidth() >= 100)
                .mapToInt(AbstractWidget::getY).min().orElse(screen.height / 4 + 48);
        float scale = Math.max(0.01F, Math.min(Math.min(256F, width - 24F) / 1003F,
                Math.max(1, firstButton - y - 8) / 315F));
        gui.pose().pushPose();
        gui.pose().translate((width - 1003 * scale) / 2F, y, 0);
        gui.pose().scale(scale, scale, 1);
        gui.setColor(1, 1, 1, alpha);
        RenderSystem.enableBlend();
        gui.blit(ResourceLocation.fromNamespaceAndPath("irisv", "textures/gui/minecraft_title.png"),
                0, 0, 0F, 0F, 1003, 315, 1003, 315);
        gui.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        gui.pose().popPose();
        callback.cancel();
    }
}
