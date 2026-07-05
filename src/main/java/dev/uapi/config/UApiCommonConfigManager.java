package dev.uapi.config;

/** Safe common-config cache; defaults are available before NeoForge loads common.toml. */
public final class UApiCommonConfigManager {
    private static volatile boolean logOptionalIntegrations = true;
    private static volatile boolean loaded;

    private UApiCommonConfigManager() {}

    public static boolean logOptionalIntegrations() {
        return logOptionalIntegrations;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void reloadFromSpec() {
        logOptionalIntegrations = UApiCommonConfig.LOG_OPTIONAL_INTEGRATIONS.get();
        loaded = true;
    }
}
