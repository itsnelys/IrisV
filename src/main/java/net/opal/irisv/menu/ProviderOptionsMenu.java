package net.opal.irisv.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.theme.UiTheme;

import java.util.ArrayList;
import java.util.List;

public class ProviderOptionsMenu extends Screen {
    private static final int CONTENT_TOP = 58;
    private static final int CONTENT_BOTTOM_PADDING = 52;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 6;
    private static final int SECTION_TITLE_HEIGHT = 18;
    private static final int SECTION_GAP = 16;
    private static final int MIN_TWO_COLUMN_WIDTH = 460;
    private static final int SINGLE_COLUMN_WIDTH = 200;
    private static final int TWO_COLUMN_WIDTH = 428;
    private static final int COLUMN_GAP = 28;

    private final Screen parent;
    private final List<Section> sections = new ArrayList<>();
    private int scrollOffset;

    protected ProviderOptionsMenu(Screen parent) {
        super(Component.translatable("menu.irisv.providers.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        sections.clear();
        ConfigOptions config = ConfigOptions.getInstance();

        Section general = addSection("menu.irisv.providers.section.tooltips");
        general.add(toggleText("menu.irisv.providers.blocks", config.enableBlockProviderTooltips), button -> {
            config.enableBlockProviderTooltips = !config.enableBlockProviderTooltips;
            button.setMessage(toggleText("menu.irisv.providers.blocks", config.enableBlockProviderTooltips));
            config.save();
        }, "menu.irisv.providers.tooltip.blocks");
        general.add(toggleText("menu.irisv.providers.entities", config.enableEntityTooltip), button -> {
            config.enableEntityTooltip = !config.enableEntityTooltip;
            button.setMessage(toggleText("menu.irisv.providers.entities", config.enableEntityTooltip));
            config.save();
        }, "menu.irisv.providers.tooltip.entities");
        general.add(toggleText("menu.irisv.providers.drops", config.enableDropTooltip), button -> {
            config.enableDropTooltip = !config.enableDropTooltip;
            button.setMessage(toggleText("menu.irisv.providers.drops", config.enableDropTooltip));
            config.save();
        }, "menu.irisv.providers.tooltip.drops");
        general.add(toggleText("menu.irisv.providers.fluids", config.enableFluidTooltips), button -> {
            config.enableFluidTooltips = !config.enableFluidTooltips;
            button.setMessage(toggleText("menu.irisv.providers.fluids", config.enableFluidTooltips));
            config.save();
        }, "menu.irisv.providers.tooltip.fluids");
        general.add(toggleText("menu.irisv.providers.inventories", config.enableInventoryTooltips), button -> {
            config.enableInventoryTooltips = !config.enableInventoryTooltips;
            button.setMessage(toggleText("menu.irisv.providers.inventories", config.enableInventoryTooltips));
            config.save();
        }, "menu.irisv.providers.tooltip.inventories");

        Section inventory = addSection("menu.irisv.providers.section.inventory");
        inventory.add(toggleText("menu.irisv.providers.recipes", config.enableRecipeOverlay), button -> {
            config.enableRecipeOverlay = !config.enableRecipeOverlay;
            button.setMessage(toggleText("menu.irisv.providers.recipes", config.enableRecipeOverlay));
            config.save();
        }, "menu.irisv.providers.tooltip.recipes");

        inventory.add(toggleText("menu.irisv.inventory_search", config.inventorySearchHighlight), button -> {
            config.inventorySearchHighlight = !config.inventorySearchHighlight;
            button.setMessage(toggleText("menu.irisv.inventory_search", config.inventorySearchHighlight));
            config.save();
        }, "menu.irisv.tooltip.inventory_search");

        Section hud = addSection("menu.irisv.providers.section.hud");
        inventory.add(toggleText("menu.irisv.recipe_availability", config.recipeAvailability), button -> {
            config.recipeAvailability = !config.recipeAvailability;
            button.setMessage(toggleText("menu.irisv.recipe_availability", config.recipeAvailability));
            config.save();
        }, "menu.irisv.tooltip.recipe_availability");
        hud.add(toggleText("menu.irisv.providers.hud", config.enableIndicators), button -> {
            config.enableIndicators = !config.enableIndicators;
            button.setMessage(toggleText("menu.irisv.providers.hud", config.enableIndicators));
            config.save();
        }, "menu.irisv.providers.tooltip.hud");
        hud.add(toggleText("menu.irisv.providers.tool_durability", config.enableToolDurabilityIndicator), button -> {
            config.enableToolDurabilityIndicator = !config.enableToolDurabilityIndicator;
            button.setMessage(toggleText("menu.irisv.providers.tool_durability", config.enableToolDurabilityIndicator));
            config.save();
        }, "menu.irisv.providers.tooltip.tool_durability");
        hud.add(toggleText("menu.irisv.providers.durability_details", config.showDurabilityDetails), button -> {
            config.showDurabilityDetails = !config.showDurabilityDetails;
            button.setMessage(toggleText("menu.irisv.providers.durability_details", config.showDurabilityDetails));
            config.save();
        }, "menu.irisv.providers.tooltip.durability_details");

        Section recipeHuds = addSection("menu.irisv.providers.section.recipe_huds");
        for (var category : net.opal.irisv.recip.RecipeHudCategory.values()) {
            recipeHuds.add(toggleText(category.labelKey(), !config.disabledRecipeHudCategories.contains(category.key())), button -> {
                if (!config.disabledRecipeHudCategories.remove(category.key())) config.disabledRecipeHudCategories.add(category.key());
                button.setMessage(toggleText(category.labelKey(), !config.disabledRecipeHudCategories.contains(category.key())));
                config.save();
            }, "menu.irisv.tooltip.hud_category");
        }

        var integrations = net.opal.irisv.api.compat.IrisVCompatibility.integrations();
        if (!integrations.isEmpty()) {
            Section compatibility = addSection("menu.irisv.providers.section.compatibility");
            for (var integration : integrations) {
                String key = net.opal.irisv.api.compat.IrisVCompatibility.settingKey(integration);
                compatibility.add(integrationText(integration.title(), !config.disabledRecipeHudCategories.contains(key)), button -> {
                    if (!config.disabledRecipeHudCategories.remove(key)) config.disabledRecipeHudCategories.add(key);
                    button.setMessage(integrationText(integration.title(), !config.disabledRecipeHudCategories.contains(key)));
                    config.save();
                }, "menu.irisv.tooltip.compatibility");
            }
        }

        this.addRenderableWidget(Button.builder(Component.translatable("menu.irisv.return"), button -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).tooltip(tooltip("menu.irisv.tooltip.return")).pos((this.width - returnButtonWidth()) / 2, this.height - 30).size(returnButtonWidth(), BUTTON_HEIGHT).build());

        clampScroll();
        updateButtonPositions();
    }

    private Section addSection(String titleKey) {
        Section section = new Section(titleKey);
        sections.add(section);
        return section;
    }

    private Button addOptionButton(Component label, Button.OnPress onPress, String tooltipKey) {
        Button button = Button.builder(label, onPress)
                .tooltip(tooltip(tooltipKey))
                .pos(0, 0)
                .size(120, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(button);
        return button;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        UiTheme theme = UiTheme.getCurrent();
        guiGraphics.fill(0, 0, this.width, this.height, theme.gui_bgOverlay());

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        renderHeader(guiGraphics, theme);
        renderFooter(guiGraphics, theme);

        renderSections(guiGraphics, theme);
        renderScrollbar(guiGraphics, theme);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int previous = scrollOffset;
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * (BUTTON_HEIGHT + ROW_GAP)), 0, maxScroll());
        if (previous != scrollOffset) {
            updateButtonPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateButtonPositions() {
        Layout layout = createLayout();
        int listBottom = listBottom();

        for (Section section : sections) {
            for (Option option : section.options) {
                option.button.setX(option.x);
                option.button.setY(option.y - scrollOffset);
                option.button.setWidth(option.width);
                boolean visible = option.button.getY() >= CONTENT_TOP && option.button.getY() + BUTTON_HEIGHT <= listBottom;
                option.button.visible = visible;
                option.button.active = visible;
            }
        }
    }

    private void renderSections(GuiGraphics guiGraphics, UiTheme theme) {
        Layout layout = createLayout();
        int listBottom = listBottom();

        for (Section section : sections) {
            int y = section.y - scrollOffset;
            if (y + SECTION_TITLE_HEIGHT < CONTENT_TOP || y > listBottom) continue;

            Component title = sectionTitle(section.titleKey);
            int separatorY = y + SECTION_TITLE_HEIGHT - 1;

            guiGraphics.drawString(this.font, title, section.x, y + 4, 0xFFFFFF, true);
            guiGraphics.fill(section.x, separatorY, section.x + section.width, separatorY + 1, theme.gui_separatorLine());
        }
    }

    private Layout createLayout() {
        boolean twoColumns = this.width >= MIN_TWO_COLUMN_WIDTH;
        int contentWidth = Math.min(twoColumns ? TWO_COLUMN_WIDTH : SINGLE_COLUMN_WIDTH, Math.max(120, this.width - 36));
        int left = (this.width - contentWidth) / 2;
        int columnGap = twoColumns ? COLUMN_GAP : 0;
        int columnWidth = twoColumns ? (contentWidth - columnGap) / 2 : contentWidth;
        int rightX = left + columnWidth + columnGap;

        int generalY = CONTENT_TOP;
        int rightY = CONTENT_TOP;
        int stackedY = CONTENT_TOP;

        for (Section section : sections) {
            int sectionX;
            int sectionY;
            if (twoColumns && section.titleKey.equals("menu.irisv.providers.section.tooltips")) {
                sectionX = left;
                sectionY = generalY;
                generalY += positionSection(section, sectionX, sectionY, columnWidth);
            } else if (twoColumns) {
                sectionX = rightX;
                sectionY = rightY;
                rightY += positionSection(section, sectionX, sectionY, columnWidth) + SECTION_GAP;
            } else {
                sectionX = left;
                sectionY = stackedY;
                stackedY += positionSection(section, sectionX, sectionY, columnWidth) + SECTION_GAP;
            }
        }

        int contentHeight = twoColumns ? Math.max(generalY, rightY - SECTION_GAP) - CONTENT_TOP : stackedY - SECTION_GAP - CONTENT_TOP;
        return new Layout(left, contentWidth, contentHeight);
    }

    private int positionSection(Section section, int x, int y, int width) {
        section.x = x;
        section.y = y;
        section.width = width;

        int halfWidth = (width - 4) / 2;
        int buttonY = y + SECTION_TITLE_HEIGHT + ROW_GAP;
        int column = 0;

        for (int i = 0; i < section.options.size(); i++) {
            Option option = section.options.get(i);
            boolean lastOddOption = i == section.options.size() - 1 && column == 0;
            boolean fullWidth = section.options.size() == 1 || lastOddOption || section.titleKey.equals("menu.irisv.providers.section.recipe_huds")
                    || section.titleKey.equals("menu.irisv.providers.section.compatibility");

            option.x = fullWidth || column == 0 ? x : x + halfWidth + 4;
            option.y = buttonY;
            option.width = fullWidth ? width : halfWidth;

            if (fullWidth || column == 1) {
                buttonY += BUTTON_HEIGHT + ROW_GAP;
                column = 0;
            } else {
                column = 1;
            }
        }
        return buttonY - y - ROW_GAP;
    }

    private void renderHeader(GuiGraphics guiGraphics, UiTheme theme) {
        guiGraphics.fill(0, 0, this.width, 35, theme.gui_barColor());
        guiGraphics.fill(0, 34, this.width, 35, theme.gui_lineColor());
        guiGraphics.drawCenteredString(this.font, Component.literal("§l").append(this.title), this.width / 2, 12, 0xFFFFFF);
    }

    private void renderFooter(GuiGraphics guiGraphics, UiTheme theme) {
        int footerY = this.height - 40;
        int centerX = this.width / 2;
        int returnWidth = returnButtonWidth();
        int holeLeft = centerX - returnWidth / 2;
        int holeRight = holeLeft + returnWidth;

        guiGraphics.fill(0, footerY, holeLeft, this.height, theme.gui_barColor());
        guiGraphics.fill(0, footerY, holeLeft, footerY + 1, theme.gui_lineColor());
        guiGraphics.fill(holeRight, footerY, this.width, this.height, theme.gui_barColor());
        guiGraphics.fill(holeRight, footerY, this.width, footerY + 1, theme.gui_lineColor());

        guiGraphics.fill(holeLeft, footerY, holeRight, this.height - 30, theme.gui_barColor());
        guiGraphics.fill(holeLeft, footerY, holeRight, footerY + 1, theme.gui_lineColor());
        guiGraphics.fill(holeLeft, this.height - 10, holeRight, this.height, theme.gui_barColor());
    }

    private void renderScrollbar(GuiGraphics guiGraphics, UiTheme theme) {
        if (maxScroll() <= 0) return;

        Layout layout = createLayout();
        int top = CONTENT_TOP;
        int bottom = listBottom();
        int barX = layout.left + layout.width + 8;
        int trackHeight = bottom - top;
        int thumbHeight = Math.max(18, (trackHeight * trackHeight) / Math.max(trackHeight, contentHeight()));
        int thumbY = top + (int) ((trackHeight - thumbHeight) * (scrollOffset / (float) maxScroll()));

        guiGraphics.fill(barX, top, barX + 2, bottom, theme.gui_separatorLine());
        guiGraphics.fill(barX - 1, thumbY, barX + 3, thumbY + thumbHeight, theme.gui_lineColor());
    }

    private void clampScroll() {
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (listBottom() - CONTENT_TOP));
    }

    private int contentHeight() {
        return createLayout().contentHeight;
    }

    private int listBottom() {
        return this.height - CONTENT_BOTTOM_PADDING;
    }

    private int returnButtonWidth() {
        return Math.min(200, Math.max(120, this.width - 36));
    }

    private static Component sectionTitle(String key) {
        return Component.literal("§8> §f").append(Component.translatable(key));
    }

    private static Component integrationText(Component title, boolean enabled) {
        return title.copy().append(": ").append(Component.translatable(enabled ? "menu.irisv.on" : "menu.irisv.off"));
    }

    private static Component toggleText(String key, boolean enabled) {
        UiTheme theme = UiTheme.getCurrent();
        Component state = Component.literal(enabled ? theme.gui_onColor() : theme.gui_offColor())
                .append(Component.translatable(enabled ? "menu.irisv.on" : "menu.irisv.off"));
        return Component.translatable(key, state);
    }

    private class Section {
        private final String titleKey;
        private final List<Option> options = new ArrayList<>();
        private int x;
        private int y;
        private int width;

        private Section(String titleKey) {
            this.titleKey = titleKey;
        }

        private void add(Component label, Button.OnPress onPress, String tooltipKey) {
            options.add(new Option(addOptionButton(label, onPress, tooltipKey)));
        }
    }

    private static class Option {
        private final Button button;
        private int x;
        private int y;
        private int width;

        private Option(Button button) {
            this.button = button;
        }
    }

    private record Layout(int left, int width, int contentHeight) {}

    private static Tooltip tooltip(String key) {
        return Tooltip.create(Component.translatable(key));
    }
}
