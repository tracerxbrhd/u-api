package dev.uapi.api.profile;

import net.minecraft.resources.ResourceLocation;

/** Idempotent lifecycle handle for a server-scoped profile facet provider. */
public interface ProfileFacetRegistration extends AutoCloseable {
    ResourceLocation providerId();

    boolean active();

    @Override
    void close();
}
