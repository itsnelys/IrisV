package net.opal.irisv.recip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.opal.irisv.Irisv;
import net.opal.irisv.menu.MenuOptionIrisv;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.theme.UiTheme;

import java.util.ArrayList;
import java.util.List;

public final class RecipeInventoryOverlay {
    private static final int ITEM_SIZE = 16;
    private static final int CELL_SIZE = 18;
    private static final int MARGIN_Y = 10;
    private static final int RIGHT_MARGIN = 20;
    private static final int SEARCH_SPACING = 8;
    private static final int SEARCH_HEIGHT = 22;
    private static final int PAGE_BUTTON_SIZE = 22;
    private static final int TOOL_BUTTON_WIDTH = 22;
    private static final int FAVORITE_COLUMNS = 3;
    private static final long DOUBLE_CLICK_MS = 300L;
    private static final long PAGE_FADE_MS = 140L;
    private static final ResourceLocation OPTIONS_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/inventory_options.png");
    private static final ResourceLocation WIKI_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/wiki_icon.png");
    private static final ResourceLocation EYE_OPEN_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/eye_open.png");
    private static final ResourceLocation EYE_CLOSED_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/eye_closed.png");
    private static final ResourceLocation PAGE_LEFT_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/page_left.png");
    private static final ResourceLocation PAGE_RIGHT_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/page_right.png");

    private static EditBox searchBox;
    private static String query = "";
    private static List<ItemStack> filteredItems = new ArrayList<>();
    private static final List<ItemStack> favorites = new ArrayList<>();
    private static int page;
    private static int favoriteScroll;
    private static long pageAnimationStartMs;
    private static RecipeCategory activeCategory = RecipeCategory.ALL;
    private static boolean categoryMenuOpen;
    private static boolean highlightSearchMode;
    private static boolean recipePanelHidden;
    private static long lastSearchClickMs;
    private static boolean stateLoaded;

