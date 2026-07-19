package dev.uapi.internal.services;

import dev.uapi.UApi;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Removes server-scoped services after the server lifecycle ends. */
@EventBusSubscriber(modid = UApi.MOD_ID)
public final class ServiceLifecycleEvents {
    private ServiceLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ServiceLifecycleCleanup.clearServerServices();
    }
}
