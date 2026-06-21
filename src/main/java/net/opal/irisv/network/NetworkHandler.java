package net.opal.irisv.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.opal.irisv.Irisv;

public class NetworkHandler {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        // On crée le registrar pour le mod
        final PayloadRegistrar registrar = event.registrar(Irisv.MODID).versioned("1");

        registrar.playToClient(
                BlockDataPayload.TYPE,
                BlockDataPayload.CODEC,
                ClientDataCache::handleData
        );
    }
}
