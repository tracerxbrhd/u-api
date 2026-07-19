package dev.uapi.config;

import dev.uapi.api.diagnostics.UApiDiagnostics;

/** Safe common-config cache; defaults are available before NeoForge loads common.toml. */
public final class UApiCommonConfigManager {
    private static volatile boolean logOptionalIntegrations = true;
    private static volatile boolean loaded;
    private static volatile boolean diagnosticsEnabled;
    private static volatile boolean optionalIntegrationsEnabled = true;
    private static volatile boolean debugLogging;

    private UApiCommonConfigManager() {}

    public static boolean logOptionalIntegrations() {
        return logOptionalIntegrations;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean diagnosticsEnabled() {
        return diagnosticsEnabled;
    }

    public static boolean optionalIntegrationsEnabled() { return optionalIntegrationsEnabled; }

    public static boolean debugLogging() { return debugLogging; }

    public static void reloadFromSpec() {
        logOptionalIntegrations = UApiCommonConfig.LOG_OPTIONAL_INTEGRATIONS.get();
        diagnosticsEnabled = UApiCommonConfig.ENABLE_DIAGNOSTICS.get();
        optionalIntegrationsEnabled = UApiCommonConfig.ENABLE_OPTIONAL_INTEGRATIONS.get();
        debugLogging = UApiCommonConfig.DEBUG_LOGGING.get();
        UApiDiagnostics.setEnabled(diagnosticsEnabled);
        loaded = true;
    }
}
