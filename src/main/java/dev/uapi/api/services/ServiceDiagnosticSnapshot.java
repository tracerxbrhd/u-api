package dev.uapi.api.services;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Immutable, point-in-time view of all active U-API service registrations. */
public record ServiceDiagnosticSnapshot(long revision, List<Entry> registrations) {
    public ServiceDiagnosticSnapshot {
        registrations = List.copyOf(Objects.requireNonNull(registrations, "registrations"));
    }

    public int totalRegistrations() {
        return registrations.size();
    }

    public long registrationsIn(ServiceScope scope) {
        Objects.requireNonNull(scope, "scope");
        return registrations.stream().filter(entry -> entry.scope() == scope).count();
    }

    /** Immutable diagnostic description which deliberately does not expose the service instance. */
    public record Entry(
        String contractType,
        Identifier serviceId,
        ServiceScope scope,
        String implementationType
    ) {
        public Entry {
            Objects.requireNonNull(contractType, "contractType");
            Objects.requireNonNull(serviceId, "serviceId");
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(implementationType, "implementationType");
        }
    }
}
