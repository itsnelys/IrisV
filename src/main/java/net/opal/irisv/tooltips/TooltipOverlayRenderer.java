package net.opal.irisv.tooltips;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.theme.UiTheme;
import net.opal.irisv.tooltips.overlay.*;
import net.opal.irisv.tooltips.providers.TooltipFluidProviderRegistry;

import java.util.List;
import java.util.Map;

public class TooltipOverlayRenderer {

    private static final Map<BlockPos, Float> ANIMATED_RATIOS = new java.util.HashMap<>();

    public static void render(GuiGraphics gui, Font font, TooltipData.BlockInfo info, IBlockAccessor accessor, float visualProgress, long timeSinceFinish, int screenWidth, int screenHeight) {
        UiTheme theme = UiTheme.getCurrent();
        ConfigOptions config = ConfigOptions.getInstance();
        boolean hasCtrl = Screen.hasControlDown();

        // 1. CALCUL DU LAYOUT (Contient maintenant fluidHeight)
        BlockTooltipLayout layout = BlockTooltipLayout.calculate(font, info, accessor, hasCtrl);

        // --- LOGIQUE DE POSITIONNEMENT DYNAMIQUE ---
        int margin = config.compactMode ? 0 : 10;
        int x;
        int y;

        switch (config.tooltipPosition) {
            case TOP_LEFT -> { x = margin; y = margin; }
            case TOP_RIGHT -> { x = screenWidth - layout.width() - margin; y = margin; }
            case BOTTOM_LEFT -> { x = margin; y = screenHeight - layout.height() - margin; }
            case BOTTOM_RIGHT -> { x = screenWidth - layout.width() - margin; y = screenHeight - layout.height() - margin; }
            case TOP_CENTER -> { x = (screenWidth - layout.width()) / 2; y = margin; }
            default -> { x = (screenWidth - layout.width()) / 2; y = margin; }
        }

        // 2. RENDU DU FOND
        gui.fill(x, y, x + layout.width(), y + layout.height(), theme.tooltip_backgroundColor());
        renderBorder(gui, x, y, layout.width(), layout.height());

        // 3. HEADER
        String finalTitle = (accessor != null && accessor.getTitleOverride() != null) ? accessor.getTitleOverride() : info.name();
        ItemStack finalIcon = (accessor != null && !accessor.getIcon().isEmpty()) ? accessor.getIcon() : info.icon();

        gui.renderFakeItem(finalIcon, x + 5, y + 5);
        gui.drawString(font, finalTitle, x + 26, y + 5, theme.tooltip_titleColor(), true);

        // 4. INFOS D'ÉTAT (Santé, etc.)
        int currentY = renderStateInfo(gui, font, info, x, y);

        // 5. PREVIEW D'INVENTAIRE (Fours, Coffres, Alambics)
        TooltipOverlayRendererInventory.renderInventoryPreview(
                gui,
                font,
                accessor.getPreviewItems(),
                hasCtrl,
                x,
                currentY,
                accessor.state(),
                accessor.blockEntity(),
                info
        );

// --- 6. RENDU DU FLUIDE DYNAMIQUE ---
        if (config.advancedTooltips) {
            var fluidData = TooltipFluidProviderRegistry.get(accessor.state(), accessor.blockEntity(), accessor.level(), accessor.pos());

            if (fluidData != null && !fluidData.isEmpty()) {
                FluidStack stack = fluidData.getFluidStack();
                BlockPos pos = accessor.pos();

                String nameTxt = stack.getHoverName().getString();
                String amountTxt = net.opal.irisv.tooltips.providers.fluid.GenericFluidProvider.formatCompact(fluidData.getAmount())
                        + " / " + net.opal.irisv.tooltips.providers.fluid.GenericFluidProvider.formatCompact(fluidData.getCapacity());

                int drawX = x + 26;
                int barWidth = layout.width() - 32;
                int barHeight = 12;
                int fluidY = currentY + 4;

                // LOGIQUE D'ANIMATION BASÉE SUR LE RATIO
                float targetRatio = (float) fluidData.getAmount() / (float) fluidData.getCapacity();
                float currentAnimRatio = ANIMATED_RATIOS.getOrDefault(pos, targetRatio);

                if (Math.abs(currentAnimRatio - targetRatio) > 0.001f) {
                    currentAnimRatio += (targetRatio - currentAnimRatio) * 0.05f;
                    ANIMATED_RATIOS.put(pos, currentAnimRatio);
                } else {
                    currentAnimRatio = targetRatio;
                    ANIMATED_RATIOS.put(pos, targetRatio);
                }

                int fillWidth = (int) (currentAnimRatio * barWidth);

                // DESSIN BARRE
                gui.fill(drawX, fluidY, drawX + barWidth, fluidY + barHeight, 0xAA000000);
                if (fillWidth > 0) {
                    var extensions = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(stack.getFluid());
                    int fluidColor = extensions.getTintColor(stack);
                    ResourceLocation atlas = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
                    var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(atlas).apply(extensions.getStillTexture(stack));

                    for (int i = 0; i < fillWidth; i += 16) {
                        int w = Math.min(16, fillWidth - i);
                        gui.blit(net.minecraft.client.renderer.RenderType::guiTextured, atlas, drawX + i, fluidY,
                                sprite.getU0() * 1024, sprite.getV0() * 1024, w, barHeight, 1024, 1024, fluidColor);
                    }
                }

                renderBorder(gui, drawX, fluidY, barWidth, barHeight);
                gui.drawString(font, nameTxt, drawX + 4, fluidY + 2, 0xFFFFFFFF, true);
                gui.drawString(font, amountTxt, drawX + barWidth - font.width(amountTxt) - 4, fluidY + 2, 0xFFFFFFFF, true);

                // STATISTIQUES AVANCÉES
                java.util.List<net.minecraft.network.chat.Component> statsLines = new java.util.ArrayList<>();
                net.opal.irisv.tooltips.providers.fluid.GenericFluidProvider.addFluidStats(stack, statsLines);

                int statsY = fluidY + barHeight + 4;
                for (net.minecraft.network.chat.Component line : statsLines) {
                    gui.drawString(font, line, drawX, statsY, 0xFFFFFFFF, true);
                    statsY += 10;
                }

                currentY += layout.fluidHeight();
            }
        }

        // 7. MOD NAME & TOOLS
        TooltipOverlayRendererUtils.renderModName(gui, font, info, x, y, layout.height());

        if (accessor != null && accessor.state() != null) {
            TooltipOverlayRendererTools.renderRequiredTools(gui, font, info, accessor.state(), x, y, layout.nameWidth());

            // La barre de progression
            int barY = y + layout.height() - 1;
            TooltipOverlayRendererUtils.renderProgressBar(gui, accessor.state(), x, barY, layout.width(), visualProgress, timeSinceFinish);
        }
    }

