package net.opal.irisv.recip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.opal.irisv.theme.UiTheme;

import java.util.List;

public class RecipeDisplayScreen extends Screen {
    private final List<RecipeHolder<?>> recipes;
    private final Screen parentScreen;
    private int currentRecipe;

    public RecipeDisplayScreen(List<RecipeHolder<?>> recipes) {
        this(recipes, null);
    }

    public RecipeDisplayScreen(List<RecipeHolder<?>> recipes, Screen parentScreen) {
        super(Component.translatable("recip.irisv.display"));
        this.recipes = recipes;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        if (recipes.size() > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                currentRecipe = (currentRecipe + recipes.size() - 1) % recipes.size();
            }).pos(this.width / 2 - 62, 24).size(20, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                currentRecipe = (currentRecipe + 1) % recipes.size();
            }).pos(this.width / 2 + 42, 24).size(20, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("menu.irisv.return"), button -> {
            returnToParent();
        }).pos(this.width / 2 - 50, this.height - 30).size(100, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        if (recipes.isEmpty() || this.minecraft == null || this.minecraft.level == null) return;

        UiTheme theme = UiTheme.getCurrent();
        RecipeHolder<?> holder = recipes.get(currentRecipe);

        int panelWidth = 204;
        int panelHeight = 128;
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;

        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, theme.tooltip_backgroundColor());
        guiGraphics.fill(x, y, x + panelWidth, y + 1, theme.tooltip_borderColor());
        guiGraphics.fill(x, y + panelHeight - 1, x + panelWidth, y + panelHeight, theme.tooltip_borderColor());
        guiGraphics.fill(x, y, x + 1, y + panelHeight, theme.tooltip_borderColor());
        guiGraphics.fill(x + panelWidth - 1, y, x + panelWidth, y + panelHeight, theme.tooltip_borderColor());

        String page = (currentRecipe + 1) + " / " + recipes.size();
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, y + 8, theme.tooltip_titleColor());
        guiGraphics.drawCenteredString(this.font, Component.literal(page), this.width / 2, y + 22, 0xFFFFFFFF);

        ItemStack result = holder.value().getResultItem(this.minecraft.level.registryAccess());
        ItemStack hovered = renderIngredients(guiGraphics, holder, x + 20, y + 42, mouseX, mouseY);

        int outputX = x + 158;
        int outputY = y + 62;
        renderSlot(guiGraphics, outputX - 2, outputY - 2);
        renderArrow(guiGraphics, x + 102, y + 66);
        guiGraphics.renderFakeItem(result, outputX, outputY);
        guiGraphics.renderItemDecorations(this.font, result, outputX, outputY);

        if (isHovering(mouseX, mouseY, outputX, outputY, 16, 16)) {
            hovered = result;
        }

        String recipeId = this.font.plainSubstrByWidth(holder.id().toString(), panelWidth - 16);
        guiGraphics.drawString(this.font, recipeId, x + 8, y + panelHeight - 14, 0xFF909090, false);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (!hovered.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private ItemStack renderIngredients(GuiGraphics guiGraphics, RecipeHolder<?> holder, int x, int y, int mouseX, int mouseY) {
        NonNullList<Ingredient> ingredients = holder.value().getIngredients();
        int recipeWidth = 3;
        int recipeHeight = 3;
        ItemStack hovered = ItemStack.EMPTY;

        if (holder.value() instanceof CraftingRecipe && holder.value() instanceof ShapedRecipe shaped) {
            recipeWidth = shaped.getWidth();
            recipeHeight = shaped.getHeight();
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                renderSlot(guiGraphics, x + col * 20 - 2, y + row * 20 - 2);
            }
        }

        for (int slot = 0; slot < ingredients.size() && slot < recipeWidth * recipeHeight; slot++) {
            Ingredient ingredient = ingredients.get(slot);
            ItemStack stack = ingredientStack(ingredient);
            if (stack.isEmpty()) continue;

            int slotX = x + (slot % recipeWidth) * 20;
            int slotY = y + (slot / recipeWidth) * 20;
            guiGraphics.renderFakeItem(stack, slotX, slotY);
            guiGraphics.renderItemDecorations(this.font, stack, slotX, slotY);

            if (isHovering(mouseX, mouseY, slotX, slotY, 16, 16)) {
                hovered = stack;
            }
        }

        return hovered;
    }

    private static void renderSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 20, y + 20, 0x66000000);
        guiGraphics.fill(x, y, x + 20, y + 1, 0xAA6E7F92);
        guiGraphics.fill(x, y, x + 1, y + 20, 0xAA6E7F92);
        guiGraphics.fill(x, y + 19, x + 20, y + 20, 0xAA111820);
        guiGraphics.fill(x + 19, y, x + 20, y + 20, 0xAA111820);
    }

    private static void renderArrow(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y + 5, x + 30, y + 9, 0xFF28374A);
        guiGraphics.fill(x + 25, y + 1, x + 29, y + 13, 0xFF28374A);
        guiGraphics.fill(x + 29, y + 3, x + 33, y + 11, 0xFF28374A);
        guiGraphics.fill(x + 33, y + 5, x + 36, y + 9, 0xFF28374A);
        guiGraphics.fill(x, y + 6, x + 26, y + 8, 0xFFFFFFFF);
        guiGraphics.fill(x + 25, y + 4, x + 29, y + 10, 0xFFFFFFFF);
        guiGraphics.fill(x + 29, y + 6, x + 32, y + 8, 0xFFFFFFFF);
    }

    private void returnToParent() {
        if (this.minecraft == null) return;
        if (parentScreen != null) {
            this.minecraft.setScreen(parentScreen);
        } else if (this.minecraft.player != null) {
            this.minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(this.minecraft.player));
        }
    }

    private static ItemStack ingredientStack(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) return ItemStack.EMPTY;
        int index = (int) ((System.currentTimeMillis() / 1000L) % stacks.length);
        return stacks[index];
    }

    private static boolean isHovering(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
