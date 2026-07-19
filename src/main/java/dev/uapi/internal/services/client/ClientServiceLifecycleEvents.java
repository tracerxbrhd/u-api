package dev.uapi.internal.services.client;

import dev.uapi.UApi;
import dev.uapi.internal.services.ServiceLifecycleCleanup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Client-only cleanup for services tied to the active server connection. */
@EventBusSubscriber(modid = UApi.MOD_ID, value = Dist.CLIENT)
public final class ClientServiceLifecycleEvents {
    private ClientServiceLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ServiceLifecycleCleanup.clearClientConnectionServices();
    }
}
