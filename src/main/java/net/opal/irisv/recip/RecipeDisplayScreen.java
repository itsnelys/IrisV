package net.opal.irisv.recip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import java.util.*;

/** Client-side browser: displaying recipes never changes the inventory. */
public class RecipeDisplayScreen extends Screen {
    private static final RecipeType<Recipe<?>> BREWING = new RecipeType<>() {};
    private final List<RecipeLookup.BrewingEntry> brewing;
    private final Screen parentScreen;
    private final ItemStack target;
    private final boolean uses;
    private boolean layered;
    Screen originScreen() { return parentScreen; }
    private final Map<RecipeType<?>, List<RecipeHolder<?>>> groups = new LinkedHashMap<>();
    private final List<RecipeType<?>> categories;
    private final Map<ResourceLocation, List<Ingredient>> ingredientCache = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private record BookmarkButton(Hit bounds, RecipeBookmarks.Entry entry) {}
    private final List<BookmarkButton> favoriteButtons = new ArrayList<>();
    private RecipeBookmarks.Entry renderedBookmark;
    private RecipeBookmarks.Entry hoveredRecipe;
    private List<RecipeBookmarks.Entry> previewEntries = List.of();
    private int previewIndex;
    private ItemStack renderedOutput = ItemStack.EMPTY;
    private final Map<ResourceLocation, Integer> ingredientOffsets = new HashMap<>();
    private int category, page, left, top;
    private int sidebarX, sidebarY, sidebarHeight, recipeCenter;
    private int visibleRecipes, panelHeight, panelStride, renderTop;
    private int frameX, frameY, frameWidth, frameHeight, tabStart, tabCount;
    private float scale;

    public RecipeDisplayScreen(List<RecipeHolder<?>> recipes) { this(recipes, null); }
    public RecipeDisplayScreen(List<RecipeHolder<?>> recipes, Screen parent) { this(recipes, parent, ItemStack.EMPTY, false); }
    private RecipeDisplayScreen(List<RecipeHolder<?>> recipes, Screen parent, ItemStack target, boolean uses) {
        this(recipes, parent, target, uses, null);
    }
    private RecipeDisplayScreen(List<RecipeHolder<?>> recipes, Screen parent, ItemStack target, boolean uses,
                                List<RecipeLookup.BrewingEntry> savedBrewing) {
        super(Component.translatable(uses ? "recip.irisv.uses" : "recip.irisv.display"));
        this.parentScreen = parent instanceof RecipeDisplayScreen browser ? browser.parentScreen : parent;
        this.target = target.copy();
        this.uses = uses;
        brewing = savedBrewing != null ? savedBrewing : target.isEmpty() ? List.of() : RecipeLookup.findBrewing(target, uses);
        recipes.stream().sorted(Comparator.comparing(h -> BuiltInRegistries.RECIPE_TYPE.getKey(h.value().getType()).toString()))
                .forEach(h -> groups.computeIfAbsent(h.value().getType(), key -> new ArrayList<>()).add(h));
        if (!brewing.isEmpty()) groups.put(BREWING, List.of());
        categories = List.copyOf(groups.keySet());
    }
    public static void open(Screen parent, ItemStack stack, boolean uses) {
        RecipeDisplayScreen next = new RecipeDisplayScreen(
                uses ? RecipeLookup.findUses(stack) : RecipeLookup.findRecipesForOutput(stack), parent, stack, uses);
        if (!next.categories.isEmpty()) show(next, parent);
    }

    static RecipeDisplayScreen preview(List<RecipeBookmarks.Entry> entries) {
        RecipeDisplayScreen screen = new RecipeDisplayScreen(List.of(), null, ItemStack.EMPTY, false, List.of());
        screen.minecraft = Minecraft.getInstance();
        screen.font = screen.minecraft.font;
        screen.previewEntries = List.copyOf(entries);
        return screen;
    }

    void scrollPreview(int direction) {
        if (!previewEntries.isEmpty()) previewIndex = Math.floorMod(previewIndex + direction, previewEntries.size());
    }

    RecipeBookmarks.Entry selectedPreview() { return previewEntries.get(previewIndex); }

