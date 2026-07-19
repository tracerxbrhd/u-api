package dev.uapi.internal.services;

import dev.uapi.api.services.ServiceScope;

/** Internal lifecycle bridge used by loader event subscribers. */
public final class ServiceLifecycleCleanup {
    private ServiceLifecycleCleanup() {
    }

    public static int clearServerServices() {
        return ServiceRegistryBackend.instance().clearScope(ServiceScope.SERVER);
    }

    public static int clearClientConnectionServices() {
        return ServiceRegistryBackend.instance().clearScope(ServiceScope.CLIENT_CONNECTION);
    }
}
