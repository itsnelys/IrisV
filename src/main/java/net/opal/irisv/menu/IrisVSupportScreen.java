package net.opal.irisv.menu;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.opal.irisv.api.compat.IrisVCompatibility;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.theme.UiTheme;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class IrisVSupportScreen extends Screen {
    private final Screen parent;
    private Component status = Component.empty();
    private int scroll;
    private int maxScroll;

    public IrisVSupportScreen(Screen parent) {
        super(Component.translatable("irisv.support.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        int w = panelWidth();
        int x = (width - w) / 2;
        int half = (w - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("irisv.settings.export"), button -> {
            if (Files.exists(ConfigOptions.exchangePath())) confirm("irisv.settings.overwrite", this::exportFile);
            else exportFile();
        }).bounds(x, 42, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("irisv.settings.import"), button ->
                confirm("irisv.settings.confirm", this::importFile)).bounds(x + half + 4, 42, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("irisv.settings.folder"), button -> {
            try {
                Files.createDirectories(ConfigOptions.exchangePath().getParent());
                Util.getPlatform().openFile(ConfigOptions.exchangePath().getParent().toFile());
            } catch (Exception exception) { failure(exception); }
        }).bounds(x, 66, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.irisv.return"), button -> onClose())
                .bounds(x, height - 30, w, 20).build());
    }

    private void confirm(String key, Runnable action) {
        minecraft.setScreen(new ConfirmScreen(accepted -> {
            minecraft.setScreen(this);
            if (accepted) action.run();
        }, title, Component.translatable(key)));
    }

    private void exportFile() {
        try {
            ConfigOptions.getInstance().exportSettingsFile();
            status = Component.translatable("irisv.settings.exported").withStyle(ChatFormatting.GREEN);
            net.opal.irisv.commun.utils.FunctionUtilsChat.clientAction(minecraft.player, "irisv.settings.exported");
            scroll = 0;
        } catch (Exception exception) { failure(exception); }
    }

    private void importFile() {
        try {
            ConfigOptions.importSettingsFile();
            status = Component.translatable("irisv.settings.imported").withStyle(ChatFormatting.GREEN);
            net.opal.irisv.commun.utils.FunctionUtilsChat.clientAction(minecraft.player, "irisv.settings.imported");
            scroll = 0;
        } catch (Exception exception) { failure(exception); }
    }

    private void failure(Exception exception) {
        net.opal.irisv.commun.utils.FunctionUtilsLogs.errorLog("Settings", "Settings operation failed", exception);
        net.opal.irisv.commun.utils.FunctionUtilsChat.clientError(minecraft.player, "irisv.settings.failed");
        status = Component.translatable("irisv.settings.failed").withStyle(ChatFormatting.RED);
        scroll = 0;
    }

    private List<Component> lines() {
        List<Component> lines = new ArrayList<>();
        if (!status.getString().isEmpty()) lines.add(status);
        lines.add(Component.translatable("irisv.settings.file").withStyle(ChatFormatting.WHITE));
        lines.add(Component.literal(ConfigOptions.exchangePath().toAbsolutePath().toString()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("irisv.settings.scope").withStyle(ChatFormatting.GRAY));
        lines.add(Component.empty());
        lines.add(Component.translatable("irisv.compat.title").withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable("irisv.compat.vanilla").withStyle(ChatFormatting.GRAY));
        ConfigOptions config = ConfigOptions.getInstance();
        if (!config.enableRecipeOverlay) lines.add(Component.translatable("irisv.compat.overlay_off").withStyle(ChatFormatting.YELLOW));
        for (var integration : IrisVCompatibility.integrations()) {
            Component name = IrisVCompatibility.call(integration, integration::title,
                    Component.literal(integration.getClass().getSimpleName()));
            boolean failed = IrisVCompatibility.isFailed(integration);
            boolean disabled = !config.enableRecipeOverlay || config.disabledRecipeHudCategories.contains(IrisVCompatibility.settingKey(integration));
            String state = failed ? "failed" : disabled ? "disabled" : "registered";
            lines.add(Component.empty().append(name).append(" - ")
                    .append(Component.translatable("irisv.compat." + state))
                    .withStyle(failed ? ChatFormatting.RED : disabled ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
            lines.add(Component.literal(IrisVCompatibility.settingKey(integration).substring(7)).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (IrisVCompatibility.integrations().isEmpty()) {
            lines.add(Component.translatable("irisv.compat.none").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.empty());
        lines.add(Component.translatable("irisv.compat.limits").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("irisv.compat.transfer").withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        renderBackground(gui, mouseX, mouseY, delta);
        UiTheme theme = UiTheme.getCurrent();
        gui.fill(0, 0, width, height, theme.gui_bgOverlay());
        gui.fill(0, 0, width, 35, theme.gui_barColor());
        gui.fill(0, 34, width, 35, theme.gui_lineColor());
        gui.fill(0, height - 40, width, height, theme.gui_barColor());
        gui.fill(0, height - 40, width, height - 39, theme.gui_lineColor());
        gui.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int top = 96, bottom = height - 48, w = panelWidth(), x = (width - w) / 2;
        var wrapped = lines().stream().flatMap(line -> font.split(line, w - 8).stream()).toList();
        int visible = Math.max(1, bottom - top);
        maxScroll = Math.max(0, wrapped.size() * 13 - visible);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        if (bottom > top) {
            gui.enableScissor(x, top, x + w, bottom);
            int y = top - scroll;
            for (var line : wrapped) {
                gui.drawString(font, line, x, y, 0xFFFFFF, false);
                y += 13;
            }
            gui.disableScissor();
            if (maxScroll > 0) {
                int thumb = Math.min(visible, Math.max(8, visible * visible / (wrapped.size() * 13)));
                int thumbY = top + (visible - thumb) * scroll / maxScroll;
                gui.fill(x + w + 3, top, x + w + 5, bottom, theme.gui_separatorLine());
                gui.fill(x + w + 3, thumbY, x + w + 5, thumbY + thumb, theme.gui_lineColor());
            }
        }
        for (var widget : renderables) widget.render(gui, mouseX, mouseY, delta);
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Mth.clamp(scroll - (int) (dy * 20), 0, maxScroll);
        return true;
    }

    private int panelWidth() { return Math.max(80, Math.min(360, width - 36)); }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
