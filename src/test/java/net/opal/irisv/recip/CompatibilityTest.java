package net.opal.irisv.recip;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.opal.irisv.api.compat.IrisVCompatibility;
import net.opal.irisv.api.compat.RecipeIntegration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityTest {
    private RecipeIntegration adapter(String name) {
        return new RecipeIntegration() {
            public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath("test", name); }
            public Component title() { return Component.literal(name); }
            public boolean supportsScreen(AbstractContainerScreen<?> screen) { return false; }
        };
    }

    @Test void catalogOnlyDefaultsDenyTransfers() {
        var integration = adapter("catalog");
        assertFalse(integration.supportsRecipe(null, null));
        assertFalse(integration.canTransfer(null, null));
        assertFalse(integration.transfer(null, null, true));
        assertEquals("compat:test:catalog", IrisVCompatibility.settingKey(integration));
    }

    @Test void rejectsDuplicateIdsAndReturnsImmutableSnapshot() {
        var integration = adapter("unique");
        IrisVCompatibility.register(integration);
        assertThrows(IllegalArgumentException.class, () -> IrisVCompatibility.register(adapter("unique")));
        assertThrows(UnsupportedOperationException.class, () -> IrisVCompatibility.integrations().clear());
    }

    @Test void faultyCallbackFailsClosedForTheSession() {
        var integration = adapter("faulty");
        assertFalse(IrisVCompatibility.isFailed(integration));
        assertFalse(IrisVCompatibility.call(integration, () -> { throw new IllegalStateException("test failure"); }, false));
        assertTrue(IrisVCompatibility.isFailed(integration));
        assertFalse(IrisVCompatibility.call(integration, () -> true, false));
        assertTrue(IrisVCompatibility.call(adapter("healthy"), () -> true, false));
    }
}
