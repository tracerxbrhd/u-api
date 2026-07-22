package dev.uapi.api.services;

import net.minecraft.resources.Identifier;

/**
 * Marker contract for a service published through {@link UApiServices}.
 *
 * <p>Implementations must return a stable identifier for their full registration lifetime.</p>
 */
public interface UApiService {
    Identifier serviceId();
}
