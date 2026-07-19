package dev.uapi.api.profile;

import dev.uapi.UApi;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Last-resort lifecycle cleanup for provider handles which an addon forgot to close. */
@EventBusSubscriber(modid = UApi.MOD_ID)
public final class ProfileFacetLifecycleEvents {
    private ProfileFacetLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ProfileFacetRegistry.clear(event.getServer());
    }
}
