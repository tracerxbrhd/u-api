package dev.uapi.integration;

import dev.uapi.UApi;
import dev.uapi.config.UApiCommonConfig;
import net.neoforged.fml.ModList;

import java.util.Set;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public final class IntegrationService {
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private IntegrationService() {}

    public static boolean isLoaded(String modId) {
        boolean loaded = ModList.get().isLoaded(modId);
        if (UApiCommonConfig.LOG_OPTIONAL_INTEGRATIONS.get() && LOGGED.add(modId))
            UApi.LOGGER.info("Optional integration {}: {}", modId, loaded ? "available" : "absent");
        return loaded;
    }

    public static boolean areLoaded(Collection<String> modIds) {
        return modIds.stream().allMatch(IntegrationService::isLoaded);
    }
}
