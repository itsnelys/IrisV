package net.opal.irisv.recip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class RecipeTransfer {
    private record Move(int source, int target) {}
    private record Need(int target, Predicate<ItemStack> matches) {}
    private RecipeTransfer() {}

    static boolean canTransfer(Screen screen, RecipeBookmarks.Entry entry) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || !(screen instanceof AbstractContainerScreen<?> container)
                || container.getMenu() != mc.player.containerMenu || !container.getMenu().getCarried().isEmpty()
                || !container.getMenu().stillValid(mc.player)) return false;
        var integration = net.opal.irisv.api.compat.IrisVCompatibility.find(screen);
        if (integration != null) return entry.recipe() != null && net.opal.irisv.api.compat.IrisVCompatibility.call(integration,
                () -> integration.supportsRecipe(container, entry.recipe()) && integration.canTransfer(container, entry.recipe()), false);
        if (!screen.getClass().getPackageName().equals("net.minecraft.client.gui.screens.inventory")) return false;
        if (!RecipeAvailability.ingredientsAvailable(screen, entry)) return false;
        return container.getMenu() instanceof RecipeBookMenu<?, ?> || plan(container.getMenu(), entry) != null;
    }

    static boolean transfer(Screen screen, RecipeBookmarks.Entry entry, boolean fillAll) {
        if (!canTransfer(screen, entry)) return false;
        var mc = Minecraft.getInstance();
        var menu = ((AbstractContainerScreen<?>) screen).getMenu();
        var integration = net.opal.irisv.api.compat.IrisVCompatibility.find(screen);
        if (integration != null) return net.opal.irisv.api.compat.IrisVCompatibility.call(integration,
                () -> integration.transfer((AbstractContainerScreen<?>) screen, entry.recipe(), fillAll), false);
        if (menu instanceof RecipeBookMenu<?, ?>) {
            mc.gameMode.handlePlaceRecipe(menu.containerId, entry.recipe(), fillAll && menu instanceof CraftingMenu);
            return true;
        }
        List<Move> moves = plan(menu, entry);
        if (moves == null) return false;
        for (Move move : moves) {
            // Vanilla clicks preserve server authority and return the unused stack to its source.
            mc.gameMode.handleInventoryMouseClick(menu.containerId, move.source, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(menu.containerId, move.target, 1, ClickType.PICKUP, mc.player);
            if (!menu.getCarried().isEmpty()) mc.gameMode.handleInventoryMouseClick(menu.containerId, move.source, 0, ClickType.PICKUP, mc.player);
        }
        if (menu instanceof StonecutterMenu stonecutter && entry.recipe() != null) {
            for (int i = 0; i < stonecutter.getRecipes().size(); i++) {
                if (stonecutter.getRecipes().get(i).id().equals(entry.recipe().id())) {
                    stonecutter.clickMenuButton(mc.player, i);
                    mc.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    break;
                }
            }
        }
        return true;
    }

    private static List<Move> plan(AbstractContainerMenu menu, RecipeBookmarks.Entry entry) {
        List<Need> needs = new ArrayList<>();
        if (entry.brewing() != null) {
            int bottle = -1;
            for (int i = 0; i < 3; i++) if (ItemStack.isSameItemSameComponents(menu.getSlot(i).getItem(), entry.brewing().input())) { bottle = i; break; }
            if (bottle < 0) for (int i = 0; i < 3; i++) if (!menu.getSlot(i).hasItem()) { bottle = i; break; }
            if (bottle < 0) return null;
            needs.add(new Need(bottle, stack -> ItemStack.isSameItemSameComponents(stack, entry.brewing().input())));
            needs.add(new Need(3, stack -> ItemStack.isSameItemSameComponents(stack, entry.brewing().reagent())));
        } else {
            var ingredients = RecipeLookup.ingredients(entry.recipe());
            for (int i = 0; i < ingredients.size(); i++) {
                var ingredient = ingredients.get(i);
                if (!ingredient.isEmpty()) needs.add(new Need(i, ingredient::test));
            }
        }
        List<Need> missing = new ArrayList<>();
        for (Need need : needs) {
            var slot = menu.getSlot(need.target);
            if (slot.hasItem()) {
                if (!need.matches.test(slot.getItem())) return null;
            } else missing.add(need);
        }
        List<Integer> sources = new ArrayList<>();
        var player = Minecraft.getInstance().player;
        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.getSlot(i);
            if (slot.container != player.getInventory() || !slot.mayPickup(player)) continue;
            for (int n = 0; n < Math.min(slot.getItem().getCount(), missing.size()); n++) sources.add(i);
        }
        int[] owner = new int[sources.size()];
        java.util.Arrays.fill(owner, -1);
        for (int n = 0; n < missing.size(); n++) if (!assign(n, missing, sources, menu, owner, new boolean[sources.size()])) return null;
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < owner.length; i++) if (owner[i] >= 0) moves.add(new Move(sources.get(i), missing.get(owner[i]).target));
        return moves;
    }

    private static boolean assign(int need, List<Need> needs, List<Integer> sources, AbstractContainerMenu menu, int[] owner, boolean[] seen) {
        Need target = needs.get(need);
        for (int i = 0; i < sources.size(); i++) {
            ItemStack stack = menu.getSlot(sources.get(i)).getItem();
            if (seen[i] || !target.matches.test(stack) || !menu.getSlot(target.target).mayPlace(stack)) continue;
            seen[i] = true;
            if (owner[i] < 0 || assign(owner[i], needs, sources, menu, owner, seen)) { owner[i] = need; return true; }
        }
        return false;
    }
}
