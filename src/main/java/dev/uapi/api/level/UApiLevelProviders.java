package dev.uapi.api.level;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Neutral registry used by progression-aware ecosystem mods. */
public final class UApiLevelProviders {
    public static final Identifier VANILLA_EXPERIENCE =
        Identifier.fromNamespaceAndPath("u_api", "vanilla_experience");
    private static final Map<Identifier, PlayerLevelProvider> PROVIDERS = new ConcurrentHashMap<>();

    private UApiLevelProviders() {}

    public static void register(Identifier id, PlayerLevelProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        PlayerLevelProvider previous = PROVIDERS.putIfAbsent(id, provider);
        if (previous != null && previous != provider) {
            throw new IllegalStateException("Level provider already registered: " + id);
        }
    }

    public static Optional<PlayerLevelProvider> find(Identifier id) {
        Objects.requireNonNull(id, "id");
        if (VANILLA_EXPERIENCE.equals(id)) return Optional.of(player -> player.experienceLevel);
        return Optional.ofNullable(PROVIDERS.get(id));
    }

    public static int level(Identifier id, ServerPlayer player) {
        int value = find(id).orElseThrow(() -> new IllegalStateException("Unknown level provider: " + id))
            .level(Objects.requireNonNull(player, "player"));
        return Math.max(0, value);
    }
}
