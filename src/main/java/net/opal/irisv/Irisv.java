package net.opal.irisv;

import net.opal.irisv.event.onBlockBreak;
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

    // File d'attente de tâches serveur
    public Irisv(IEventBus modEventBus) {
        ConfigOptions.load();

        // --- Bus d'Événements du MOD (modEventBus) ---
        // Utilisé pour l'initialisation, comme l'enregistrement des paquets
        modEventBus.register(NetworkHandler.class);

        // --- Bus d'Événements NEOFORGE (NeoForge.EVENT_BUS) ---
        // Utilisé pour les événements de jeu (ticks, rendus, clics)
        NeoForge.EVENT_BUS.register(TooltipManager.class);
        NeoForge.EVENT_BUS.addListener(ConfigReloader::register);

        // Enregistrement du sender pour le multijoueur
        NeoForge.EVENT_BUS.register(ServerDataSender.class);

        NeoForge.EVENT_BUS.register(onBlockBreak.class);


        NeoForge.EVENT_BUS.register(MenuPause.class);
        NeoForge.EVENT_BUS.register(MainMenu.class);
        NeoForge.EVENT_BUS.register(MainMenuTitleOverlay.class);
    }
}

