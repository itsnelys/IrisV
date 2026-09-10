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
    private static final int FAVORITE_COLUMNS = 4;
    private static final long DOUBLE_CLICK_MS = 300L;
    private static final long PAGE_FADE_MS = 140L;
    private static final ResourceLocation OPTIONS_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/inventory_options.png");
    private static final ResourceLocation WIKI_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/wiki_icon.png");
    private static final ResourceLocation EYE_OPEN_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/eye_open.png");
    private static final ResourceLocation EYE_CLOSED_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/eye_closed.png");

    private static EditBox searchBox;
    private static EditBox favoriteSearchBox;
    private static String favoriteQuery = "";
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
    private enum FavoriteFilter { RECIPE, BLOCK, ITEM, GLOBAL }
    private static FavoriteFilter favoriteFilter = FavoriteFilter.GLOBAL;
    private static FavoriteView pressedFavorite;
    private static Screen dragScreen;
    private static double pressX, pressY;
    private static boolean draggingFavorite;
    private record FavoriteView(ItemStack stack, List<RecipeBookmarks.Entry> recipes) {
        boolean isRecipe() { return !recipes.isEmpty(); }
    }
    private static RecipeDisplayScreen favoritePreview;
    private static List<RecipeBookmarks.Entry> previewSource = List.of();

    private RecipeInventoryOverlay() {}

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        pressedFavorite = null;
        draggingFavorite = false;
        dragScreen = null;
        Screen screen = event.getScreen();
        if (!isOverlayEnabled() || !isSupportedScreen(screen)) {
            searchBox = null;
            favoriteSearchBox = null;
            return;
        }

        if (!(screen instanceof RecipeDisplayScreen)) {
            query = "";
            favoriteQuery = "";
        }
        RecipeItemIndex.rebuild();
        RecipeBookmarks.invalidate();
        favoritePreview = null;
        previewSource = List.of();
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
        favoriteSearchBox = new EditBox(Minecraft.getInstance().font, layout.favoritesX + 4, layout.favoritesY - 45,
                favoriteWidth(layout) - 20, 14, Component.translatable("recip.irisv.favorites.search"));
        favoriteSearchBox.setBordered(false);
        favoriteSearchBox.setMaxLength(80);
        favoriteSearchBox.setHint(Component.translatable("recip.irisv.search"));
        favoriteSearchBox.setTextColor(0xFFE5E5E5);
        favoriteSearchBox.setValue(favoriteQuery);
        favoriteSearchBox.visible = hasFavorites() && !recipePanelHidden;
        favoriteSearchBox.setResponder(value -> { favoriteQuery = value; favoriteScroll = 0; });
        event.addListener(favoriteSearchBox);
    }

    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (Minecraft.getInstance().screen instanceof RecipeDisplayScreen browser && event.getScreen() == browser.originScreen()) {
            event.setCanceled(true);
        }
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen != Minecraft.getInstance().screen) return;
        if (!isOverlayEnabled() || !isSupportedScreen(screen)) return;
        if (searchBox == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        Layout layout = layout(screen);
        if (favoriteSearchBox != null) {
            favoriteSearchBox.visible = hasFavorites() && !recipePanelHidden;
            if (!favoriteSearchBox.visible) favoriteSearchBox.setFocused(false);
        }

        int totalPages = totalPages(layout);
        if (page >= totalPages) page = Math.max(0, totalPages - 1);

        renderVisibilityButton(gui, layout, event.getMouseX(), event.getMouseY());
        if (recipePanelHidden) return;

        renderInventorySearch(gui, screen);
        renderPagination(gui, layout, totalPages, event.getMouseX(), event.getMouseY());
        renderSearchFrame(gui, layout);
        renderCategoryButton(gui, layout, event.getMouseX(), event.getMouseY());
        renderOptionsButton(gui, layout, event.getMouseX(), event.getMouseY());
        renderWikiButton(gui, layout, event.getMouseX(), event.getMouseY());
        renderUnpinButton(gui, layout, event.getMouseX(), event.getMouseY());
        renderFavorites(gui, layout, event.getMouseX(), event.getMouseY());
        renderResetSearchButton(gui, layout, event.getMouseX(), event.getMouseY());

        searchBox.setX(layout.searchX + 4);
        searchBox.setY(layout.searchY + 7);
        searchBox.setWidth(Math.max(20, layout.searchWidth - (query.isBlank() ? 8 : 18)));
        searchBox.render(gui, event.getMouseX(), event.getMouseY(), event.getPartialTick());

        ItemStack hovered = renderItems(gui, layout, event.getMouseX(), event.getMouseY());
        if (pressedFavorite != null && dragScreen == screen) {
            updateFavoriteDrag(event.getMouseX(), event.getMouseY());
            if (draggingFavorite) {
                renderFavoriteFilter(gui, layout, event.getMouseX(), event.getMouseY());
                gui.pose().pushPose();
                gui.pose().translate(0, 0, 700);
                int insertion = favoriteInsertion(layout, event.getMouseX(), event.getMouseY());
                if (insertion >= 0) {
                    int columns = favoriteColumns(layout);
                    int local = insertion - favoriteScroll * columns;
                    int markerX = layout.favoritesX + (local % columns) * favoriteWidth(layout) / columns;
                    int markerY = layout.favoritesY + (local / columns) * CELL_SIZE;
                    gui.fill(markerX, markerY, markerX + 2, markerY + ITEM_SIZE, 0xFFFFFFFF);
                }
                gui.renderFakeItem(pressedFavorite.stack, event.getMouseX() - 8, event.getMouseY() - 8);
                gui.pose().popPose();
                return;
            }
        }
        renderCategoryMenu(gui, layout, event.getMouseX(), event.getMouseY());
        renderFavoriteFilter(gui, layout, event.getMouseX(), event.getMouseY());
        if (hovered.isEmpty()) {
            hovered = favoriteAt(layout, event.getMouseX(), event.getMouseY());
        }
        FavoriteView hoveredFavorite = favoriteViewAt(layout, event.getMouseX(), event.getMouseY());
        if (hoveredFavorite != null && hoveredFavorite.isRecipe()) {
            previewFor(hoveredFavorite).renderPreview(gui, event.getMouseX(), event.getMouseY(), screen.width, screen.height);
        } else if (!hovered.isEmpty() && !isInsideCategoryMenu(layout, event.getMouseX(), event.getMouseY())) {
            gui.renderTooltip(Minecraft.getInstance().font, tooltipFor(hovered), hovered.getTooltipImage(), hovered, event.getMouseX(), event.getMouseY());
        } else if (isHovering(event.getMouseX(), event.getMouseY(), layout.searchX, layout.searchY, layout.searchWidth, SEARCH_HEIGHT)) {
            renderSearchHelpTooltip(gui, event.getMouseX(), event.getMouseY());
        }
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!isOverlayEnabled() || !isSupportedScreen(screen)) return;

        Layout layout = layout(screen);

        if (isHovering(event.getMouseX(), event.getMouseY(), layout.visibilityX, layout.visibilityY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            recipePanelHidden = !recipePanelHidden;
            net.opal.irisv.commun.utils.FunctionUtilsChat.clientAction(Minecraft.getInstance().player,
                    recipePanelHidden ? "irisv.chat.catalog_hidden" : "irisv.chat.catalog_visible");
            if (recipePanelHidden && searchBox != null) {
                searchBox.setFocused(false);
            }
            playClick();
            event.setCanceled(true);
            return;
        }

        if (recipePanelHidden) return;

        int favoriteHeaderX = layout.favoritesX;
        if (hasFavorites() && event.getButton() == 0 && isHovering(event.getMouseX(), event.getMouseY(), favoriteHeaderX, layout.favoritesY - 50, favoriteWidth(layout), 18)) {
            if (event.getMouseX() >= favoriteHeaderX + favoriteWidth(layout) - 14) favoriteSearchBox.setValue("");
            searchBox.setFocused(false);
            favoriteSearchBox.setFocused(true);
            screen.setFocused(favoriteSearchBox);
            favoriteSearchBox.mouseClicked(event.getMouseX(), event.getMouseY(), 0);
            event.setCanceled(true);
            return;
        }
        if (favoriteSearchBox != null) favoriteSearchBox.setFocused(false);
        if (hasFavorites() && event.getButton() == 0 && isHovering(event.getMouseX(), event.getMouseY(), favoriteHeaderX, layout.favoritesY - 28, favoriteWidth(layout), 20)) {
            int index = Math.min(3, (int) (event.getMouseX() - favoriteHeaderX) / layout.favoriteCellWidth);
            favoriteFilter = FavoriteFilter.values()[index];
            net.opal.irisv.commun.utils.FunctionUtilsChat.clientAction(Minecraft.getInstance().player,
                    "irisv.chat.favorite_filter", Component.translatable("irisv.chat.filter." + favoriteFilter.name().toLowerCase(java.util.Locale.ROOT)));
            favoriteScroll = 0;
            ConfigOptions.getInstance().favoriteFilter = favoriteFilter.name();
            ConfigOptions.getInstance().save();
            playClick();
            event.setCanceled(true);
            return;
        }

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
            net.opal.irisv.commun.utils.FunctionUtilsChat.clientAction(Minecraft.getInstance().player,
                    "irisv.chat.catalog_filter", selectedCategory.name());
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

        if (isHovering(event.getMouseX(), event.getMouseY(), layout.wikiX + PAGE_BUTTON_SIZE + 4, layout.wikiY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            if (event.getButton() == 0 && PinnedRecipeHud.hasPinned()) {
                PinnedRecipeHud.unpin();
                playClick();
            }
            event.setCanceled(true);
            return;
        }

        ItemStack clicked = itemAt(layout, event.getMouseX(), event.getMouseY());
        if (clicked.isEmpty()) {
            FavoriteView favorite = favoriteViewAt(layout, event.getMouseX(), event.getMouseY());
            if (favorite != null && event.getButton() == 0 && favoriteFilter == FavoriteFilter.GLOBAL
                    && favoriteQuery.isBlank() && !Screen.hasShiftDown() && !Screen.hasControlDown()) {
                pressedFavorite = favorite;
                dragScreen = screen;
                pressX = event.getMouseX();
                pressY = event.getMouseY();
                draggingFavorite = false;
                event.setCanceled(true);
                return;
            }
            if (favorite != null && favorite.isRecipe()) {
                if (handleCreativeClick(favorite.stack, event.getButton())) {
                    event.setCanceled(true);
                    return;
                }
                if (event.getButton() == 2) RecipeBookmarks.toggle(previewFor(favorite).selectedPreview());
                else if (event.getButton() == 0) RecipeDisplayScreen.openSaved(screen, favorite.recipes);
                else if (event.getButton() == 1) RecipeDisplayScreen.open(screen, favorite.stack, true);
                playClick();
                event.setCanceled(true);
                return;
            }
            clicked = favoriteAt(layout, event.getMouseX(), event.getMouseY());
        }
        if (!clicked.isEmpty()) {
            if (event.getButton() == 2) {
                toggleFavorite(clicked);
                saveState();
                playClick();
            } else if (event.getButton() == 0 || event.getButton() == 1) {
                if (!handleCreativeClick(clicked, event.getButton())) {
                    RecipeDisplayScreen.open(screen, clicked, event.getButton() == 1);
                    playClick();
                }
            }
            event.setCanceled(true);
        }
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        Screen screen = event.getScreen();
        if (!isOverlayEnabled() || !isSupportedScreen(screen)) return;
        if (recipePanelHidden) return;
        Layout layout = layout(screen);
        FavoriteView favorite = favoriteViewAt(layout, event.getMouseX(), event.getMouseY());
        ItemStack hovered = itemAt(layout, event.getMouseX(), event.getMouseY());
        if (hovered.isEmpty() && favorite != null) hovered = favorite.stack;
        if (pressedFavorite == null && handleCreativeScroll(hovered, event.getScrollDeltaY())) {
            event.setCanceled(true);
            return;
        }
        if (favorite != null && favorite.isRecipe() && !Screen.hasShiftDown() && pressedFavorite == null) {
            if (event.getScrollDeltaY() != 0) previewFor(favorite).scrollPreview(event.getScrollDeltaY() < 0 ? 1 : -1);
            event.setCanceled(true);
            return;
        }
        if (isInsideFavorites(layout, event.getMouseX(), event.getMouseY()) && favoriteViews().size() > layout.favoriteRows * favoriteColumns(layout)) {
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
        if (event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            pressedFavorite = null;
            draggingFavorite = false;
            dragScreen = null;
        }
        if (!isSearchFocusedFor(event.getScreen())) {
            if (!isOverlayEnabled() || recipePanelHidden || !isSupportedScreen(event.getScreen())) return;
            var mc = Minecraft.getInstance();
            double mx = mc.mouseHandler.xpos() * event.getScreen().width / mc.getWindow().getScreenWidth();
            double my = mc.mouseHandler.ypos() * event.getScreen().height / mc.getWindow().getScreenHeight();
            Layout layout = layout(event.getScreen());
            FavoriteView favorite = favoriteViewAt(layout, mx, my);
            var key = net.opal.irisv.client.ClientKeyBindings.PIN;
            if (favorite != null && favorite.isRecipe() && key.matches(event.getKeyCode(), event.getScanCode())) {
                PinnedRecipeHud.toggle(previewFor(favorite).selectedPreview());
                event.setCanceled(true);
                return;
            }
            ItemStack stack = itemAt(layout, mx, my);
            if (stack.isEmpty() && favorite != null) stack = favorite.stack;
            if (!stack.isEmpty() && handleRecipeKey(event.getScreen(), stack, event.getKeyCode(), event.getScanCode())) event.setCanceled(true);
            return;
        }
        if (event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) return;
        EditBox focused = favoriteSearchBox != null && favoriteSearchBox.isFocused() ? favoriteSearchBox : searchBox;
        if (focused.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    static boolean handleRecipeKey(Screen screen, ItemStack stack, int key, int scan) {
        if (net.opal.irisv.client.ClientKeyBindings.RECIPES.matches(key, scan)) {
            RecipeDisplayScreen.open(screen, stack, false); return true;
        }
        if (net.opal.irisv.client.ClientKeyBindings.USES.matches(key, scan)) {
            RecipeDisplayScreen.open(screen, stack, true); return true;
        }
        return false;
    }

    public static void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!isSearchFocusedFor(event.getScreen())) return;
        EditBox focused = favoriteSearchBox != null && favoriteSearchBox.isFocused() ? favoriteSearchBox : searchBox;
        if (focused.charTyped(event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    public static void onInventoryMobEffects(ScreenEvent.RenderInventoryMobEffects event) {
        if (isOverlayEnabled() && event.getScreen() instanceof InventoryScreen) {
            event.setCompact(true);
        }
    }

    private static void renderPagination(GuiGraphics gui, Layout layout, int totalPages, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        String pageText = (page + 1) + "/" + totalPages;
        gui.fill(layout.prevX, layout.pageButtonY, layout.nextX + PAGE_BUTTON_SIZE, layout.pageButtonY + PAGE_BUTTON_SIZE, palette.pageBackground);
        RecipeNavigationButton.draw(gui, layout.prevX, layout.pageButtonY, PAGE_BUTTON_SIZE, false,
                isHovering(mouseX, mouseY, layout.prevX, layout.pageButtonY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE), totalPages > 1);
        RecipeNavigationButton.draw(gui, layout.nextX, layout.pageButtonY, PAGE_BUTTON_SIZE, true,
                isHovering(mouseX, mouseY, layout.nextX, layout.pageButtonY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE), totalPages > 1);
        gui.fill(layout.pageBoxLeft, layout.pageButtonY, layout.pageBoxRight, layout.pageButtonY + PAGE_BUTTON_SIZE, palette.pageBackground);
        int textY = layout.pageButtonY + (PAGE_BUTTON_SIZE - mc.font.lineHeight) / 2 + 1;
        drawCenteredText(gui, mc, pageText, (layout.pageBoxLeft + layout.pageBoxRight) / 2, textY, palette.text);
    }

    private static void drawCenteredText(GuiGraphics gui, Minecraft mc, String text, int centerX, int y, int color) {
        gui.drawString(mc.font, text, centerX - mc.font.width(text) / 2, y, color, false);
    }

    private static void renderInventorySearch(GuiGraphics gui, Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (!ConfigOptions.getInstance().inventorySearchHighlight || !highlightSearchMode || query.isBlank()
                || mc.player == null || !(screen instanceof AbstractContainerScreen<?> container)) return;
        int highlightRgb = UiTheme.getCurrent().block_countNormalHex() & 0x00FFFFFF;
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 100);
        for (var slot : container.getMenu().slots) {
            if (slot.container != mc.player.getInventory() || !slot.isActive() || !slot.hasItem()
                    || slot.x < 0 || slot.y < 0 || !RecipeSearch.matchesAnyToken(slot.getItem(), query)) continue;
            int x = container.getGuiLeft() + slot.x;
            int y = container.getGuiTop() + slot.y;
            if (x < 0 || y < 0 || x + ITEM_SIZE > screen.width || y + ITEM_SIZE > screen.height) continue;
            gui.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, 0x30000000 | highlightRgb);
            gui.renderOutline(x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, 0xFF000000 | highlightRgb);
        }
        gui.pose().popPose();
    }

    private static void renderCategoryButton(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        renderButtonBox(gui, layout.toolX, layout.searchY, TOOL_BUTTON_WIDTH, SEARCH_HEIGHT, isHovering(mouseX, mouseY, layout.toolX, layout.searchY, TOOL_BUTTON_WIDTH, SEARCH_HEIGHT));
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
        renderButtonBox(gui, layout.optionsX, layout.optionsY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, isHovering(mouseX, mouseY, layout.optionsX, layout.optionsY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE));
        gui.blit(OPTIONS_ICON, layout.optionsX + 3, layout.optionsY + 3, 0.0F, 0.0F, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        if (isHovering(mouseX, mouseY, layout.optionsX, layout.optionsY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            gui.renderTooltip(mc.font, Component.literal("IrisV"), mouseX, mouseY);
        }
    }

    private static void renderUnpinButton(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        int x = layout.wikiX + PAGE_BUTTON_SIZE + 4;
        int y = layout.wikiY;
        boolean enabled = PinnedRecipeHud.hasPinned();
        boolean hovered = isHovering(mouseX, mouseY, x, y, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE);
        RecipeNavigationButton.background(gui, x, y, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, hovered, enabled);
        gui.renderFakeItem(new ItemStack(net.minecraft.world.item.Items.ITEM_FRAME), x + 3, y + 3);
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 300);
        gui.drawString(Minecraft.getInstance().font, "x", x + 13, y + 11, enabled ? 0xFFFF6666 : 0xFF888888, true);
        if (!enabled) gui.fill(x + 2, y + 2, x + PAGE_BUTTON_SIZE - 2, y + PAGE_BUTTON_SIZE - 2, 0x66000000);
        gui.pose().popPose();
        if (hovered) gui.renderTooltip(Minecraft.getInstance().font,
                Component.translatable(enabled ? "recip.irisv.unpin" : "recip.irisv.unpin.empty"), mouseX, mouseY);
    }

    private static void renderWikiButton(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        renderButtonBox(gui, layout.wikiX, layout.wikiY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, isHovering(mouseX, mouseY, layout.wikiX, layout.wikiY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE));
        gui.blit(WIKI_ICON, layout.wikiX + 3, layout.wikiY + 3, 0.0F, 0.0F, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        if (isHovering(mouseX, mouseY, layout.wikiX, layout.wikiY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            gui.renderTooltip(mc.font, Component.translatable("recip.irisv.wiki"), mouseX, mouseY);
        }
    }

    private static void renderVisibilityButton(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        OverlayPalette palette = OverlayPalette.current();
        renderButtonBox(gui, layout.visibilityX, layout.visibilityY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, isHovering(mouseX, mouseY, layout.visibilityX, layout.visibilityY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE));
        ResourceLocation icon = recipePanelHidden ? EYE_CLOSED_ICON : EYE_OPEN_ICON;
        gui.blit(icon, layout.visibilityX + 3, layout.visibilityY + 3, 0.0F, 0.0F, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        if (isHovering(mouseX, mouseY, layout.visibilityX, layout.visibilityY, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)) {
            gui.renderTooltip(mc.font, Component.translatable(recipePanelHidden ? "recip.irisv.show" : "recip.irisv.hide"), mouseX, mouseY);
        }
    }

    private static void renderFavorites(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        List<FavoriteView> visibleFavorites = favoriteViews();
        if (visibleFavorites.isEmpty()) return;
        favoriteScroll = clamp(favoriteScroll, 0, maxFavoriteScroll(layout));
        int columns = favoriteColumns(layout);
        int start = favoriteScroll * columns;
        int visible = Math.min(visibleFavorites.size() - start, layout.favoriteRows * columns);
        for (int index = 0; index < visible; index++) {
            int x = favoriteItemX(layout, index % columns);
            int y = layout.favoritesY + (index / columns) * CELL_SIZE;
            FavoriteView view = visibleFavorites.get(start + index);
            ItemStack stack = view.stack;
            gui.renderFakeItem(stack, x, y);
            renderFavoriteStar(gui, x, y);
            if (view.isRecipe()) {
                gui.pose().pushPose();
                // Item rendering adds its own depth; keep the badge below vanilla tooltips (Z=400).
                gui.pose().translate(x + 9, y + 9, 100);
                gui.pose().scale(0.5F, 0.5F, 1);
                gui.renderFakeItem(RecipeDisplayScreen.workstation(view.recipes.getFirst()), 0, 0);
                gui.pose().popPose();
            }
            if (isHovering(mouseX, mouseY, x, y, CELL_SIZE, CELL_SIZE)) {
                renderOutline(gui, x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, 0xCCFFFFFF);
            }
        }
        renderFavoriteScrollbar(gui, layout);
    }

    private static boolean hasFavorites() {
        return !favorites.isEmpty() || !RecipeBookmarks.entries().isEmpty();
    }

    private static List<FavoriteView> favoriteViews() {
        List<FavoriteView> views = new ArrayList<>();
        if (favoriteFilter == FavoriteFilter.RECIPE || favoriteFilter == FavoriteFilter.GLOBAL) {
            var groups = new java.util.LinkedHashMap<String, List<RecipeBookmarks.Entry>>();
            for (var entry : RecipeBookmarks.entries()) groups.computeIfAbsent(stackKey(entry.output()), key -> new ArrayList<>()).add(entry);
            for (var group : groups.values()) views.add(new FavoriteView(group.getFirst().output(), List.copyOf(group)));
        }
        if (favoriteFilter != FavoriteFilter.RECIPE) for (ItemStack stack : favorites) {
            boolean block = stack.getItem() instanceof net.minecraft.world.item.BlockItem;
            if (favoriteFilter == FavoriteFilter.BLOCK && !block || favoriteFilter == FavoriteFilter.ITEM && block) continue;
            views.add(new FavoriteView(stack, List.of()));
        }
        List<String> order = ConfigOptions.getInstance().favoriteOrder;
        views.sort(java.util.Comparator.comparingInt(view -> {
            int rank = order.indexOf(favoriteKey(view));
            return rank < 0 ? Integer.MAX_VALUE : rank;
        }));
        if (!favoriteQuery.isBlank()) views.removeIf(view -> !RecipeSearch.matchesAnyToken(view.stack, favoriteQuery));
        return views;
    }

    private static String favoriteKey(FavoriteView view) {
        return (view.isRecipe() ? "recipe:" : "item:") + stackKey(view.stack);
    }

    private static void updateFavoriteDrag(double x, double y) {
        if (Math.hypot(x - pressX, y - pressY) >= 4) draggingFavorite = true;
    }

    private static int favoriteInsertion(Layout layout, double x, double y) {
        if (!isHovering(x, y, layout.favoritesX, layout.favoritesY, favoriteWidth(layout), layout.favoriteRows * CELL_SIZE)) return -1;
        int columns = favoriteColumns(layout);
        int row = (int) (y - layout.favoritesY) / CELL_SIZE;
        int boundary = (int) Math.round((x - layout.favoritesX) * columns / favoriteWidth(layout));
        return Math.min(favoriteViews().size(), (favoriteScroll + row) * columns + boundary);
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (pressedFavorite == null || event.getButton() != 0) return;
        FavoriteView pressed = pressedFavorite;
        updateFavoriteDrag(event.getMouseX(), event.getMouseY());
        boolean moved = draggingFavorite;
        pressedFavorite = null;
        draggingFavorite = false;
        event.setCanceled(true);
        if (event.getScreen() != dragScreen || recipePanelHidden || !isOverlayEnabled()) return;
        dragScreen = null;
        if (!moved) {
            if (pressed.isRecipe()) RecipeDisplayScreen.openSaved(event.getScreen(), pressed.recipes);
            else RecipeDisplayScreen.open(event.getScreen(), pressed.stack, false);
            playClick();
            return;
        }
        if (favoriteFilter != FavoriteFilter.GLOBAL || !favoriteQuery.isBlank()) return;
        int insertion = favoriteInsertion(layout(event.getScreen()), event.getMouseX(), event.getMouseY());
        if (insertion < 0) return;
        List<FavoriteView> views = favoriteViews();
        int from = -1;
        for (int i = 0; i < views.size(); i++) if (favoriteKey(views.get(i)).equals(favoriteKey(pressed))) { from = i; break; }
        if (from < 0) return;
        FavoriteView movedView = views.remove(from);
        views.add(insertion > from ? insertion - 1 : insertion, movedView);
        ConfigOptions config = ConfigOptions.getInstance();
        List<String> order = new ArrayList<>(views.stream().map(RecipeInventoryOverlay::favoriteKey).toList());
        // Preserve ordering keys for unavailable mod entries without deleting their bookmarks.
        for (String key : config.favoriteOrder) if (!order.contains(key)) order.add(key);
        config.favoriteOrder = order;
        config.save();
        net.opal.irisv.commun.utils.FunctionUtilsChat.clientAction(Minecraft.getInstance().player, "irisv.chat.favorites_reordered");
        playClick();
    }

    private static RecipeDisplayScreen previewFor(FavoriteView favorite) {
        if (favoritePreview == null || !previewSource.equals(favorite.recipes)) {
            favoritePreview = RecipeDisplayScreen.preview(favorite.recipes);
            previewSource = favorite.recipes;
        }
        return favoritePreview;
    }

    private static void renderFavoriteFilter(GuiGraphics gui, Layout layout, int mouseX, int mouseY) {
        if (!hasFavorites()) return;
        var font = Minecraft.getInstance().font;
        int x = layout.favoritesX;
        int y = layout.favoritesY - 50;
        int width = favoriteWidth(layout);
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 100);
        gui.fill(x, y, x + width, y + 18, 0xE5000000);
        gui.renderOutline(x, y, width, 18, favoriteSearchBox != null && favoriteSearchBox.isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
        if (favoriteSearchBox != null) {
            favoriteSearchBox.setX(x + 4);
            favoriteSearchBox.setY(y + 5);
            favoriteSearchBox.setWidth(width - 20);
            favoriteSearchBox.visible = true;
            favoriteSearchBox.render(gui, mouseX, mouseY, 0);
        }
        if (!favoriteQuery.isEmpty()) gui.drawString(font, "x", x + width - 11, y + 5, 0xFFCCCCCC, false);
        var icons = new net.minecraft.world.item.Item[] {
                net.minecraft.world.item.Items.CRAFTING_TABLE, net.minecraft.world.item.Items.GRASS_BLOCK,
                net.minecraft.world.item.Items.IRON_INGOT, net.minecraft.world.item.Items.COMPASS };
        for (int i = 0; i < 4; i++) {
            int buttonX = x + i * layout.favoriteCellWidth;
            int buttonY = y + 22;
            boolean active = FavoriteFilter.values()[i] == favoriteFilter;
            int buttonWidth = layout.favoriteCellWidth - 1;
            boolean hovered = isHovering(mouseX, mouseY, buttonX, buttonY, buttonWidth, 20);
            RecipeNavigationButton.background(gui, buttonX, buttonY, buttonWidth, 20, hovered, true);
            if (active) gui.renderOutline(buttonX, buttonY, buttonWidth, 20, 0xFFFFFFFF);
            gui.renderFakeItem(new ItemStack(icons[i]), buttonX + (buttonWidth - 16) / 2, buttonY + 2);
        }
        gui.fill(x, y + 46, x + width, y + 47, 0xAA000000);
        gui.fill(x, y + 47, x + width, y + 48, 0xFF888888);
        for (int i = 0; i < 4; i++) if (isHovering(mouseX, mouseY, x + i * layout.favoriteCellWidth, y + 22, layout.favoriteCellWidth - 1, 20))
            gui.renderTooltip(font, Component.literal(FavoriteFilter.values()[i].name()), mouseX, mouseY);
        if (isHovering(mouseX, mouseY, x, y, width, 18) && (favoriteSearchBox == null || !favoriteSearchBox.isFocused()))
            gui.renderTooltip(font, Component.translatable("recip.irisv.favorites.search"), mouseX, mouseY);
        gui.pose().popPose();
    }

    private static int favoriteWidth(Layout layout) {
        return FAVORITE_COLUMNS * layout.favoriteCellWidth;
    }

    private static int favoriteColumns(Layout layout) {
        return Math.max(FAVORITE_COLUMNS, favoriteWidth(layout) / 20);
    }

    private static int favoriteItemX(Layout layout, int column) {
        int columns = favoriteColumns(layout);
        int start = column * favoriteWidth(layout) / columns;
        int end = (column + 1) * favoriteWidth(layout) / columns;
        return layout.favoritesX + start + (end - start - ITEM_SIZE) / 2;
    }

    private static void renderFavoriteScrollbar(GuiGraphics gui, Layout layout) {
        if (favoriteViews().size() <= layout.favoriteRows * favoriteColumns(layout)) return;
        OverlayPalette palette = OverlayPalette.current();
        int x = layout.favoritesX - 6;
        int y = layout.favoritesY;
        int height = layout.favoriteRows * CELL_SIZE - 2;
        int totalRows = favoriteTotalRows(layout);
        int thumbHeight = Math.max(8, height * layout.favoriteRows / totalRows);
        int thumbTravel = Math.max(1, height - thumbHeight);
        int thumbY = y + thumbTravel * favoriteScroll / maxFavoriteScroll(layout);
        gui.fill(x, y, x + 2, y + height, palette.scrollTrack);
        gui.fill(x, thumbY, x + 2, thumbY + thumbHeight, palette.scrollThumb);
    }

    private static void renderSearchHelpTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        gui.renderTooltip(mc.font, Component.translatable("recip.irisv.search"), mouseX, mouseY);
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

    private static void renderButtonBox(GuiGraphics gui, int x, int y, int width, int height, boolean hovered) {
        RecipeNavigationButton.background(gui, x, y, width, height, hovered, true);
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
        try { favoriteFilter = FavoriteFilter.valueOf(config.favoriteFilter); }
        catch (IllegalArgumentException | NullPointerException ignored) { favoriteFilter = FavoriteFilter.GLOBAL; }
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
        net.opal.irisv.commun.utils.FunctionUtilsChat.clientAction(Minecraft.getInstance().player,
                existing >= 0 ? "irisv.chat.favorite_removed" : "irisv.chat.favorite_added", stack.getHoverName());
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
        FavoriteView view = favoriteViewAt(layout, mouseX, mouseY);
        return view == null ? ItemStack.EMPTY : view.stack;
    }

    private static FavoriteView favoriteViewAt(Layout layout, double mouseX, double mouseY) {
        List<FavoriteView> visibleFavorites = favoriteViews();
        favoriteScroll = clamp(favoriteScroll, 0, maxFavoriteScroll(layout));
        int columns = favoriteColumns(layout);
        int start = favoriteScroll * columns;
        int visible = Math.min(visibleFavorites.size() - start, layout.favoriteRows * columns);
        for (int i = 0; i < visible; i++) {
            int x = favoriteItemX(layout, i % columns);
            int y = layout.favoritesY + (i / columns) * CELL_SIZE;
            if (isHovering(mouseX, mouseY, x, y, CELL_SIZE, CELL_SIZE)) {
                return visibleFavorites.get(start + i);
            }
        }
        return null;
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
        if (favoriteViews().isEmpty()) return false;
        int x = favoriteViews().size() > layout.favoriteRows * favoriteColumns(layout) ? layout.favoritesX - 8 : layout.favoritesX;
        int width = favoriteWidth(layout) + (favoriteViews().size() > layout.favoriteRows * favoriteColumns(layout) ? 8 : 0);
        return isHovering(mouseX, mouseY, x, layout.favoritesY, width, layout.favoriteRows * CELL_SIZE);
    }

    private static int maxFavoriteScroll(Layout layout) {
        return Math.max(0, favoriteTotalRows(layout) - layout.favoriteRows);
    }

    private static int favoriteTotalRows(Layout layout) {
        int columns = favoriteColumns(layout);
        return Math.max(1, (favoriteViews().size() + columns - 1) / columns);
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
        if (screen instanceof RecipeDisplayScreen browser) return browser.originScreen() == null || isSupportedScreen(browser.originScreen());
        var integration = net.opal.irisv.api.compat.IrisVCompatibility.find(screen);
        if (integration != null) return !ConfigOptions.getInstance().disabledRecipeHudCategories.contains(
                net.opal.irisv.api.compat.IrisVCompatibility.settingKey(integration));
        boolean supported = screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen
                || screen instanceof AbstractContainerScreen<?> container && container.getXSize() == 176
                && screen.getClass().getPackageName().equals("net.minecraft.client.gui.screens.inventory");
        return supported && !ConfigOptions.getInstance().disabledRecipeHudCategories.contains(RecipeHudCategory.of(screen).key());
    }

    private static boolean isCreativePlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isCreative();
    }

    static boolean isSearchFocusedFor(Screen screen) {
        return isOverlayEnabled()
                && !recipePanelHidden
                && isSupportedScreen(screen)
                && searchBox != null
                && (searchBox.isFocused() || favoriteSearchBox != null && favoriteSearchBox.isFocused());
    }

    static boolean handleCreativeClick(ItemStack stack, int button) {
        if (stack.isEmpty() || !isCreativePlayer() || (button != 0 && button != 1)
                || (!Screen.hasShiftDown() && !Screen.hasControlDown())) return false;
        giveCreativeStack(stack, Screen.hasShiftDown() ? stack.getMaxStackSize() : 1);
        playClick();
        return true;
    }

    static boolean handleCreativeScroll(ItemStack stack, double delta) {
        if (stack.isEmpty() || !isCreativePlayer() || !Screen.hasControlDown()) return false;
        if (delta > 0) giveCreativeStack(stack, 1);
        return true;
    }

    private static void giveCreativeStack(ItemStack stack, int count) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || !mc.player.isCreative()) return;
        ItemStack remaining = stack.copy();
        remaining.setCount(count);

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

    private static Layout layout(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();
        int inventoryRight = screen instanceof AbstractContainerScreen<?> container
                ? container.getGuiLeft() + container.getXSize() : (screen.width + 176) / 2;
        int rightEdge = screen.width - 14;
        int leftBound = inventoryRight + 42;
        if (rightEdge - leftBound < 96) {
            leftBound = inventoryRight + 8;
        }
        if (screen instanceof RecipeDisplayScreen) {
            leftBound = Math.min(leftBound, rightEdge - Math.max(80, Math.min(162, screen.width / 4)));
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
        int favoritesX = optionsX + 6;
        int inventoryLeft = screen instanceof AbstractContainerScreen<?> container ? container.getGuiLeft() : (screen.width - 176) / 2;
        int favoriteCellWidth = screen instanceof RecipeDisplayScreen ? 20
                : Math.max(20, Math.min(panelWidth, inventoryLeft - favoritesX - 8) / FAVORITE_COLUMNS);
        int favoritesY = Math.max(8, gridY) + 50;
        int favoriteRows = Math.max(1, (optionsY - favoritesY - 6) / CELL_SIZE);

        int prevX = gridX;
        int nextX = gridX + gridWidth - PAGE_BUTTON_SIZE;
        int pageBoxLeft = prevX + PAGE_BUTTON_SIZE;
        int pageBoxRight = nextX;

        return new Layout(panelX, panelY, panelWidth, panelHeight, columns, rows, columns * rows, gridX, gridY, searchX, searchY, searchWidth, resetX, resetY, categoryX, categoryWidth, toolX, visibilityX, visibilityY, optionsX, optionsY, wikiX, wikiY, favoritesX, favoritesY, favoriteRows, prevX, nextX, pageButtonY, pageBoxLeft, pageBoxRight, favoriteCellWidth);
    }

    private static Layout layoutFallback() {
        return new Layout(0, 0, 200, 200, 9, 10, 90, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20);
    }

    static int recipeAreaLeft(Screen screen) {
        Layout layout = layout(screen);
        return layout.favoritesX + favoriteWidth(layout) + 12;
    }

    static int recipeAreaRight(Screen screen) {
        return layout(screen).panelX - 8;
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
            int pageBoxRight,
            int favoriteCellWidth
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
