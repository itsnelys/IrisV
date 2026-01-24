package net.opal.irisv;

import net.opal.irisv.menu.MainMenu;
import net.opal.irisv.menu.MainMenuTitleOverlay;
import net.opal.irisv.menu.MenuPause;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.option.ConfigReloader;
import net.opal.irisv.tooltips.TooltipOverlay;

@Mod(Irisv.MODID)
public class Irisv {
    public static final String MODID = "irisv";
    public static final Logger LOGGER = LogManager.getLogger();

    // File d'attente de tâches serveur
    public Irisv(IEventBus modEventBus) {
        // --- Enregistrement des Configs et Outils ---
        ConfigOptions.load();

        // --- Enregistrement des Bus d'événements ---
        NeoForge.EVENT_BUS.register(TooltipOverlay.class);
        NeoForge.EVENT_BUS.addListener(ConfigReloader::register);

        NeoForge.EVENT_BUS.register(MenuPause.class);
        NeoForge.EVENT_BUS.register(MainMenu.class);
        NeoForge.EVENT_BUS.register(MainMenuTitleOverlay.class);
    }
}