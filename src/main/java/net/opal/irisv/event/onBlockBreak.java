package net.opal.irisv.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.opal.irisv.network.ClientDataCache;

public class onBlockBreak {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Note : j'ai retiré le 'static' car on utilise 'this' dans le register
        if (event.getLevel().isClientSide()) {
            ClientDataCache.remove(event.getPos());
        }
    }
}