    static void openSaved(Screen parent, List<RecipeBookmarks.Entry> entries) {
        if (entries.isEmpty()) return;
        List<RecipeHolder<?>> recipes = entries.stream().filter(e -> e.recipe() != null).map(RecipeBookmarks.Entry::recipe).toList();
        List<RecipeLookup.BrewingEntry> brewing = entries.stream().filter(e -> e.brewing() != null).map(RecipeBookmarks.Entry::brewing).toList();
        show(new RecipeDisplayScreen(recipes, parent, entries.getFirst().output(), false, brewing), parent);
    }

    private static void show(RecipeDisplayScreen next, Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (parent instanceof RecipeDisplayScreen browser && browser.layered) mc.popGuiLayer();
        next.layered = true;
        mc.pushGuiLayer(next);
    }

    void renderPreview(GuiGraphics gui, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (previewEntries.isEmpty()) return;
        var entry = selectedPreview();
        panelHeight = entry.recipe() != null && ingredientPages(entry.recipe()) > 1 ? 112 : 88;
        var titleLines = font.split(entry.output().getHoverName(), 148);
        int headerHeight = Math.max(32, 16 + titleLines.size() * font.lineHeight);
        int previewHeight = headerHeight + panelHeight + 30;
        scale = Math.min(1F, Math.min((screenWidth - 16) / 192F, (screenHeight - 16) / (float) previewHeight));
        int boxWidth = Math.round(192 * scale);
        int boxHeight = Math.round(previewHeight * scale);
        int x = mouseX + 12;
        if (x + boxWidth > screenWidth - 4) x = mouseX - boxWidth - 12;
        x = Math.max(4, Math.min(x, screenWidth - boxWidth - 4));
        int y = Math.max(4, Math.min(mouseY - 12, screenHeight - boxHeight - 4));
        left = x + Math.round(8 * scale);
        renderTop = y + Math.round(headerHeight * scale);
        hits.clear();
        gui.pose().pushPose();
        gui.pose().translate(x, y, 600);
        gui.pose().scale(scale, scale, 1);
        gui.fill(3, 3, 195, previewHeight + 3, 0x70000000);
        gui.fill(0, 0, 192, previewHeight, 0xFF101010);
        gui.renderOutline(0, 0, 192, previewHeight, 0xFF000000);
        gui.renderOutline(1, 1, 190, previewHeight - 2, 0xFF747474);
        gui.fill(2, 2, 190, headerHeight - 4, 0xFF262626);
        gui.renderFakeItem(entry.output(), 10, (headerHeight - 16) / 2 - 2);
        int titleY = (headerHeight - 4 - titleLines.size() * font.lineHeight) / 2;
        for (int i = 0; i < titleLines.size(); i++) {
            gui.drawString(font, titleLines.get(i), 34, titleY + i * font.lineHeight, 0xFFF2F2F2, true);
        }
        gui.fill(8, headerHeight - 4, 184, headerHeight - 3, 0xFF555555);
        gui.pose().pushPose();
        gui.pose().translate(8, headerHeight, 0);
        if (entry.recipe() != null) renderRecipe(gui, entry.recipe());
        else renderBrewing(gui, entry.brewing());
        gui.pose().popPose();
        int footerY = headerHeight + panelHeight + 6;
        ItemStack workstation = workstation(entry);
        String pagination = (previewIndex + 1) + " / " + previewEntries.size();
        int paginationWidth = font.width(pagination);
        gui.renderFakeItem(workstation, 8, footerY);
        gui.drawString(font, font.plainSubstrByWidth(workstation.getHoverName().getString(), 146 - paginationWidth),
                28, footerY + 4, 0xFFBDBDBD, false);
        gui.drawString(font, pagination, 184 - paginationWidth, footerY + 4, 0xFFF2F2F2, false);
        gui.pose().popPose();
    }
    @Override protected void init() {
        hits.clear();
        favoriteButtons.clear();
        hoveredRecipe = null;
        int areaLeft = RecipeInventoryOverlay.recipeAreaLeft(this);
        int areaRight = RecipeInventoryOverlay.recipeAreaRight(this);
        int center = (areaLeft + areaRight) / 2;
        frameWidth = Math.min(250, Math.max(80, areaRight - areaLeft - 26));
        frameX = center - (frameWidth + 26) / 2 + 26;
        frameY = 32;
        frameHeight = Math.max(80, height - frameY - 10);
        int bodyTop = frameY + 40;
        int bodyHeight = Math.max(24, frameHeight - 48);
        visibleRecipes = Math.min(3, Math.max(0, recipeCount() - page * 3));
        panelHeight = 88;
        for (int i = 0; i < visibleRecipes && !isBrewing(); i++) {
            if (ingredientPages(currentRecipes().get(page * 3 + i)) > 1) panelHeight = 112;
        }
        int totalHeight = 3 * (panelHeight + 8) - 8;
        scale = Math.min(1F, Math.min(Math.max(1, frameWidth - 38) / 176F, bodyHeight / (float) totalHeight));
        int panelWidth = Math.round(176 * scale);
        left = frameX + (frameWidth - panelWidth - 24) / 2;
        panelStride = bodyHeight / 3;
        top = bodyTop + Math.max(0, (panelStride - Math.round(panelHeight * scale)) / 2);
        recipeCenter = frameX + frameWidth / 2;
        sidebarX = frameX - 24;
        sidebarY = frameY + 4;
        sidebarHeight = 26;
        tabCount = Math.max(1, bodyHeight / 24);
        tabStart = category / tabCount * tabCount;
        for (int i = tabStart; i < Math.min(categories.size(), tabStart + tabCount); i++) {
            final int index = i;
            addRenderableWidget(new StationButton(sidebarX, bodyTop + (i - tabStart) * 24,
                    station(categories.get(i)), i == category, b -> { category = index; page = 0; rebuildWidgets(); }));
        }
        sidebarY = bodyTop;
        sidebarHeight = Math.min(tabCount, categories.size()) * 24;
        addRenderableWidget(new RecipeNavigationButton(frameX + 5, frameY + 4, 14, false, b -> changeCategory(-1))).active = categories.size() > 1;
        addRenderableWidget(new RecipeNavigationButton(frameX + frameWidth - 19, frameY + 4, 14, true, b -> changeCategory(1))).active = categories.size() > 1;
        addRenderableWidget(new RecipeNavigationButton(frameX + 5, frameY + 20, 14, false, b -> changePage(-1))).active = pageCount() > 1;
        addRenderableWidget(new RecipeNavigationButton(frameX + frameWidth - 19, frameY + 20, 14, true, b -> changePage(1))).active = pageCount() > 1;
        for (int i = 0; i < visibleRecipes && !isBrewing(); i++) {
            RecipeHolder<?> holder = currentRecipes().get(page * 3 + i);
            int ingredientPages = ingredientPages(holder);
            if (ingredientPages <= 1) continue;
            int controlsY = top + i * panelStride + Math.round(90 * scale);
            int side = Math.max(12, Math.round(18 * scale));
            addRenderableWidget(new RecipeNavigationButton(left + Math.round(8 * scale), controlsY, side, false, b -> {
                ingredientOffsets.compute(holder.id(), (key, value) -> Math.floorMod((value == null ? 0 : value) - 1, ingredientPages)); hits.clear();
            }));
            addRenderableWidget(new RecipeNavigationButton(left + Math.round(150 * scale), controlsY, side, true, b -> {
                ingredientOffsets.compute(holder.id(), (key, value) -> ((value == null ? 0 : value) + 1) % ingredientPages); hits.clear();
            }));
        }
    }
    private int ingredientPages(RecipeHolder<?> holder) {
        return Math.max(1, (ingredientCache.computeIfAbsent(holder.id(), key -> RecipeLookup.ingredients(holder)).size() + 11) / 12);
    }
    private List<RecipeHolder<?>> currentRecipes() { return categories.isEmpty() ? List.of() : groups.get(categories.get(category)); }
    private boolean isBrewing() { return !categories.isEmpty() && categories.get(category) == BREWING; }
    private int recipeCount() { return isBrewing() ? brewing.size() : currentRecipes().size(); }
    private int pageCount() { return Math.max(1, (recipeCount() + 2) / 3); }
    private void changeCategory(int amount) { category = Math.floorMod(category + amount, categories.size()); page = 0; rebuildWidgets(); }
    private void changePage(int amount) {
        if (recipeCount() > 0) page = Math.floorMod(page + amount, pageCount());
        hits.clear();
        rebuildWidgets();
    }
    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(gui);
    }

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);
        gui.fill(frameX + 21, frameY + 4, frameX + frameWidth - 21, frameY + 18, 0x88000000);
        gui.fill(frameX + 21, frameY + 20, frameX + frameWidth - 21, frameY + 34, 0x88000000);
        if (!categories.isEmpty()) gui.drawCenteredString(font,
                font.plainSubstrByWidth(station(categories.get(category)).getHoverName().getString(), Math.max(1, frameWidth - 46)),
                recipeCenter, frameY + 7, 0xFFFFFFFF);
        hits.clear();
        List<RecipeHolder<?>> recipes = currentRecipes();
        favoriteButtons.clear();
        hoveredRecipe = null;
        if (recipeCount() == 0) gui.drawCenteredString(font, Component.translatable("recip.irisv.none"), width / 2, height / 2, 0xFFFFFFFF);
        else if (minecraft != null && minecraft.level != null) {
            gui.drawCenteredString(font, (page + 1) + " / " + pageCount(), recipeCenter, frameY + 23, 0xFFFFFFFF);
            for (int i = 0; i < visibleRecipes; i++) {
                renderTop = top + i * panelStride;
                gui.pose().pushPose();
                gui.pose().translate(left, renderTop, 0);
                gui.pose().scale(scale, scale, 1);
                if (isBrewing()) renderBrewing(gui, brewing.get(page * 3 + i));
                else renderRecipe(gui, recipes.get(page * 3 + i));
                gui.pose().popPose();
                if (!renderedOutput.isEmpty()) renderFavoriteButton(gui, renderedOutput);
                if (mouseX >= left && mouseX <= left + Math.round(176 * scale) + 24
                        && mouseY >= renderTop && mouseY < renderTop + Math.round(panelHeight * scale)) hoveredRecipe = renderedBookmark;
            }
        }
        for (var widget : renderables) widget.render(gui, mouseX, mouseY, partialTick);
        for (BookmarkButton bookmark : favoriteButtons) {
            Hit bounds = bookmark.bounds;
            if (mouseX >= bounds.x && mouseX < bounds.x + 20 && mouseY >= bounds.y - 23 && mouseY < bounds.y - 3) {
                hoveredRecipe = bookmark.entry;
                gui.renderTooltip(font, Component.translatable(PinnedRecipeHud.isPinned(bookmark.entry) ? "recip.irisv.unpin" : "recip.irisv.pin"), mouseX, mouseY);
            }
        }
        for (BookmarkButton bookmark : favoriteButtons) if (bookmark.bounds.contains(mouseX, mouseY)) {
            Hit button = bookmark.bounds;
            gui.renderOutline(button.x, button.y, button.size, button.size, 0xFFFFFFFF);
            gui.renderTooltip(font, Component.translatable(RecipeBookmarks.contains(bookmark.entry)
                    ? "recip.irisv.favorite.remove" : "recip.irisv.favorite.add"), mouseX, mouseY);
        }
        if (net.opal.irisv.option.ConfigOptions.getInstance().recipeAvailability && RecipeAvailability.isWorkstation(parentScreen)) {
            for (BookmarkButton bookmark : favoriteButtons) {
                Hit bounds = bookmark.bounds;
                if (mouseX >= bounds.x && mouseX < bounds.x + 20 && mouseY >= bounds.y + 23 && mouseY < bounds.y + 43) {
                    String key = !RecipeAvailability.compatible(parentScreen, bookmark.entry) ? "recip.irisv.availability.station"
                            : RecipeTransfer.canTransfer(parentScreen, bookmark.entry) ? "recip.irisv.availability.ready" : "recip.irisv.availability.missing";
                    gui.renderTooltip(font, font.split(Component.translatable(key), 210), mouseX, mouseY);
                }
            }
        }
        for (Hit hit : hits) if (hit.contains(mouseX, mouseY)) {
            gui.renderOutline(hit.x - 1, hit.y - 1, hit.size + 2, hit.size + 2, 0xFFFFFFFF);
            gui.renderTooltip(font, hit.stack, mouseX, mouseY);
            break;
        }
    }
    private void renderFavoriteButton(GuiGraphics gui, ItemStack output) {
        int x = left + Math.round(176 * scale) + 4;
        int y = renderTop + Math.max(0, (Math.round(panelHeight * scale) - 20) / 2);
        renderFrame(gui, x, y, 20, 20);
        gui.blit(ResourceLocation.fromNamespaceAndPath("irisv", "textures/gui/wiki_icon.png"), x + 2, y + 2,
                0.0F, 0.0F, 16, 16, 16, 16);
        if (RecipeBookmarks.contains(renderedBookmark)) gui.renderOutline(x, y, 20, 20, 0xFFFFD36A);
        favoriteButtons.add(new BookmarkButton(new Hit(x, y, 20, output.copy()), renderedBookmark));
        renderFrame(gui, x, y - 23, 20, 20);
        gui.renderFakeItem(new ItemStack(Items.ITEM_FRAME), x + 2, y - 21);
        if (PinnedRecipeHud.isPinned(renderedBookmark)) gui.renderOutline(x, y - 23, 20, 20, 0xFFFFD36A);
        if (net.opal.irisv.option.ConfigOptions.getInstance().recipeAvailability && RecipeAvailability.isWorkstation(parentScreen)) {
            boolean available = RecipeTransfer.canTransfer(parentScreen, renderedBookmark);
            int indicatorY = y + 23;
            renderFrame(gui, x, indicatorY, 20, 20);
            gui.renderOutline(x, indicatorY, 20, 20, available ? 0xFF5CEC78 : 0xFFFF6565);
            gui.setColor(available ? 0.36F : 1F, available ? 1F : 0.36F, 0.36F, 1F);
            gui.blit(ResourceLocation.fromNamespaceAndPath("irisv", "textures/gui/page_right.png"), x + 2, indicatorY + 2,
                    0F, 0F, 16, 16, 16, 16);
            gui.setColor(1F, 1F, 1F, 1F);
        }
    }
    private static void renderFrame(GuiGraphics gui, int x, int y, int width, int height) {
        RecipeNavigationButton.background(gui, x, y, width, height, false, true);
    }

    private void renderRecipeFrame(GuiGraphics gui) {
        gui.fill(0, 0, 176, panelHeight, 0xFF8B8B8B);
        gui.fill(1, 1, 176, panelHeight, 0xFFE4E4E4);
        gui.fill(1, 1, 174, panelHeight - 2, 0xFFC6C6C6);
    }
    private void renderBrewing(GuiGraphics gui, RecipeLookup.BrewingEntry entry) {
        renderedBookmark = RecipeBookmarks.brewing(entry);
        renderedOutput = entry.output();
        renderRecipeFrame(gui);
        gui.blit(ResourceLocation.withDefaultNamespace("textures/gui/container/brewing_stand.png"), 0, 0, 0, 0, 176, 82);
        gui.drawString(font, Component.translatable("block.minecraft.brewing_stand"), 8, 6, 0xFF404040, false);
        slot(gui, entry.reagent(), 79, 17);
        slot(gui, entry.input(), 56, 51);
        slot(gui, entry.output(), 102, 51);
        slot(gui, new ItemStack(Items.BLAZE_POWDER), 17, 17);
        gui.drawString(font, ">", 84, 54, 0xFF404040, false);
        gui.drawString(font, "20 s", 8, 76, 0xFF404040, false);
    }
    private void renderRecipe(GuiGraphics gui, RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        RecipeType<?> type = recipe.getType();
        ItemStack station = station(type);
        List<Ingredient> ingredients = ingredientCache.computeIfAbsent(holder.id(), key -> RecipeLookup.ingredients(holder));
        ItemStack output = recipe.getResultItem(minecraft.level.registryAccess());
        int ingredientPage = ingredientOffsets.getOrDefault(holder.id(), 0);
        renderRecipeFrame(gui);
        boolean standardCrafting = recipe instanceof CraftingRecipe && ingredients.size() <= 9
                && (!(recipe instanceof ShapedRecipe shape) || shape.getWidth() <= 3 && shape.getHeight() <= 3);
        // Reuse the actual working area of vanilla containers, without the inventory.
        String texture = standardCrafting ? null : type == RecipeType.SMELTING ? "furnace"
                : type == RecipeType.BLASTING ? "blast_furnace" : type == RecipeType.SMOKING ? "smoker"
                : type == RecipeType.STONECUTTING ? "stonecutter" : type == RecipeType.SMITHING ? "smithing" : null;
        if (texture != null) gui.blit(ResourceLocation.withDefaultNamespace("textures/gui/container/" + texture + ".png"), 0, 0, 0, 0, 176, 82);
        if (!standardCrafting) gui.drawString(font, font.plainSubstrByWidth(station.getHoverName().getString(), 160), 8, 6, 0xFF404040, false);
        if (standardCrafting) {
            for (int i = 0; i < 9; i++) slot(gui, ItemStack.EMPTY, 30 + i % 3 * 18, 17 + i / 3 * 18);
            gui.blitSprite(ResourceLocation.withDefaultNamespace("container/furnace/burn_progress"), 94, 35, 24, 16);
            int columns = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            for (int i = 0; i < ingredients.size(); i++) slot(gui, ingredient(ingredients.get(i)), 30 + i % columns * 18, 17 + i / columns * 18);
            slot(gui, output, 124, 35);
            if (!(recipe instanceof ShapedRecipe)) gui.drawString(font, Component.translatable("recip.irisv.shapeless"), 8, 76, 0xFF404040, false);
        } else if (recipe instanceof AbstractCookingRecipe cooking) {
            slot(gui, ingredients.isEmpty() ? ItemStack.EMPTY : ingredient(ingredients.getFirst()), 56, 17);
            slot(gui, output, 116, 35);
            if (type != RecipeType.CAMPFIRE_COOKING) {
                slot(gui, uses && target.getBurnTime(type) > 0 ? target.copyWithCount(1) : new ItemStack(Items.COAL), 56, 53);
                gui.blitSprite(ResourceLocation.withDefaultNamespace("container/furnace/lit_progress"), 57, 36, 14, 14);
            } else slot(gui, station, 56, 53);
            gui.blitSprite(ResourceLocation.withDefaultNamespace("container/furnace/burn_progress"), 79, 34, 24, 16);
            gui.drawString(font, Component.translatable("recip.irisv.cooking", cooking.getCookingTime() / 20F, cooking.getExperience()), 8, 76, 0xFF404040, false);
        } else if (recipe instanceof SmithingRecipe smithing) {
            ItemStack template = ingredient(ingredients.get(0)), base = ingredient(ingredients.get(1)), addition = ingredient(ingredients.get(2));
            if (!uses && recipe instanceof SmithingTrimRecipe && smithing.isBaseIngredient(target)) base = target.copyWithCount(1);
            ItemStack assembled = smithing.assemble(new SmithingRecipeInput(template, base, addition), minecraft.level.registryAccess());
            if (!assembled.isEmpty()) output = assembled;
            slot(gui, template, 8, 48); slot(gui, base, 26, 48); slot(gui, addition, 44, 48); slot(gui, output, 98, 48);
        } else if (type == RecipeType.STONECUTTING) {
            slot(gui, ingredients.isEmpty() ? ItemStack.EMPTY : ingredient(ingredients.getFirst()), 20, 33);
            slot(gui, output, 143, 33);
        } else {
            int start = ingredientPage * 12;
            for (int i = start; i < Math.min(start + 12, ingredients.size()); i++)
                slot(gui, ingredient(ingredients.get(i)), 10 + (i - start) % 4 * 18, 22 + (i - start) / 4 * 18);
            gui.drawString(font, ">", 103, 39, 0xFF404040, false);
            slot(gui, output, 138, 35);
        }
        if (ingredients.isEmpty() || output.isEmpty()) gui.drawString(font, Component.translatable("recip.irisv.dynamic"), 8, 76, 0xFF404040, false);
        if (ingredientPages(holder) > 1) gui.drawCenteredString(font, (ingredientPage + 1) + " / " + ingredientPages(holder), 88, 94, 0xFF404040);
        renderedOutput = output;
        renderedBookmark = RecipeBookmarks.recipe(holder, output);
    }
    private void slot(GuiGraphics gui, ItemStack stack, int x, int y) {
        gui.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        gui.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
        gui.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        if (stack.isEmpty()) return;
        gui.renderFakeItem(stack, x, y);
        gui.renderItemDecorations(font, stack, x, y);
        hits.add(new Hit(left + Math.round(x * scale), renderTop + Math.round(y * scale), Math.max(1, Math.round(16 * scale)), stack.copy()));
    }
    private ItemStack ingredient(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) return ItemStack.EMPTY;
        if (uses && ingredient.test(target)) return target.copyWithCount(1);
        return stacks[(int) ((System.currentTimeMillis() / 1000) % stacks.length)];
    }
    static ItemStack workstation(RecipeBookmarks.Entry entry) {
        return entry.brewing() != null ? new ItemStack(Items.BREWING_STAND) : station(entry.recipe().value().getType());
    }

    private static ItemStack station(RecipeType<?> type) {
        if (type == BREWING) return new ItemStack(Items.BREWING_STAND);
        if (type == RecipeType.CRAFTING) return new ItemStack(Items.CRAFTING_TABLE);
        if (type == RecipeType.SMELTING) return new ItemStack(Items.FURNACE);
        if (type == RecipeType.BLASTING) return new ItemStack(Items.BLAST_FURNACE);
        if (type == RecipeType.SMOKING) return new ItemStack(Items.SMOKER);
        if (type == RecipeType.CAMPFIRE_COOKING) return new ItemStack(Items.CAMPFIRE);
        if (type == RecipeType.STONECUTTING) return new ItemStack(Items.STONECUTTER);
        if (type == RecipeType.SMITHING) return new ItemStack(Items.SMITHING_TABLE);
        ItemStack stack = new ItemStack(Items.KNOWLEDGE_BOOK);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(BuiltInRegistries.RECIPE_TYPE.getKey(type).toString()));
        return stack;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) for (BookmarkButton bookmark : favoriteButtons) {
            Hit bounds = bookmark.bounds;
            if (x >= bounds.x && x < bounds.x + 20 && y >= bounds.y - 23 && y < bounds.y - 3) {
                PinnedRecipeHud.toggle(bookmark.entry); return true;
            }
        }
        if (button == 0 && net.opal.irisv.option.ConfigOptions.getInstance().recipeAvailability) {
            for (BookmarkButton bookmark : favoriteButtons) {
                Hit bounds = bookmark.bounds;
                if (x >= bounds.x && x < bounds.x + 20 && y >= bounds.y + 23 && y < bounds.y + 43
                        && RecipeAvailability.isWorkstation(parentScreen)) {
                    if (RecipeTransfer.transfer(parentScreen, bookmark.entry, Screen.hasShiftDown())) {
                        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
                        onClose();
                    }
                    return true;
                }
            }
        }
        if (button == 0) for (BookmarkButton favorite : favoriteButtons) if (favorite.bounds.contains(x, y)) {
            RecipeBookmarks.toggle(favorite.entry);
            return true;
        }
        if (button == 0 || button == 1) for (Hit hit : hits) if (hit.contains(x, y)) {
            if (!RecipeInventoryOverlay.handleCreativeClick(hit.stack, button)) open(this, hit.stack, button == 1);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (vertical == 0) return false;
        for (Hit hit : hits) if (hit.contains(x, y) && RecipeInventoryOverlay.handleCreativeScroll(hit.stack, vertical)) return true;
        if (!categories.isEmpty() && x >= sidebarX && x < sidebarX + 22
                && y >= sidebarY && y < sidebarY + sidebarHeight) {
            changeCategory(vertical > 0 ? -1 : 1);
            return true;
        }
        if (!categories.isEmpty() && x >= frameX && x < frameX + frameWidth && y >= frameY - 24 && y < frameY + 18) {
            changeCategory(vertical > 0 ? -1 : 1);
            return true;
        }
        changePage(vertical > 0 ? -1 : 1); return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (RecipeInventoryOverlay.isSearchFocusedFor(this)) return super.keyPressed(key, scan, modifiers);
        if (hoveredRecipe != null && net.opal.irisv.client.ClientKeyBindings.PIN.matches(key, scan)) {
            PinnedRecipeHud.toggle(hoveredRecipe); return true;
        }
        double mx = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
        double my = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
        for (Hit hit : hits) if (hit.contains(mx, my) && RecipeInventoryOverlay.handleRecipeKey(this, hit.stack, key, scan)) return true;
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) { onClose(); return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) { changePage(1); return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) { changePage(-1); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() {
        if (minecraft == null) return;
        if (layered) {
            minecraft.popGuiLayer();
            if (minecraft.screen == parentScreen) parentScreen.init(minecraft, width, height);
            return;
        }
        if (parentScreen != null) minecraft.setScreen(parentScreen);
        else if (minecraft.player != null) minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(minecraft.player));
    }
    private record Hit(int x, int y, int size, ItemStack stack) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + size && my >= y && my < y + size; }
    }
    private static class StationButton extends Button {
        private final ItemStack icon;
        private final boolean selected;
        StationButton(int x, int y, ItemStack icon, boolean selected, OnPress action) {
            super(x, y, 22, 22, icon.getHoverName(), action, DEFAULT_NARRATION);
            this.icon = icon; this.selected = selected;
            setTooltip(Tooltip.create(icon.getHoverName()));
        }
        @Override protected void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
            Component label = getMessage();
            setMessage(Component.empty());
            super.renderWidget(gui, mx, my, partial);
            setMessage(label);
            if (selected) gui.renderOutline(getX(), getY(), width, height, 0xFFFFD36A);
            gui.renderFakeItem(icon, getX() + 3, getY() + 3);
        }
    }
}
