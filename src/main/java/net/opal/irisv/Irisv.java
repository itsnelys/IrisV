package net.opal.irisv;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.opal.irisv.event.onBlockBreak;
import net.opal.irisv.indicators.IndicatorOverlayRenderer;
import net.opal.irisv.menu.MainMenu;
import net.opal.irisv.menu.MainMenuTitleOverlay;
import net.opal.irisv.menu.MenuPause;
import net.opal.irisv.network.NetworkHandler;
import net.opal.irisv.network.ServerDataSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.option.ConfigReloader;
import net.opal.irisv.tooltips.TooltipManager;

@Mod(Irisv.MODID)
public class Irisv {
    public static final String MODID = "irisv";
    public static final Logger LOGGER = LogManager.getLogger();

    public Irisv(IEventBus modEventBus) {
        ConfigOptions.load();

        // --- Bus d'Événements du MOD (Initialisation) ---
        modEventBus.register(NetworkHandler.class);

        // IMPORTANT : Enregistrement des Overlays (HUD) sur le bus du MOD
        modEventBus.addListener(this::registerGuiLayers);

        // --- Bus d'Événements NEOFORGE (Gameplay) ---
        NeoForge.EVENT_BUS.register(TooltipManager.class);
        NeoForge.EVENT_BUS.addListener(ConfigReloader::register);
        NeoForge.EVENT_BUS.register(ServerDataSender.class);
        NeoForge.EVENT_BUS.register(onBlockBreak.class);

        // Menus et Overlays de menu
        NeoForge.EVENT_BUS.register(MenuPause.class);
        NeoForge.EVENT_BUS.register(MainMenu.class);
        NeoForge.EVENT_BUS.register(MainMenuTitleOverlay.class);
    }

    /**
     * Enregistre les couches de l'interface utilisateur (HUD)
     */
    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(MODID, "indicators"),
                (guiGraphics, deltaTracker) -> {
                    // Ici deltaTracker est bien du type DeltaTracker attendu par le nouveau renderer
                    IndicatorOverlayRenderer.render(guiGraphics, deltaTracker);
                }
        );
    }
}