    private RecipeInventoryOverlay() {}

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!isOverlayEnabled() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !isSupportedScreen(screen)) {
            searchBox = null;
            return;
        }

        query = "";
        RecipeItemIndex.rebuild();
        loadState();
        refreshFilteredItems();

        Layout layout = layout(screen);

        searchBox = new EditBox(Minecraft.getInstance().font, layout.searchX, layout.searchY, layout.searchWidth, SEARCH_HEIGHT, Component.translatable("recip.irisv.search"));
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setTextColorUneditable(0xFFAAAAAA);
        searchBox.setMaxLength(80);
        searchBox.setValue(query);
        searchBox.setResponder(value -> {
            query = value;
            page = 0;
            refreshFilteredItems();
            saveState();
        });
        event.addListener(searchBox);
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!isOverlayEnabled() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !isSupportedScreen(screen)) return;
        if (searchBox == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        Layout layout = layout(screen);

        int totalPages = totalPages(layout);
        if (page >= totalPages) page = Math.max(0, totalPages - 1);

        renderVisibilityButton(gui, layout, event.getMouseX(), event.getMouseY());
        if (recipePanelHidden) return;

        renderPagination(gui, layout, totalPages);
        renderSearchFrame(gui, layout);
        renderCategoryButton(gui, layout, event.getMouseX(), event.getMouseY());
        renderOptionsButton(gui, layout, event.getMouseX(), event.getMouseY());
        renderWikiButton(gui, layout, event.getMouseX(), event.getMouseY());
        renderFavorites(gui, layout, event.getMouseX(), event.getMouseY());
        renderResetSearchButton(gui, layout, event.getMouseX(), event.getMouseY());

        searchBox.setX(layout.searchX + 4);
        searchBox.setY(layout.searchY + 7);
        searchBox.setWidth(Math.max(20, layout.searchWidth - (query.isBlank() ? 8 : 18)));
        searchBox.render(gui, event.getMouseX(), event.getMouseY(), event.getPartialTick());

        ItemStack hovered = renderItems(gui, layout, event.getMouseX(), event.getMouseY());
        renderCategoryMenu(gui, layout, event.getMouseX(), event.getMouseY());
        if (hovered.isEmpty()) {
            hovered = favoriteAt(layout, event.getMouseX(), event.getMouseY());
        }
        if (!hovered.isEmpty() && !isInsideCategoryMenu(layout, event.getMouseX(), event.getMouseY())) {
            gui.renderTooltip(Minecraft.getInstance().font, tooltipFor(hovered), hovered.getTooltipImage(), hovered, event.getMouseX(), event.getMouseY());
        } else if (isHovering(event.getMouseX(), event.getMouseY(), layout.searchX, layout.searchY, layout.searchWidth, SEARCH_HEIGHT)) {
            renderSearchHelpTooltip(gui, event.getMouseX(), event.getMouseY());
        }
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isOverlayEnabled() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !isSupportedScreen(screen)) return;

        Layout layout = layout(screen);

        if (isHovering(event.getMouseX(), event.getMouseY(), layout.visibilityX, layout.visibilityY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            recipePanelHidden = !recipePanelHidden;
            if (recipePanelHidden && searchBox != null) {
                searchBox.setFocused(false);
            }
            playClick();
            event.setCanceled(true);
            return;
        }

        if (recipePanelHidden) return;

        if (!query.isBlank() && isHovering(event.getMouseX(), event.getMouseY(), layout.resetX, layout.resetY, 10, 10)) {
            query = "";
            page = 0;
            refreshFilteredItems();
            if (searchBox != null) searchBox.setValue("");
            saveState();
            playClick();
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == 0 && isHovering(event.getMouseX(), event.getMouseY(), layout.searchX, layout.searchY, layout.searchWidth, SEARCH_HEIGHT)) {
            if (searchBox != null) {
                searchBox.setFocused(true);
                screen.setFocused(searchBox);
            }
            long now = System.currentTimeMillis();
            if (now - lastSearchClickMs <= DOUBLE_CLICK_MS) {
                highlightSearchMode = !highlightSearchMode;
                lastSearchClickMs = 0L;
                page = 0;
                refreshFilteredItems();
                saveState();
                playClick();
            } else {
                lastSearchClickMs = now;
            }
            event.setCanceled(true);
            return;
        }

        if (isHovering(event.getMouseX(), event.getMouseY(), layout.prevX, layout.pageButtonY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            changePage(layout, -1);
            playClick();
            event.setCanceled(true);
            return;
        }

        if (isHovering(event.getMouseX(), event.getMouseY(), layout.nextX, layout.pageButtonY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            changePage(layout, 1);
            playClick();
            event.setCanceled(true);
            return;
        }

        RecipeCategory selectedCategory = categoryAt(layout, event.getMouseX(), event.getMouseY());
        if (selectedCategory != null) {
            activeCategory = selectedCategory;
            categoryMenuOpen = false;
            page = 0;
            refreshFilteredItems();
            saveState();
            playClick();
            event.setCanceled(true);
            return;
        }

        if (isHovering(event.getMouseX(), event.getMouseY(), layout.toolX, layout.searchY, TOOL_BUTTON_WIDTH, SEARCH_HEIGHT)) {
            categoryMenuOpen = !categoryMenuOpen;
            playClick();
            event.setCanceled(true);
            return;
        }

        if (isHovering(event.getMouseX(), event.getMouseY(), layout.optionsX, layout.optionsY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            Minecraft.getInstance().setScreen(new MenuOptionIrisv(screen));
            playClick();
            event.setCanceled(true);
            return;
        }

        if (isHovering(event.getMouseX(), event.getMouseY(), layout.wikiX, layout.wikiY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            Minecraft.getInstance().setScreen(new RecipeWikiScreen(screen));
            playClick();
            event.setCanceled(true);
            return;
        }

        ItemStack clicked = itemAt(layout, event.getMouseX(), event.getMouseY());
        if (clicked.isEmpty()) {
            clicked = favoriteAt(layout, event.getMouseX(), event.getMouseY());
        }
        if (!clicked.isEmpty()) {
            if (event.getButton() == 2) {
                toggleFavorite(clicked);
                saveState();
                playClick();
            } else if (event.getButton() == 0) {
                if (isCreativePlayer()) {
                    giveCreativeStack(clicked);
                    playClick();
                }
            }
            event.setCanceled(true);
        }
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!isOverlayEnabled() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !isSupportedScreen(screen)) return;
        if (recipePanelHidden) return;
        Layout layout = layout(screen);
        if (isInsideFavorites(layout, event.getMouseX(), event.getMouseY()) && favorites.size() > layout.favoriteRows * FAVORITE_COLUMNS) {
            favoriteScroll = clamp(favoriteScroll + (event.getScrollDeltaY() < 0 ? 1 : -1), 0, maxFavoriteScroll(layout));
            event.setCanceled(true);
            return;
        }
        if (!isInsidePanel(layout, event.getMouseX(), event.getMouseY())) return;

        if (event.getScrollDeltaY() < 0) changePage(layout, 1);
        if (event.getScrollDeltaY() > 0) changePage(layout, -1);
        event.setCanceled(true);
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isSearchFocusedFor(event.getScreen())) return;
        if (searchBox.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    public static void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!isSearchFocusedFor(event.getScreen())) return;
        if (searchBox.charTyped(event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    public static void onInventoryMobEffects(ScreenEvent.RenderInventoryMobEffects event) {
        if (isOverlayEnabled() && event.getScreen() instanceof InventoryScreen) {
            event.setCompact(true);
        }
    }

    private static void renderPagination(GuiGraphics gui, Layout layout, int totalPages) {
        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        String pageText = (page + 1) + "/" + totalPages;
        gui.fill(layout.prevX, layout.pageButtonY, layout.nextX + PAGE_BUTTON_SIZE, layout.pageButtonY + PAGE_BUTTON_SIZE, palette.pageBackground);
        renderButtonBox(gui, layout.prevX, layout.pageButtonY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, palette);
        renderButtonBox(gui, layout.nextX, layout.pageButtonY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, palette);
        gui.fill(layout.pageBoxLeft, layout.pageButtonY, layout.pageBoxRight, layout.pageButtonY + PAGE_BUTTON_SIZE, palette.pageBackground);
        int textY = layout.pageButtonY + (PAGE_BUTTON_SIZE - mc.font.lineHeight) / 2 + 1;
        gui.blit(PAGE_LEFT_ICON, layout.prevX + 3, layout.pageButtonY + 3, 0.0F, 0.0F, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        gui.blit(PAGE_RIGHT_ICON, layout.nextX + 3, layout.pageButtonY + 3, 0.0F, 0.0F, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        drawCenteredText(gui, mc, pageText, (layout.pageBoxLeft + layout.pageBoxRight) / 2, textY, palette.text);
    }

    private static void drawCenteredText(GuiGraphics gui, Minecraft mc, String text, int centerX, int y, int color) {
        gui.drawString(mc.font, text, centerX - mc.font.width(text) / 2, y, color, false);
    }

    private static void renderCategoryButton(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        renderButtonBox(gui, layout.toolX, layout.searchY, TOOL_BUTTON_WIDTH, SEARCH_HEIGHT, palette);
        if (categoryMenuOpen || activeCategory != RecipeCategory.ALL) {
            renderOutline(gui, layout.toolX, layout.searchY, TOOL_BUTTON_WIDTH, SEARCH_HEIGHT, palette.searchHighlight);
        }
        gui.renderFakeItem(activeCategory.icon(), layout.toolX + 3, layout.searchY + 3);
        if (isHovering(mouseX, mouseY, layout.toolX, layout.searchY, TOOL_BUTTON_WIDTH, SEARCH_HEIGHT)) {
            gui.renderTooltip(mc.font, Component.literal(activeCategory.displayName()), mouseX, mouseY);
        }
    }

    private static void renderResetSearchButton(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        if (query.isBlank()) return;
        OverlayPalette palette = OverlayPalette.current();
        int color = isHovering(mouseX, mouseY, layout.resetX, layout.resetY, 10, 10) ? palette.text : palette.buttonLight;
        gui.drawString(Minecraft.getInstance().font, "x", layout.resetX + 2, layout.resetY, color, false);
    }

    private static void renderOptionsButton(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        renderButtonBox(gui, layout.optionsX, layout.optionsY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, palette);
        gui.blit(OPTIONS_ICON, layout.optionsX + 3, layout.optionsY + 3, 0.0F, 0.0F, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        if (isHovering(mouseX, mouseY, layout.optionsX, layout.optionsY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            gui.renderTooltip(mc.font, Component.literal("IrisV"), mouseX, mouseY);
        }
    }

    private static void renderWikiButton(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        renderButtonBox(gui, layout.wikiX, layout.wikiY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, palette);
        gui.blit(WIKI_ICON, layout.wikiX + 3, layout.wikiY + 3, 0.0F, 0.0F, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        if (isHovering(mouseX, mouseY, layout.wikiX, layout.wikiY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            gui.renderTooltip(mc.font, Component.translatable("recip.irisv.wiki"), mouseX, mouseY);
        }
    }

    private static void renderVisibilityButton(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        renderButtonBox(gui, layout.visibilityX, layout.visibilityY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, palette);
        ResourceLocation icon = recipePanelHidden ? EYE_CLOSED_ICON : EYE_OPEN_ICON;
        gui.blit(icon, layout.visibilityX + 3, layout.visibilityY + 3, 0.0F, 0.0F, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        if (isHovering(mouseX, mouseY, layout.visibilityX, layout.visibilityY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            gui.renderTooltip(mc.font, Component.translatable(recipePanelHidden ? "recip.irisv.show" : "recip.irisv.hide"), mouseX, mouseY);
        }
    }

    private static void renderFavorites(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        if (favorites.isEmpty()) return;
        favoriteScroll = clamp(favoriteScroll, 0, maxFavoriteScroll(layout));
        int start = favoriteScroll * FAVORITE_COLUMNS;
        int visible = Math.min(favorites.size() - start, layout.favoriteRows * FAVORITE_COLUMNS);
        for (int index = 0; index < visible; index++) {
            int x = layout.favoritesX + (index % FAVORITE_COLUMNS) * CELL_SIZE;
            int y = layout.favoritesY + (index / FAVORITE_COLUMNS) * CELL_SIZE;
            ItemStack stack = favorites.get(start + index);
            gui.renderFakeItem(stack, x, y);
            renderFavoriteStar(gui, x, y);
            if (isHovering(mouseX, mouseY, x, y, CELL_SIZE, CELL_SIZE)) {
                renderOutline(gui, x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, 0xCCFFFFFF);
            }
        }
        renderFavoriteScrollbar(gui, layout);
    }

    private static void renderFavoriteScrollbar(GuiGraphics gui, Layout layout) {
        if (favorites.size() <= layout.favoriteRows * FAVORITE_COLUMNS) return;
        OverlayPalette palette = OverlayPalette.current();
        int x = layout.favoritesX - 6;
        int y = layout.favoritesY;
        int height = layout.favoriteRows * CELL_SIZE - 2;
        int totalRows = favoriteTotalRows();
        int thumbHeight = Math.max(8, height * layout.favoriteRows / totalRows);
        int thumbTravel = Math.max(1, height - thumbHeight);
        int thumbY = y + thumbTravel * favoriteScroll / maxFavoriteScroll(layout);
        gui.fill(x, y, x + 2, y + height, palette.scrollTrack);
        gui.fill(x, thumbY, x + 2, thumbY + thumbHeight, palette.scrollThumb);
    }

    private static void renderSearchHelpTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        List<FormattedCharSequence> lines = List.of(
                Component.literal("Recherche").getVisualOrderText(),
                Component.literal("@mod  namespace").getVisualOrderText(),
                Component.literal("#id  identifiant complet").getVisualOrderText(),
                Component.literal("$path  chemin de l'item").getVisualOrderText(),
                Component.literal("Double-clic: mode surbrillance").withStyle(ChatFormatting.YELLOW).getVisualOrderText()
        );
        gui.renderTooltip(mc.font, lines, mouseX, mouseY);
    }

    private static void renderCategoryMenu(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        if (!categoryMenuOpen) return;

        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        RecipeCategory[] categories = RecipeCategory.values();
        int width = 72;
        int rowHeight = 14;
        int x = layout.toolX + TOOL_BUTTON_WIDTH - width;
        int y = layout.searchY - categories.length * rowHeight - 4;

        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 400.0F);
        try {
            gui.fill(x, y, x + width, y + categories.length * rowHeight, palette.searchBackground);
            gui.fill(x, y, x + width, y + 1, palette.searchBorder);
            gui.fill(x, y + categories.length * rowHeight - 1, x + width, y + categories.length * rowHeight, palette.searchBorder);
            gui.fill(x, y, x + 1, y + categories.length * rowHeight, palette.searchBorder);
            gui.fill(x + width - 1, y, x + width, y + categories.length * rowHeight, palette.searchBorder);

            for (int i = 0; i < categories.length; i++) {
                int rowY = y + i * rowHeight;
                if (categories[i] == activeCategory || isHovering(mouseX, mouseY, x, rowY, width, rowHeight)) {
                    gui.fill(x + 1, rowY + 1, x + width - 1, rowY + rowHeight - 1, palette.pageBackground);
                }
                gui.drawString(mc.font, categories[i].displayName(), x + 5, rowY + 3, palette.text, false);
            }
        } finally {
            gui.pose().popPose();
        }
    }

    private static void renderSearchFrame(GuiGraphics gui, Layout layout) {
        OverlayPalette palette = OverlayPalette.current();
        int border = highlightSearchMode ? palette.searchHighlight : palette.searchBorder;
        gui.fill(layout.searchX, layout.searchY, layout.searchX + layout.searchWidth, layout.searchY + SEARCH_HEIGHT, border);
        gui.fill(layout.searchX + 1, layout.searchY + 1, layout.searchX + layout.searchWidth - 1, layout.searchY + SEARCH_HEIGHT - 1, palette.searchBackground);
    }

    private static void renderButtonBox(GuiGraphics gui, int x, int y, int width, int height, OverlayPalette palette) {
        gui.fill(x, y, x + width, y + height, palette.buttonShadow);
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1, palette.buttonBase);
        gui.fill(x, y, x + width, y + 1, palette.buttonLight);
        gui.fill(x, y, x + 1, y + height, palette.buttonLight);
        gui.fill(x, y + height - 1, x + width, y + height, palette.buttonDark);
        gui.fill(x + width - 1, y, x + width, y + height, palette.buttonDark);
        gui.fill(x + 3, y + 3, x + width - 3, y + height - 3, palette.buttonInner);
    }

    private static void renderToolIcon(GuiGraphics gui, int x, int y, OverlayPalette palette) {
        gui.fill(x + 2, y, x + 5, y + 3, palette.iconDark);
        gui.fill(x + 5, y + 3, x + 8, y + 6, palette.iconDark);
        gui.fill(x + 8, y + 6, x + 11, y + 9, palette.iconDark);
        gui.fill(x, y + 11, x + 4, y + 15, palette.iconDark);
        gui.fill(x + 3, y + 8, x + 7, y + 12, palette.iconDark);
        gui.fill(x + 9, y + 1, x + 12, y + 4, palette.iconLight);
        gui.fill(x + 12, y + 4, x + 15, y + 7, palette.iconLight);
    }

    private static ItemStack renderItems(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        ItemStack hovered = ItemStack.EMPTY;
        int start = page * layout.itemsPerPage;
        int end = Math.min(filteredItems.size(), start + layout.itemsPerPage);
        float fadeAlpha = pageFadeAlpha();

        for (int index = start; index < end; index++) {
            int local = index - start;
            int x = layout.gridX + (local % layout.columns) * CELL_SIZE;
            int y = layout.gridY + (local / layout.columns) * CELL_SIZE;
            ItemStack stack = filteredItems.get(index);
            boolean matches = matchesCurrentSearch(stack);

            if (!matches) {
                renderDimmedItem(gui, stack, x, y, fadeAlpha);
            } else {
                renderItem(gui, stack, x, y, fadeAlpha);
            }
            if (favoriteIndex(stack) >= 0) {
                renderFavoriteStar(gui, x, y);
            }
            if (isHovering(mouseX, mouseY, x, y, CELL_SIZE, CELL_SIZE)) {
                hovered = stack;
                renderOutline(gui, x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, 0xCCFFFFFF);
            }
        }

        return hovered;
    }

    private static void renderItem(GuiGraphics gui, ItemStack stack, int x, int y, float alpha) {
        if (alpha >= 0.99F) {
            gui.renderFakeItem(stack, x, y);
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        gui.renderFakeItem(stack, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderDimmedItem(GuiGraphics gui, ItemStack stack, int x, int y, float fadeAlpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.18F, 0.18F, 0.18F, 0.22F * fadeAlpha);
        gui.renderFakeItem(stack, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderOutline(GuiGraphics gui, int x, int y, int width, int height, int color) {
        gui.fill(x, y, x + width, y + 1, color);
        gui.fill(x, y + height - 1, x + width, y + height, color);
        gui.fill(x, y, x + 1, y + height, color);
        gui.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static void renderFavoriteStar(GuiGraphics gui, int x, int y) {
        OverlayPalette palette = OverlayPalette.current();
        int cx = x + 12;
        int cy = y + 3;
        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 300.0F);
        try {
            gui.fill(cx, cy - 2, cx + 1, cy + 3, palette.searchHighlight);
            gui.fill(cx - 2, cy, cx + 3, cy + 1, palette.searchHighlight);
            gui.fill(cx - 1, cy - 1, cx + 2, cy + 2, palette.searchHighlight);
            gui.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
        } finally {
            gui.pose().popPose();
        }
    }

    private static ItemStack itemAt(Layout layout, double mouseX, double mouseY) {
        int start = page * layout.itemsPerPage;
        int end = Math.min(filteredItems.size(), start + layout.itemsPerPage);

        for (int index = start; index < end; index++) {
            int local = index - start;
            int x = layout.gridX + (local % layout.columns) * CELL_SIZE;
            int y = layout.gridY + (local / layout.columns) * CELL_SIZE;
            if (isHovering(mouseX, mouseY, x, y, CELL_SIZE, CELL_SIZE)) {
                return filteredItems.get(index);
            }
        }

        return ItemStack.EMPTY;
    }

    private static List<Component> tooltipFor(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        List<Component> tooltip = new ArrayList<>(stack.getTooltipLines(
                Item.TooltipContext.of(mc.level),
                mc.player,
                mc.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED : net.minecraft.world.item.TooltipFlag.Default.NORMAL
        ));
        tooltip.add(Component.literal(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        return tooltip;
    }

    private static void refreshFilteredItems() {
        filteredItems = RecipeItemIndex.getItems().stream()
                .filter(stack -> activeCategory.matches(stack))
                .filter(stack -> highlightSearchMode || query.isBlank() || RecipeSearch.matchesAnyToken(stack, query))
                .toList();
        page = Math.min(page, Math.max(0, totalPages(layoutFallback()) - 1));
    }

    private static boolean matchesCurrentSearch(ItemStack stack) {
        return query.isBlank() || RecipeSearch.matches(stack, query);
    }

    private static void loadState() {
        if (stateLoaded) return;
        ConfigOptions config = ConfigOptions.getInstance();
        highlightSearchMode = config.recipeHighlightSearchMode;
        page = Math.max(0, config.recipePage);
        try {
            activeCategory = RecipeCategory.valueOf(config.recipeCategory);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            activeCategory = RecipeCategory.ALL;
        }
        favorites.clear();
        for (String key : config.recipeFavorites) {
            findIndexedStack(key).ifPresent(stack -> favorites.add(stack.copy()));
        }
        stateLoaded = true;
    }

    private static void saveState() {
        ConfigOptions config = ConfigOptions.getInstance();
        config.recipeCategory = activeCategory.name();
        config.recipeHighlightSearchMode = highlightSearchMode;
        config.recipePage = page;
        config.recipeFavorites = favorites.stream().map(RecipeInventoryOverlay::stackKey).toList();
        config.save();
    }

    private static java.util.Optional<ItemStack> findIndexedStack(String key) {
        for (ItemStack stack : RecipeItemIndex.getItems()) {
            if (stackKey(stack).equals(key)) return java.util.Optional.of(stack);
        }
        return java.util.Optional.empty();
    }

    private static void toggleFavorite(ItemStack stack) {
        int existing = favoriteIndex(stack);
        if (existing >= 0) {
            favorites.remove(existing);
        } else {
            favorites.add(0, stack.copy());
        }
        favoriteScroll = 0;
    }

    private static int favoriteIndex(ItemStack stack) {
        String key = stackKey(stack);
        for (int i = 0; i < favorites.size(); i++) {
            if (stackKey(favorites.get(i)).equals(key)) return i;
        }
        return -1;
    }

    private static String stackKey(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getComponents();
    }

    private static ItemStack favoriteAt(Layout layout, double mouseX, double mouseY) {
        int start = favoriteScroll * FAVORITE_COLUMNS;
        int visible = Math.min(favorites.size() - start, layout.favoriteRows * FAVORITE_COLUMNS);
        for (int i = 0; i < visible; i++) {
            int x = layout.favoritesX + (i % FAVORITE_COLUMNS) * CELL_SIZE;
            int y = layout.favoritesY + (i / FAVORITE_COLUMNS) * CELL_SIZE;
            if (isHovering(mouseX, mouseY, x, y, CELL_SIZE, CELL_SIZE)) {
                return favorites.get(start + i);
            }
        }
        return ItemStack.EMPTY;
    }

    private static RecipeCategory categoryAt(Layout layout, double mouseX, double mouseY) {
        if (!categoryMenuOpen) return null;
        RecipeCategory[] categories = RecipeCategory.values();
        int width = 72;
        int rowHeight = 14;
        int x = layout.toolX + TOOL_BUTTON_WIDTH - width;
        int y = layout.searchY - categories.length * rowHeight - 4;
        if (!isHovering(mouseX, mouseY, x, y, width, categories.length * rowHeight)) return null;
        int index = (int) ((mouseY - y) / rowHeight);
        return categories[Math.max(0, Math.min(categories.length - 1, index))];
    }

    private static boolean isInsideCategoryMenu(Layout layout, double mouseX, double mouseY) {
        if (!categoryMenuOpen) return false;
        int width = 72;
        int rowHeight = 14;
        int x = layout.toolX + TOOL_BUTTON_WIDTH - width;
        int y = layout.searchY - RecipeCategory.values().length * rowHeight - 4;
        return isHovering(mouseX, mouseY, x, y, width, RecipeCategory.values().length * rowHeight);
    }

    private static boolean isInsideFavorites(Layout layout, double mouseX, double mouseY) {
        if (favorites.isEmpty()) return false;
        int x = favorites.size() > layout.favoriteRows * FAVORITE_COLUMNS ? layout.favoritesX - 8 : layout.favoritesX;
        int width = FAVORITE_COLUMNS * CELL_SIZE + (favorites.size() > layout.favoriteRows * FAVORITE_COLUMNS ? 8 : 0);
        return isHovering(mouseX, mouseY, x, layout.favoritesY, width, layout.favoriteRows * CELL_SIZE);
    }

    private static int maxFavoriteScroll(Layout layout) {
        return Math.max(0, favoriteTotalRows() - layout.favoriteRows);
    }

    private static int favoriteTotalRows() {
        return Math.max(1, (favorites.size() + FAVORITE_COLUMNS - 1) / FAVORITE_COLUMNS);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void changePage(Layout layout, int direction) {
        int totalPages = totalPages(layout);
        int nextPage = Math.floorMod(page + direction, totalPages);
        if (nextPage != page) {
            page = nextPage;
            pageAnimationStartMs = System.currentTimeMillis();
            saveState();
        }
    }

    private static float pageFadeAlpha() {
        if (pageAnimationStartMs == 0L) return 1.0F;
        long elapsed = System.currentTimeMillis() - pageAnimationStartMs;
        if (elapsed >= PAGE_FADE_MS) return 1.0F;
        return Math.max(0.25F, elapsed / (float) PAGE_FADE_MS);
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static boolean isOverlayEnabled() {
        return ConfigOptions.getInstance().enableRecipeOverlay;
    }

    private static boolean isSupportedScreen(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }

    private static boolean isCreativePlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isCreative();
    }

    private static boolean isSearchFocusedFor(Screen screen) {
        return isOverlayEnabled()
                && screen instanceof AbstractContainerScreen<?>
                && isSupportedScreen(screen)
                && searchBox != null
                && searchBox.isFocused();
    }

    private static void giveCreativeStack(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        ItemStack remaining = stack.copy();
        remaining.setCount(Screen.hasShiftDown() ? remaining.getMaxStackSize() : 1);

        while (!remaining.isEmpty()) {
            int inventorySlot = findCreativeTargetSlot(remaining);
            if (inventorySlot < 0) {
                mc.gameMode.handleCreativeModeItemAdd(remaining.copy(), -1);
                return;
            }

            ItemStack existing = mc.player.getInventory().getItem(inventorySlot);
            ItemStack updated;
            if (existing.isEmpty()) {
                updated = remaining.copy();
                remaining.setCount(0);
            } else {
                updated = existing.copy();
                int moved = Math.min(remaining.getCount(), updated.getMaxStackSize() - updated.getCount());
                updated.grow(moved);
                remaining.shrink(moved);
            }

            mc.player.getInventory().setItem(inventorySlot, updated);
            mc.gameMode.handleCreativeModeItemAdd(updated.copy(), menuSlotFromInventorySlot(inventorySlot));
        }
    }

    private static int findCreativeTargetSlot(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack existing = mc.player.getInventory().getItem(i);
            if (canMergeInto(existing, stack)) {
                return i;
            }
        }
        return mc.player.getInventory().getFreeSlot();
    }

    private static boolean canMergeInto(ItemStack existing, ItemStack stack) {
        return !existing.isEmpty()
                && existing.getCount() < existing.getMaxStackSize()
                && ItemStack.isSameItemSameComponents(existing, stack);
    }

    private static int menuSlotFromInventorySlot(int inventorySlot) {
        return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
    }

    private static Layout layout(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();
        int inventoryRight = screen.getGuiLeft() + screen.getXSize();
        int rightEdge = screen.width - 14;
        int leftBound = inventoryRight + 42;
        if (rightEdge - leftBound < 96) {
            leftBound = inventoryRight + 8;
        }
        int availableWidth = Math.max(72, rightEdge - leftBound);
        int columnsByScale = Math.max(1, (int) Math.floor(9 * (3.0D / guiScale)));
        int columnsByWidth = Math.max(4, availableWidth / CELL_SIZE);
        int columns = Math.max(4, Math.min(columnsByScale, columnsByWidth));
        int verticalReserve = SEARCH_HEIGHT + SEARCH_SPACING + PAGE_BUTTON_SIZE + (MARGIN_Y * 3);
        int rowsByScale = Math.max(1, (int) Math.floor(15 * (3.0D / guiScale)));
        int rowsByHeight = Math.max(4, (screen.height - verticalReserve - 12) / CELL_SIZE);
        int rows = Math.max(4, Math.min(rowsByScale, rowsByHeight));

        int gridWidth = columns * CELL_SIZE;
        int gridHeight = rows * CELL_SIZE;

        String pageText = (page + 1) + "/" + Math.max(1, (filteredItems.size() + (columns * rows) - 1) / (columns * rows));
        int pageTextWidth = mc.font.width(pageText);
        int pageBoxWidth = Math.max(42, pageTextWidth + 18);
        int paginationTotalWidth = gridWidth;
        while (paginationTotalWidth > availableWidth && columns > 4) {
            columns--;
            gridWidth = columns * CELL_SIZE;
            paginationTotalWidth = gridWidth;
        }
        int paginationHeight = PAGE_BUTTON_SIZE;
        int contentWidth = Math.max(gridWidth, paginationTotalWidth);
        int panelWidth = Math.max(contentWidth, 80);
        int panelHeight = paginationHeight + gridHeight + SEARCH_SPACING + SEARCH_HEIGHT + (MARGIN_Y * 3);
        int panelX = Math.max(leftBound, rightEdge - panelWidth);
        int panelY = (screen.height - panelHeight) / 2;

        int gridX = panelX + (panelWidth - gridWidth) / 2;
        int pageButtonY = panelY + MARGIN_Y;
        int gridY = pageButtonY + paginationHeight + 6;
        int searchY = panelY + panelHeight - SEARCH_HEIGHT;
        int categoryWidth = 0;
        int categoryX = gridX;
        int searchX = categoryX + categoryWidth;
        int searchWidth = Math.max(40, gridWidth - categoryWidth - TOOL_BUTTON_WIDTH);
        int toolX = searchX + searchWidth;
        int optionsX = 6;
        int optionsY = searchY;
        int visibilityX = optionsX;
        int visibilityY = pageButtonY;
        int wikiX = optionsX + PAGE_BUTTON_SIZE + 4;
        int wikiY = optionsY;
        int resetX = searchX + searchWidth - 11;
        int resetY = searchY + 6;
        int favoritesX = optionsX;
        int favoritesY = Math.max(8, gridY);
        int favoriteRows = Math.max(1, (optionsY - favoritesY - 6) / CELL_SIZE);

        int prevX = gridX;
        int nextX = gridX + gridWidth - PAGE_BUTTON_SIZE;
        int pageBoxLeft = prevX + PAGE_BUTTON_SIZE;
        int pageBoxRight = nextX;

        return new Layout(panelX, panelY, panelWidth, panelHeight, columns, rows, columns * rows, gridX, gridY, searchX, searchY, searchWidth, resetX, resetY, categoryX, categoryWidth, toolX, visibilityX, visibilityY, optionsX, optionsY, wikiX, wikiY, favoritesX, favoritesY, favoriteRows, prevX, nextX, pageButtonY, pageBoxLeft, pageBoxRight);
    }

    private static Layout layoutFallback() {
        return new Layout(0, 0, 200, 200, 9, 10, 90, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static int totalPages(Layout layout) {
        return Math.max(1, (filteredItems.size() + layout.itemsPerPage - 1) / layout.itemsPerPage);
    }

    private static boolean isInsidePanel(Layout layout, double mouseX, double mouseY) {
        return isHovering(mouseX, mouseY, layout.panelX, layout.panelY, layout.panelWidth, layout.panelHeight);
    }

    private static boolean isHovering(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int columns,
            int rows,
            int itemsPerPage,
            int gridX,
            int gridY,
            int searchX,
            int searchY,
            int searchWidth,
            int resetX,
            int resetY,
            int categoryX,
            int categoryWidth,
            int toolX,
            int visibilityX,
            int visibilityY,
            int optionsX,
            int optionsY,
            int wikiX,
            int wikiY,
            int favoritesX,
            int favoritesY,
            int favoriteRows,
            int prevX,
            int nextX,
            int pageButtonY,
            int pageBoxLeft,
            int pageBoxRight
    ) {}

    private record OverlayPalette(
            int text,
            int pageBackground,
            int searchBorder,
            int searchHighlight,
            int searchBackground,
            int scrollTrack,
            int scrollThumb,
            int buttonShadow,
            int buttonBase,
            int buttonLight,
            int buttonDark,
            int buttonInner,
            int iconDark,
            int iconLight
    ) {
        static OverlayPalette current() {
            if (ConfigOptions.getInstance().theme == ConfigOptions.Theme.DARKNESS) {
                return vanilla();
            }

            UiTheme theme = UiTheme.getCurrent();
            int accent = opaque(theme.tooltip_borderColor());
            int background = alpha(opaque(theme.tooltip_backgroundColor()), 0xDD);
            int dark = darken(accent, 0.45F);
            int base = mix(accent, 0xFF8A8A8A, 0.18F);
            int inner = mix(accent, 0xFF707070, 0.24F);
            int light = lighten(base, 0.45F);

            return new OverlayPalette(
                    opaque(theme.tooltip_titleColor()),
                    alpha(darken(background, 0.2F), 0x66),
                    light,
                    0xFFFFD45A,
                    0xEE000000,
                    0x66000000,
                    alpha(light, 0xCC),
                    0xFF000000,
                    base,
                    light,
                    dark,
                    inner,
                    darken(accent, 0.75F),
                    lighten(accent, 0.55F)
            );
        }

        private static OverlayPalette vanilla() {
            return new OverlayPalette(
                    0xFFE0D6A7,
                    0xAA202020,
                    0xFFE0E0E0,
                    0xFFFFD45A,
                    0xEE000000,
                    0x77000000,
                    0xFFE0D6A7,
                    0xFF000000,
                    0xFF8B8B8B,
                    0xFFE0E0E0,
                    0xFF373737,
                    0xFF6E6E6E,
                    0xFF202020,
                    0xFFFFFFFF
            );
        }

        private static int opaque(int color) {
            return 0xFF000000 | (color & 0x00FFFFFF);
        }

        private static int alpha(int color, int alpha) {
            return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
        }

        private static int lighten(int color, float amount) {
            return mix(color, 0xFFFFFFFF, amount);
        }

        private static int darken(int color, float amount) {
            return mix(color, 0xFF000000, amount);
        }

        private static int mix(int first, int second, float amount) {
            int r = (int) (((first >> 16) & 0xFF) * (1.0F - amount) + ((second >> 16) & 0xFF) * amount);
            int g = (int) (((first >> 8) & 0xFF) * (1.0F - amount) + ((second >> 8) & 0xFF) * amount);
            int b = (int) ((first & 0xFF) * (1.0F - amount) + (second & 0xFF) * amount);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }
}
