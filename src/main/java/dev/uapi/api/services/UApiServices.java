package dev.uapi.api.services;

import dev.uapi.internal.services.ServiceRegistryBackend;
import java.util.Optional;

/** Public typed facade for registering and discovering U-API services. */
public final class UApiServices {
    private UApiServices() {
    }

    /**
     * Registers {@code service} under the explicitly supplied public contract.
     *
     * @throws IllegalArgumentException if the implementation does not implement the contract
     * @throws IllegalStateException if the contract or the service ID is already registered
     */
    public static <T extends UApiService> ServiceRegistration register(
        Class<T> contract,
        T service,
        ServiceScope scope
    ) {
        return ServiceRegistryBackend.instance().register(contract, service, scope);
    }

    /** Finds the active implementation registered for the exact contract. */
    public static <T extends UApiService> Optional<T> find(Class<T> contract) {
        return ServiceRegistryBackend.instance().find(contract);
    }

    /** Finds the active implementation only when it belongs to {@code scope}. */
    public static <T extends UApiService> Optional<T> find(Class<T> contract, ServiceScope scope) {
        return ServiceRegistryBackend.instance().find(contract, scope);
    }

    /** Returns an immutable point-in-time diagnostic view. */
    public static ServiceDiagnosticSnapshot diagnosticSnapshot() {
        return ServiceRegistryBackend.instance().diagnosticSnapshot();
    }
}
