package net.opal.irisv.api.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.opal.irisv.Irisv;
import java.util.*;
import java.util.function.Supplier;

/** Explicit, deterministic registration; no dependency on optional mod classes. Client-only. */
public final class IrisVCompatibility {
    private static final Map<ResourceLocation, RecipeIntegration> INTEGRATIONS = new LinkedHashMap<>();
    private static final Set<RecipeIntegration> FAILED = Collections.newSetFromMap(new IdentityHashMap<>());
    private IrisVCompatibility() {}

    public static void register(RecipeIntegration integration) {
        Objects.requireNonNull(integration);
        ResourceLocation id = Objects.requireNonNull(integration.id());
        Objects.requireNonNull(integration.title());
        if (INTEGRATIONS.putIfAbsent(id, integration) != null) throw new IllegalArgumentException("Duplicate IrisV integration: " + id);
    }

    public static List<RecipeIntegration> integrations() { return List.copyOf(INTEGRATIONS.values()); }
    public static boolean isFailed(RecipeIntegration integration) { return FAILED.contains(integration); }
    public static String settingKey(RecipeIntegration integration) { return "compat:" + integration.id(); }

    public static RecipeIntegration find(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> container)) return null;
        for (RecipeIntegration integration : INTEGRATIONS.values()) {
            if (call(integration, () -> integration.supportsScreen(container), false)) return integration;
        }
        return null;
    }

    /** Quarantine a faulty adapter for this session, without falling back to guessed transfers. */
    public static <T> T call(RecipeIntegration integration, Supplier<T> action, T fallback) {
        if (FAILED.contains(integration)) return fallback;
        try { return action.get(); }
        catch (RuntimeException exception) {
            FAILED.add(integration);
            Irisv.LOGGER.error("Disabling faulty IrisV integration {} for this session", integration.getClass().getName(), exception);
            return fallback;
        }
    }
}
