package dev.uapi.server;

import dev.uapi.config.UApiServerConfig;
import dev.uapi.instance.InstanceManager;
import dev.uapi.instance.InstanceView;
import dev.uapi.reward.RewardReloadListener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class UApiServerEvents {
    private static int tickCounter;
    private UApiServerEvents() {}

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new RewardReloadListener());
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
        InstanceManager manager = InstanceManager.get(player.getServer());
        manager.findAssignedByPlayer(player.getUUID()).filter(manager::isFromPreviousServerRun)
            .ifPresent(instance -> recover(player, manager, instance));
    }

    private static void recover(ServerPlayer player, InstanceManager manager, InstanceView instance) {
        manager.returnPlayer(player, instance);
        manager.fail(instance.id(), "server_restart_recovery");
        player.sendSystemMessage(Component.translatable("message.u_api.recovered"));
    }
}
