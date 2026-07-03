package dev.uapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UApiCommonConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LOG_OPTIONAL_INTEGRATIONS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Optional integration discovery shared by dependent modules.").push("integration");
        LOG_OPTIONAL_INTEGRATIONS = builder.comment(
            "Log whether supported optional mods are present during startup.",
            "Default: true. This only affects informational log messages.")
            .define("logOptionalIntegrations", true);
        builder.pop();
        SPEC = builder.build();
    }

    private UApiCommonConfig() {}
}
