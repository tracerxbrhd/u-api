package dev.uapi.api.profile;

import net.minecraft.resources.Identifier;

/** Idempotent lifecycle handle for a server-scoped profile facet provider. */
public interface ProfileFacetRegistration extends AutoCloseable {
    Identifier providerId();

    boolean active();

    @Override
    void close();
}
