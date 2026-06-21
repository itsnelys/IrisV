package net.opal.irisv.tooltips.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockPreviewRenderer;
import net.opal.irisv.tooltips.TooltipData;
import net.opal.irisv.tooltips.overlay.renderers.TooltipOverlayRendererBlockContents;
import net.opal.irisv.tooltips.providers.TooltipBlockSpecialProviderRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TooltipOverlayRendererInventory {

    /**
     * MÉTHODE PRINCIPALE : Elle décide si on fait un rendu CUSTOM ou GÉNÉRIQUE
     */
    public static void renderInventoryPreview(GuiGraphics gui, Font font, @NotNull List<ItemStack> items, boolean hasCtrl, int x, int y, BlockState state, @Nullable BlockEntity be, TooltipData.BlockInfo info) {
        if (items.isEmpty()) return;

        for (IBlockPreviewRenderer renderer : TooltipBlockSpecialProviderRegistry.getRenderers()) {
            if (renderer.isApplicable(state, be)) {
                renderer.render(gui, font, items, x, y, state, be, info);
                return; // On a trouvé un spécialiste, on s'arrête.
            }
        }

        // 2. Si aucun rendu spécial n'est applicable, on utilise le rendu par défaut
        TooltipOverlayRendererBlockContents.RenderBlockContents(gui, font, items, hasCtrl, x, y);
    }
}
