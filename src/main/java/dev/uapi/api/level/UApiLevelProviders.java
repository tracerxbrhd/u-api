package dev.uapi.api.level;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Neutral registry used by progression-aware ecosystem mods. */
public final class UApiLevelProviders {
    public static final ResourceLocation VANILLA_EXPERIENCE =
        ResourceLocation.fromNamespaceAndPath("u_api", "vanilla_experience");
    private static final Map<ResourceLocation, PlayerLevelProvider> PROVIDERS = new ConcurrentHashMap<>();

    private UApiLevelProviders() {}

    public static void register(ResourceLocation id, PlayerLevelProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        PlayerLevelProvider previous = PROVIDERS.putIfAbsent(id, provider);
        if (previous != null && previous != provider) {
            throw new IllegalStateException("Level provider already registered: " + id);
        }
    }

    public static Optional<PlayerLevelProvider> find(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        if (VANILLA_EXPERIENCE.equals(id)) return Optional.of(player -> player.experienceLevel);
        return Optional.ofNullable(PROVIDERS.get(id));
    }

    public static int level(ResourceLocation id, ServerPlayer player) {
        int value = find(id).orElseThrow(() -> new IllegalStateException("Unknown level provider: " + id))
            .level(Objects.requireNonNull(player, "player"));
        return Math.max(0, value);
    }
}
