package net.opal.irisv.recip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.opal.irisv.option.ConfigOptions;
import java.util.*;
import java.util.function.Predicate;

/** A session-only reference: never retain a recipe across world changes. */
public final class PinnedRecipeHud {
    private static RecipeBookmarks.Entry pinned;
    private static final List<Requirement> requirements = new ArrayList<>();
    private record Requirement(String key, Component name, Predicate<ItemStack> matches) {}
    private record Line(FormattedCharSequence text, int color) {}
    private static Object level;
    private static boolean visible = true;
    private PinnedRecipeHud() {}

    static void toggle(RecipeBookmarks.Entry entry) {
        if (isPinned(entry)) { unpin(); return; }
        pinned = entry;
        requirements.clear();
        if (entry.brewing() != null) {
            addStack(entry.brewing().input());
            addStack(entry.brewing().reagent());
        } else for (var ingredient : RecipeLookup.ingredients(entry.recipe())) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] alternatives = ingredient.getItems();
            String key = Arrays.stream(alternatives).map(PinnedRecipeHud::stackKey).sorted().reduce((a, b) -> a + ";" + b).orElse("unknown");
            Component name = alternatives.length == 0 ? Component.literal("?") : alternatives[0].getHoverName();
            if (alternatives.length > 1) name = Component.translatable("recip.irisv.pinned.alternatives", name);
            requirements.add(new Requirement(key, name, ingredient::test));
        }
        level = Minecraft.getInstance().level;
        visible = true;
    }

    static boolean isPinned(RecipeBookmarks.Entry entry) { return pinned != null && pinned.key().equals(entry.key()); }
    static boolean hasPinned() { return pinned != null && level == Minecraft.getInstance().level; }
    static void unpin() {
        pinned = null;
        requirements.clear();
        level = null;
        visible = true;
    }
    private static String stackKey(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getComponents();
    }

    private static void addStack(ItemStack stack) {
        requirements.add(new Requirement(stackKey(stack), stack.getHoverName(), value -> ItemStack.isSameItemSameComponents(value, stack)));
    }
    public static void toggleVisibility() { visible = !visible; }

    public static void render(GuiGraphics gui) {
        var mc = Minecraft.getInstance();
        if (mc.level != level) { pinned = null; requirements.clear(); level = null; }
        if (pinned == null || !visible || mc.screen != null || mc.options.hideGui || mc.player == null) return;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int maxWidth = Math.max(30, Math.min(210, sw - 24));
        List<Line> lines = new ArrayList<>();
        for (var line : mc.font.split(Component.translatable("recip.irisv.pinned.title", pinned.output().getHoverName()), maxWidth)) lines.add(new Line(line, 0xFFFFFFFF));
        boolean[] available = RecipeAvailability.availableIngredients(requirements.stream().map(Requirement::matches).toList(), mc.player.getInventory().items);
        Map<String, int[]> counts = new LinkedHashMap<>();
        Map<String, Component> names = new LinkedHashMap<>();
        for (int i = 0; i < requirements.size(); i++) {
            var requirement = requirements.get(i);
            int[] count = counts.computeIfAbsent(requirement.key, key -> new int[2]);
            count[1]++;
            if (available[i]) count[0]++;
            names.put(requirement.key, requirement.name);
        }
        for (var entry : counts.entrySet()) {
            int[] count = entry.getValue();
            Component text = names.get(entry.getKey()).copy().append(" " + count[0] + "/" + count[1]);
            for (var line : mc.font.split(text, maxWidth)) lines.add(new Line(line, count[0] == count[1] ? 0xFF8FDD91 : 0xFFFFBC84));
        }
        int rowHeight = mc.font.lineHeight + 3;
        int maxRows = Math.max(1, (sh - 60) / rowHeight);
        if (lines.size() > maxRows) {
            lines = new ArrayList<>(lines.subList(0, maxRows));
            lines.set(maxRows - 1, new Line(Component.literal("...").getVisualOrderText(), 0xFFBBBBBB));
        }
        int width = lines.stream().mapToInt(line -> mc.font.width(line.text)).max().orElse(0) + 12;
        int height = lines.size() * rowHeight + 9;
        int x = Math.max(4, sw - width - 8);
        var config = ConfigOptions.getInstance();
        boolean below = config.enableBlockTooltipOverlay && (config.tooltipPosition == ConfigOptions.TooltipPosition.TOP_RIGHT
                || config.tooltipPosition == ConfigOptions.TooltipPosition.TOP_CENTER);
        int y = below ? Math.max(8, sh - height - 40) : 8;
        gui.fill(x, y, x + width, y + height, 0xC8000000);
        for (int i = 0; i < lines.size(); i++) gui.drawString(mc.font, lines.get(i).text, x + 6, y + 5 + i * rowHeight, lines.get(i).color, false);
    }
}
