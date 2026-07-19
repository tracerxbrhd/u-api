package dev.uapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UApiClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_TIMER_NOTIFICATIONS;
    public static final ModConfigSpec.BooleanValue ENABLE_PORTAL_PARTICLES;
    public static final ModConfigSpec.BooleanValue SHOW_REGISTERED_HUD;
    public static final ModConfigSpec.BooleanValue SHOW_WORLD_OVERLAYS;
    public static final ModConfigSpec.IntValue MAX_WORLD_OVERLAYS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Client-only visual preferences. They do not affect server gameplay.").push("ui");
        SHOW_TIMER_NOTIFICATIONS = builder.comment(
            "Show client notifications for instance and portal timers.",
            "Default: true.")
            .define("showTimerNotifications", true);
        ENABLE_PORTAL_PARTICLES = builder.comment(
            "Render portal particles supplied by U-API modules.",
            "Default: true. Disable to reduce visual clutter.")
            .define("enablePortalParticles", true);
        SHOW_REGISTERED_HUD = builder.comment(
            "Render HUD elements registered through U-API.",
            "Individual elements can still be hidden by their placement preference.")
            .define("showRegisteredHud", true);
        SHOW_WORLD_OVERLAYS = builder.comment(
            "Render world-anchored U-API markers such as Party pings and objectives.")
            .define("showWorldOverlays", true);
        MAX_WORLD_OVERLAYS = builder.comment(
            "Maximum number of connection-scoped world overlay markers retained by the client.",
            "The bound remains active even while overlay rendering is hidden.")
            .defineInRange("maxWorldOverlays", 512, 16, 4096);
        builder.pop();
        SPEC = builder.build();
    }

    private UApiClientConfig() {}
}
