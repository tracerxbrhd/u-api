package dev.uapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UApiCommonConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LOG_OPTIONAL_INTEGRATIONS;
    public static final ModConfigSpec.BooleanValue ENABLE_OPTIONAL_INTEGRATIONS;
    public static final ModConfigSpec.BooleanValue ENABLE_DIAGNOSTICS;
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Optional integration discovery shared by dependent modules.").push("integration");
        LOG_OPTIONAL_INTEGRATIONS = builder.comment(
            "Log whether supported optional mods are present during startup.",
            "Default: true. This only affects informational log messages.")
            .define("logOptionalIntegrations", true);
        ENABLE_OPTIONAL_INTEGRATIONS = builder.comment(
            "Allow U-API's built-in optional adapters to initialize. Addon registrations remain independent.")
            .define("enabled", true);
        builder.pop();
        builder.comment("Disabled-by-default instrumentation shared by U-API modules.").push("diagnostics");
        ENABLE_DIAGNOSTICS = builder.comment(
            "Collect UI, overlay, service, cache and packet diagnostics.",
            "Default: false. Timing reads are skipped entirely while disabled.")
            .define("enabled", false);
        DEBUG_LOGGING = builder.comment("Emit additional bounded U-API debug diagnostics.")
            .define("debugLogging", false);
        builder.pop();
        SPEC = builder.build();
    }

    private UApiCommonConfig() {}
}
