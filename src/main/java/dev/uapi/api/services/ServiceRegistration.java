package dev.uapi.api.services;

import net.minecraft.resources.Identifier;

/**
 * Ownership handle for exactly one service registration.
 *
 * <p>Closing an old handle never removes a newer registration which reused the same contract or
 * service ID after lifecycle cleanup. Closing a handle more than once is safe.</p>
 */
public interface ServiceRegistration extends AutoCloseable {
    Class<? extends UApiService> contract();

    Identifier serviceId();

    ServiceScope scope();

    boolean isActive();

    @Override
    void close();
}
