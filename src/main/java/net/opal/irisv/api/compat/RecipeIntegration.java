package net.opal.irisv.api.compat;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Client-only integration. Register during client setup, on the client thread. */
public interface RecipeIntegration {
    ResourceLocation id();
    Component title();
    boolean supportsScreen(AbstractContainerScreen<?> screen);

    /** Default: catalog only. Never infer machine behavior from slot positions. */
    default boolean supportsRecipe(AbstractContainerScreen<?> screen, RecipeHolder<?> recipe) { return false; }

    /** Read-only; called while rendering. Check inputs, capacity and server support. */
    default boolean canTransfer(AbstractContainerScreen<?> screen, RecipeHolder<?> recipe) { return false; }

    /** Use server-validated packets; never create items or mutate client inventory directly.
     * Return true only if a transfer request was issued. Revalidate before sending.
     * fillAll means the maximum supported quantity, not unlimited stacks.
     */
    default boolean transfer(AbstractContainerScreen<?> screen, RecipeHolder<?> recipe, boolean fillAll) { return false; }
}