    private record BlockTooltipLayout(int width, int height, int nameWidth, int previewHeight, int fluidHeight) {
        public static BlockTooltipLayout calculate(Font font, TooltipData.BlockInfo info, IBlockAccessor accessor, boolean hasCtrl) {
            List<ItemStack> previewItems = accessor.getPreviewItems();
            int itemCount = previewItems.size();
            CompoundTag data = (accessor.blockEntity() != null) ? net.opal.irisv.network.ClientDataCache.get(accessor.blockEntity().getBlockPos()) : new CompoundTag();

            net.opal.irisv.option.ConfigOptions config = net.opal.irisv.option.ConfigOptions.getInstance();

            // --- 1. CALCULS DU HEADER ---
            String finalTitle = (accessor.getTitleOverride() != null) ? accessor.getTitleOverride() : info.name();
            int nameWidth = font.width(finalTitle);
            int modWidth = font.width(info.modName());
            int toolsSpace = (accessor.state() != null && !info.requiredTools().isEmpty())
                    ? (info.requiredTools().size() * 14) + 4
                    : 0;

            int baseContentWidth = Math.max(nameWidth + toolsSpace, modWidth);

            // --- 2. CALCUL DYNAMIQUE DU STATE INFO (Santé, etc.) ---
            int stateHeight = 0;

            if (ConfigOptions.getInstance().enableEntityTooltip) {
                if (info.stateInfo() != null) {
                    for (String s : info.stateInfo()) {
                        if (s.startsWith("hp_render:")) {
                            try {
                                String[] values = s.replace("hp_render:", "").split("/");
                                float maxHealth = Float.parseFloat(values[1]);
                                if (maxHealth > 40.0f) {
                                    stateHeight += 10;
                                    baseContentWidth = Math.max(baseContentWidth, 80);
                                } else {
                                    stateHeight += (maxHealth > 20.0f) ? 20 : 12;
                                    baseContentWidth = Math.max(baseContentWidth, 90);
                                }
                            } catch (Exception e) {
                                stateHeight += 10;
                            }
                        } else {
                            stateHeight += 10;
                            baseContentWidth = Math.max(baseContentWidth, font.width(s));
                        }
                    }
                }
            }
            // --- 3. CALCUL DYNAMIQUE DE LA PREVIEW (Items / Machines) ---
            int previewHeight = 0;
            int previewMaxWidth = 0;

            boolean isFurnace = accessor.state() != null && accessor.state().getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock;
            boolean isBrewing = accessor.state() != null && accessor.state().getBlock() instanceof net.minecraft.world.level.block.BrewingStandBlock;

// On récupère les données via l'accessor ou le cache pour la condition
            int cookTime = data.getInt("CookTime"); // Utilise le nom synchronisé
            int burnTime = data.getInt("BurnTime");
            int brewTime = data.getInt("BrewTime");

// --- LOGIQUE FOUR ---
            if (isFurnace && (!previewItems.isEmpty() || cookTime > 0 || burnTime > 0)) {
                if (ConfigOptions.getInstance().advancedTooltips) {
                    previewMaxWidth = 100; // Un peu plus large pour l'esthétique
                    // On passe de 22 à 28 pour laisser la place à la barre de fuel en dessous
                    previewHeight = 28;
                }
            }
// --- LOGIQUE ALAMBIC ---
            else if (isBrewing && (!previewItems.isEmpty() || brewTime > 0)) {
                if (ConfigOptions.getInstance().advancedTooltips) {
                    previewMaxWidth = 120;
                    // L'alambic a aussi une barre de fuel (depuis la 1.9), on adapte aussi
                    previewHeight = 28;
                }
            }

            else if (!previewItems.isEmpty()) {
                if (itemCount <= 4 && !hasCtrl) {
                    previewHeight = (itemCount * 10) + 2;
                    int maxQteWidth = 0;
                    for (ItemStack stack : previewItems) {
                        String qteText = TooltipOverlayRendererUtils.formatCount(stack.getCount()) + "x";
                        maxQteWidth = Math.max(maxQteWidth, (int) (font.width(qteText) * 0.7f));
                    }
                    for (ItemStack stack : previewItems) {
                        int itemNameWidth = (int) (font.width(stack.getHoverName().getString()) * 0.7f);
                        previewMaxWidth = Math.max(previewMaxWidth, 16 + maxQteWidth + itemNameWidth);
                    }
                } else if (hasCtrl || itemCount <= 9) {
                    int maxIcons = hasCtrl ? Math.min(itemCount, 54) : itemCount;
                    int cols = Math.min(maxIcons, 9);
                    int rows = (maxIcons + 8) / 9;
                    previewMaxWidth = cols * 18;
                    previewHeight = rows * 18 + 4;
                    if (hasCtrl && itemCount > 54) previewHeight += 12;
                } else {
                    previewMaxWidth = 9 * 18;
                    previewHeight = 18 + 12 + 4;
                }
            }

// --- 4. FLUIDE ---
            int fluidHeight = 0;
            int fluidMaxWidth = 0;
            var fluidData = TooltipFluidProviderRegistry.get(accessor.state(), accessor.blockEntity(), accessor.level(), accessor.pos());

            if (config.advancedTooltips && fluidData != null && !fluidData.isEmpty()) {
                // Hauteur de base pour la barre de fluide
                fluidHeight = 20;

                // On vérifie si l'option globale des stats est activée
                if (config.advancedLiquidStats) {
                    if (hasCtrl) {
                        // Place pour les 3 lignes de stats (Temp, État, Viscosité)
                        fluidHeight += 34;
                    } else {
                        // Place pour l'indice "[Hold CTRL for details]"
                        fluidHeight += 12;
                    }
                }

                // Calcul de la largeur
                String fName = fluidData.getFluidStack().getHoverName().getString();
                fluidMaxWidth = Math.max(120, font.width(fName) + 60);

                // On n'élargit le cadre pour les stats que si l'option est ON et CTRL pressé
                if (config.advancedLiquidStats && hasCtrl) {
                    java.util.List<net.minecraft.network.chat.Component> statsLines = new java.util.ArrayList<>();
                    net.opal.irisv.tooltips.providers.fluid.GenericFluidProvider.addFluidStats(fluidData.getFluidStack(), statsLines);
                    for (var line : statsLines) {
                        fluidMaxWidth = Math.max(fluidMaxWidth, font.width(line) + 10);
                    }
                }
            }

            // --- 5. DIMENSIONS FINALES ---
            // On compare la largeur du Header, de la Preview d'items et du Fluide
            int width = Math.max(26 + baseContentWidth + 6, 26 + previewMaxWidth + 6);
            width = Math.max(width, 26 + fluidMaxWidth + 6);

            int height = 28 + stateHeight + previewHeight + fluidHeight;

            return new BlockTooltipLayout(width, height, nameWidth, previewHeight, fluidHeight);
        }
    }

