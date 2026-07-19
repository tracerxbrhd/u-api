package dev.uapi.api.profile;

import dev.uapi.UApi;
import dev.uapi.api.services.ServiceScope;
import dev.uapi.api.services.UApiServices;
import dev.uapi.api.social.SocialGroupService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/** Multi-provider, server-scoped registry for privacy-filtered optional profile sections. */
public final class ProfileFacetRegistry {
    public static final int MAXIMUM_FACETS = 32;
    public static final int MAXIMUM_PROVIDERS = 32;
    private static final Map<MinecraftServer, LinkedHashMap<ResourceLocation, ProfileFacetProvider>> PROVIDERS =
        new IdentityHashMap<>();

    private ProfileFacetRegistry() {
    }

    public static synchronized ProfileFacetRegistration register(
        MinecraftServer server,
        ProfileFacetProvider provider
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(provider, "provider");
        ResourceLocation providerId = Objects.requireNonNull(provider.providerId(), "providerId");
        LinkedHashMap<ResourceLocation, ProfileFacetProvider> providers =
            PROVIDERS.computeIfAbsent(server, ignored -> new LinkedHashMap<>());
        if (providers.putIfAbsent(providerId, provider) != null)
            throw new IllegalStateException("Profile facet provider is already registered: " + providerId);
        if (providers.size() > MAXIMUM_PROVIDERS) {
            providers.remove(providerId, provider);
            if (providers.isEmpty()) PROVIDERS.remove(server);
            throw new IllegalStateException("Profile facet provider limit reached for this server");
        }
        return new Registration(server, providerId, provider);
    }

    /** Returns a deterministic, bounded snapshot which is already safe to encode for the viewer. */
    public static List<ProfileFacet> query(ProfileFacetQuery query) {
        Objects.requireNonNull(query, "query");
        List<ProfileFacetProvider> providers;
        synchronized (ProfileFacetRegistry.class) {
            providers = List.copyOf(PROVIDERS.getOrDefault(query.server(), new LinkedHashMap<>()).values());
        }
        boolean sharedGroup = sharesSocialGroup(query.viewerId(), query.subjectId());
        List<ProfileFacet> result = new ArrayList<>();
        for (ProfileFacetProvider provider : providers) {
            List<ProfileFacet> supplied;
            try {
                supplied = List.copyOf(Objects.requireNonNull(provider.provide(query), "provider result"));
            } catch (RuntimeException exception) {
                UApi.LOGGER.error("Profile facet provider {} failed", provider.providerId(), exception);
                continue;
            }
            for (ProfileFacet facet : supplied) {
                if (facet == null || !visible(facet.audience(), query.selfView(), sharedGroup)) continue;
                result.add(facet);
                if (result.size() == MAXIMUM_PROVIDERS * MAXIMUM_FACETS) break;
            }
            if (result.size() == MAXIMUM_PROVIDERS * MAXIMUM_FACETS) break;
        }
        result.sort(Comparator.comparingInt(ProfileFacet::displayOrder)
            .thenComparing(facet -> facet.id().toString()));
        return List.copyOf(result.subList(0, Math.min(result.size(), MAXIMUM_FACETS)));
    }

    public static List<ProfileFacet> query(
        MinecraftServer server,
        UUID viewerId,
        UUID subjectId,
        ProfileFacetContext context
    ) {
        return query(new ProfileFacetQuery(server, viewerId, subjectId, context));
    }

    public static synchronized void clear(MinecraftServer server) {
        PROVIDERS.remove(Objects.requireNonNull(server, "server"));
    }

    private static boolean visible(ProfileFacetAudience audience, boolean self, boolean sharedGroup) {
        return switch (audience) {
            case PUBLIC -> true;
            case SHARED_SOCIAL_GROUP -> self || sharedGroup;
            case SUBJECT_ONLY -> self;
        };
    }

    private static boolean sharesSocialGroup(UUID viewerId, UUID subjectId) {
        if (viewerId.equals(subjectId)) return true;
        try {
            return UApiServices.find(SocialGroupService.class, ServiceScope.SERVER).map(service -> {
                Set<UUID> viewerGroups = service.getGroups(viewerId).stream()
                    .map(group -> group.groupId()).collect(Collectors.toUnmodifiableSet());
                return service.getGroups(subjectId).stream().anyMatch(group -> viewerGroups.contains(group.groupId()));
            }).orElse(false);
        } catch (RuntimeException exception) {
            UApi.LOGGER.error("Social group provider failed during profile audience filtering", exception);
            return false;
        }
    }

    private static final class Registration implements ProfileFacetRegistration {
        private final MinecraftServer server;
        private final ResourceLocation providerId;
        private final ProfileFacetProvider provider;
        private boolean active = true;

        private Registration(MinecraftServer server, ResourceLocation providerId, ProfileFacetProvider provider) {
            this.server = server;
            this.providerId = providerId;
            this.provider = provider;
        }

        @Override public ResourceLocation providerId() { return providerId; }

        @Override public synchronized boolean active() { return active; }

        @Override
        public synchronized void close() {
            if (!active) return;
            active = false;
            synchronized (ProfileFacetRegistry.class) {
                LinkedHashMap<ResourceLocation, ProfileFacetProvider> providers = PROVIDERS.get(server);
                if (providers == null) return;
                providers.remove(providerId, provider);
                if (providers.isEmpty()) PROVIDERS.remove(server);
            }
        }
    }
}
