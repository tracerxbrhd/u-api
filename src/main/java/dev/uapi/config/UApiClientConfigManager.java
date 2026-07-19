package dev.uapi.config;

/** Client config cache with usable defaults before client.toml is loaded. */
public final class UApiClientConfigManager {
    private static volatile boolean showRegisteredHud = true;
    private static volatile boolean showWorldOverlays = true;
    private static volatile int maximumWorldOverlays = 512;
    private static volatile boolean loaded;

    private UApiClientConfigManager() {
    }

    public static boolean showRegisteredHud() {
        return showRegisteredHud;
    }

    public static boolean showWorldOverlays() {
        return showWorldOverlays;
    }

    public static int maximumWorldOverlays() {
        return maximumWorldOverlays;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void reloadFromSpec() {
        showRegisteredHud = UApiClientConfig.SHOW_REGISTERED_HUD.get();
        showWorldOverlays = UApiClientConfig.SHOW_WORLD_OVERLAYS.get();
        maximumWorldOverlays = UApiClientConfig.MAX_WORLD_OVERLAYS.get();
        loaded = true;
    }
}
