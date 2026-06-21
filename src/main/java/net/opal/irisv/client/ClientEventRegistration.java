package net.opal.irisv.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.opal.irisv.Irisv;
import net.opal.irisv.event.onBlockBreak;
import net.opal.irisv.indicators.IndicatorOverlayRenderer;
import net.opal.irisv.menu.MainMenu;
import net.opal.irisv.menu.MainMenuTitleOverlay;
import net.opal.irisv.menu.MenuPause;
import net.opal.irisv.network.ClientDataCache;
import net.opal.irisv.tooltips.TooltipManager;

public final class ClientEventRegistration {
    private ClientEventRegistration() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientEventRegistration::registerGuiLayers);

        NeoForge.EVENT_BUS.register(TooltipManager.class);
        NeoForge.EVENT_BUS.register(onBlockBreak.class);
        NeoForge.EVENT_BUS.register(ClientDataCache.class);
        NeoForge.EVENT_BUS.register(MenuPause.class);
        NeoForge.EVENT_BUS.register(MainMenu.class);
        NeoForge.EVENT_BUS.register(MainMenuTitleOverlay.class);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "indicators"),
                IndicatorOverlayRenderer::render
        );
    }
}