    private static int renderStateInfo(GuiGraphics gui, Font font, TooltipData.BlockInfo info, int x, int y) {
        UiTheme theme = UiTheme.getCurrent();
        int currentY = y + 16;

        if (info.stateInfo() == null || info.stateInfo().isEmpty()) return currentY;

        for (String line : info.stateInfo()) {
            if (line.startsWith("hp_render:")) {
                try {
                    String[] values = line.replace("hp_render:", "").split("/");
                    float health = Float.parseFloat(values[0]);
                    float maxHealth = Float.parseFloat(values[1]);

                    // --- NOUVEAU SEUIL : 40 HP (20 coeurs) ---
                    if (maxHealth > 40.0f) {
                        // Calcul de la couleur
                        float percent = health / maxHealth;
                        String color = percent > 0.6 ? "§a" : (percent > 0.3 ? "§e" : "§c");

                        // Formatage : "❤ 96.1 / 100"
                        String hpText = String.format("§c❤ " + color + "%.1f §f/ §7%.0f", health, maxHealth);

                        gui.drawString(font, hpText, x + 26, currentY, 0xFFFFFF, true);
                        currentY += 10;
                    }
                    else if (maxHealth > 0.1f) {
                        // Rendu visuel des coeurs (1 ou 2 lignes)
                        TooltipOverlayRendererUtils.renderHearts(gui, x + 26, currentY, health, maxHealth);

                        // Si > 20 HP, on a utilisé 2 lignes (20px), sinon 1 ligne (12px)
                        currentY += (maxHealth > 20.0f) ? 20 : 12;
                    }
                    continue;
                } catch (Exception e) { /* ignore */ }
            }

            // Rendu texte classique (Mod name, etc.)
            if (!line.trim().isEmpty()) {
                gui.drawString(font, line, x + 26, currentY, theme.block_stateTextColor(), true);
                currentY += 10;
            }
        }
        return currentY;
    }

    private static void renderBorder(GuiGraphics gui, int x, int y, int width, int height) {
        UiTheme theme = UiTheme.getCurrent();
        int color = theme.tooltip_borderColor();
        gui.fill(x - 1, y - 1, x + width + 1, y, color);
        gui.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        gui.fill(x - 1, y, x, y + height, color);
        gui.fill(x + width, y, x + width + 1, y + height, color);
    }
}
