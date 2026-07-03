package dev.uapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UApiClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_TIMER_NOTIFICATIONS;
    public static final ModConfigSpec.BooleanValue ENABLE_PORTAL_PARTICLES;

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
        builder.pop();
        SPEC = builder.build();
    }

    private UApiClientConfig() {}
}
