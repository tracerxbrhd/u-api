package dev.uapi.internal.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.uapi.api.services.ServiceDiagnosticSnapshot;
import dev.uapi.api.services.ServiceRegistration;
import dev.uapi.api.services.ServiceScope;
import dev.uapi.api.services.UApiService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

final class ServiceRegistryBackendTest {
    @Test
    void registersAndFindsByExplicitContractAndScope() {
        ServiceRegistryBackend registry = new ServiceRegistryBackend();
        PrimaryService implementation = new PrimaryImplementation(id("primary"));

        try (ServiceRegistration registration = registry.register(
            PrimaryService.class, implementation, ServiceScope.SERVER
        )) {
            assertSame(implementation, registry.find(PrimaryService.class).orElseThrow());
            assertSame(implementation,
                registry.find(PrimaryService.class, ServiceScope.SERVER).orElseThrow());
            assertTrue(registry.find(PrimaryService.class, ServiceScope.GLOBAL).isEmpty());
            assertTrue(registration.isActive());
        }

        assertTrue(registry.find(PrimaryService.class).isEmpty());
    }

    @Test
    void rejectsDuplicateContractsAndDuplicateServiceIds() {
        ServiceRegistryBackend registry = new ServiceRegistryBackend();
        ServiceRegistration first = registry.register(
            PrimaryService.class, new PrimaryImplementation(id("shared")), ServiceScope.GLOBAL
        );
        try {
            assertThrows(IllegalStateException.class, () -> registry.register(
                PrimaryService.class, new PrimaryImplementation(id("other")), ServiceScope.SERVER
            ));
            assertThrows(IllegalStateException.class, () -> registry.register(
                SecondaryService.class, new SecondaryImplementation(id("shared")), ServiceScope.SERVER
            ));
        } finally {
            first.close();
        }
    }

    @Test
    void rejectsConcreteImplementationAsThePublishedContract() {
        ServiceRegistryBackend registry = new ServiceRegistryBackend();
        PrimaryImplementation implementation = new PrimaryImplementation(id("concrete"));

        assertThrows(IllegalArgumentException.class, () -> registry.register(
            PrimaryImplementation.class, implementation, ServiceScope.GLOBAL
        ));
    }

    @Test
    void lifecycleCleanupOnlyRemovesItsOwnScope() {
        ServiceRegistryBackend registry = new ServiceRegistryBackend();
        ServiceRegistration global = registry.register(
            PrimaryService.class, new PrimaryImplementation(id("global")), ServiceScope.GLOBAL
        );
        ServiceRegistration server = registry.register(
            SecondaryService.class, new SecondaryImplementation(id("server")), ServiceScope.SERVER
        );

        assertEquals(1, registry.clearScope(ServiceScope.SERVER));
        assertTrue(global.isActive());
        assertFalse(server.isActive());
        assertTrue(registry.find(PrimaryService.class).isPresent());
        assertTrue(registry.find(SecondaryService.class).isEmpty());
        global.close();
    }

    @Test
    void oldHandleCannotCloseAReplacementRegistration() {
        ServiceRegistryBackend registry = new ServiceRegistryBackend();
        ServiceRegistration old = registry.register(
            PrimaryService.class, new PrimaryImplementation(id("reused")), ServiceScope.SERVER
        );
        registry.clearScope(ServiceScope.SERVER);

        PrimaryService replacement = new PrimaryImplementation(id("reused"));
        ServiceRegistration current = registry.register(
            PrimaryService.class, replacement, ServiceScope.SERVER
        );
        old.close();

        assertTrue(current.isActive());
        assertSame(replacement, registry.find(PrimaryService.class).orElseThrow());
        current.close();
    }

    @Test
    void diagnosticSnapshotsAreImmutableAndPointInTime() {
        ServiceRegistryBackend registry = new ServiceRegistryBackend();
        ServiceRegistration registration = registry.register(
            PrimaryService.class, new PrimaryImplementation(id("diagnostic")), ServiceScope.CLIENT_CONNECTION
        );
        ServiceDiagnosticSnapshot snapshot = registry.diagnosticSnapshot();

        assertEquals(1, snapshot.totalRegistrations());
        assertEquals(1, snapshot.registrationsIn(ServiceScope.CLIENT_CONNECTION));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.registrations().clear());

        registration.close();
        assertEquals(1, snapshot.totalRegistrations());
        assertEquals(0, registry.diagnosticSnapshot().totalRegistrations());
        assertTrue(registry.diagnosticSnapshot().revision() > snapshot.revision());
    }

    @Test
    void concurrentDuplicateRegistrationHasExactlyOneWinner() throws Exception {
        ServiceRegistryBackend registry = new ServiceRegistryBackend();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ServiceRegistration>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < 16; index++) {
                int candidate = index;
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        return registry.register(
                            PrimaryService.class,
                            new PrimaryImplementation(id("candidate_" + candidate)),
                            ServiceScope.GLOBAL
                        );
                    } catch (IllegalStateException expected) {
                        return null;
                    }
                }));
            }
            start.countDown();

            List<ServiceRegistration> winners = new ArrayList<>();
            for (Future<ServiceRegistration> future : futures) {
                ServiceRegistration registration = future.get();
                if (registration != null) winners.add(registration);
            }
            assertEquals(1, winners.size());
            assertEquals(1, registry.diagnosticSnapshot().totalRegistrations());
            winners.getFirst().close();
        } finally {
            executor.shutdownNow();
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("u_api_test", path);
    }

    private interface PrimaryService extends UApiService {
    }

    private interface SecondaryService extends UApiService {
    }

    private record PrimaryImplementation(Identifier serviceId) implements PrimaryService {
    }

    private record SecondaryImplementation(Identifier serviceId) implements SecondaryService {
    }
}
