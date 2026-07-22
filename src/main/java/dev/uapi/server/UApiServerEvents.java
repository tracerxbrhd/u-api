package dev.uapi.server;

import dev.uapi.config.UApiServerConfig;
import dev.uapi.instance.InstanceManager;
import dev.uapi.instance.InstanceView;
import dev.uapi.reward.RewardReloadListener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class UApiServerEvents {
    private static int tickCounter;
    private UApiServerEvents() {}

    @SubscribeEvent
    public static void addReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath("u_api", "rewards"), new RewardReloadListener());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < UApiServerConfig.TIMER_CHECK_INTERVAL_TICKS.get()) return;
        tickCounter = 0;
        InstanceManager.get(event.getServer()).tick(System.currentTimeMillis());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !UApiServerConfig.RECOVER_PLAYERS_AFTER_RESTART.get()) return;
        InstanceManager manager = InstanceManager.get(player.level().getServer());
        manager.findAssignedByPlayer(player.getUUID()).filter(manager::isFromPreviousServerRun)
            .ifPresent(instance -> recover(player, manager, instance));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerStopping(ServerStoppingEvent event) {
        releaseServer(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerStopped(ServerStoppedEvent event) {
        // Idempotent final cleanup covers managers recreated by another mod's late stop callback.
        releaseServer(event.getServer());
    }

    private static void releaseServer(net.minecraft.server.MinecraftServer server) {
        InstanceManager.stop(server);
        tickCounter = 0;
    }

    private static void recover(ServerPlayer player, InstanceManager manager, InstanceView instance) {
        manager.returnPlayer(player, instance);
        manager.fail(instance.id(), "server_restart_recovery");
        player.sendSystemMessage(Component.translatable("message.u_api.recovered"));
    }
}
