package net.opal.irisv;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.opal.irisv.client.ClientEventRegistration;
import net.opal.irisv.network.NetworkHandler;
import net.opal.irisv.network.ServerDataSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.option.ConfigReloader;

@Mod(Irisv.MODID)
public class Irisv {
    public static final String MODID = "irisv";
    public static final Logger LOGGER = LogManager.getLogger();

    public Irisv(IEventBus modEventBus) {
        ConfigOptions.load();

        modEventBus.register(NetworkHandler.class);
        NeoForge.EVENT_BUS.addListener(ConfigReloader::register);
        NeoForge.EVENT_BUS.register(ServerDataSender.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEventRegistration.register(modEventBus);
        }
    }
}
