package dev.uapi.internal.services;

import dev.uapi.api.services.ServiceDiagnosticSnapshot;
import dev.uapi.api.services.ServiceRegistration;
import dev.uapi.api.services.ServiceScope;
import dev.uapi.api.services.UApiService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.resources.Identifier;

/** Internal atomic storage behind the public service facade. */
public final class ServiceRegistryBackend {
    private static final ServiceRegistryBackend INSTANCE = new ServiceRegistryBackend();
    private static final Comparator<Entry> DIAGNOSTIC_ORDER = Comparator
        .comparing((Entry entry) -> entry.scope().ordinal())
        .thenComparing(entry -> entry.contract().getName())
        .thenComparing(entry -> entry.serviceId().toString());

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Class<? extends UApiService>, Entry> registrationsByContract = new HashMap<>();
    private final Map<Identifier, Entry> registrationsById = new HashMap<>();
    private long nextRegistrationId;
    private long revision;

    ServiceRegistryBackend() {
    }

    public static ServiceRegistryBackend instance() {
        return INSTANCE;
    }

    public <T extends UApiService> ServiceRegistration register(
        Class<T> contract,
        T service,
        ServiceScope scope
    ) {
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(scope, "scope");
        if (!contract.isInterface()) {
            throw new IllegalArgumentException("Service contract must be a public interface: " + contract.getName());
        }
        if (!contract.isInstance(service)) {
            throw new IllegalArgumentException(
                "Service implementation " + service.getClass().getName()
                    + " does not implement " + contract.getName()
            );
        }
        Identifier serviceId = Objects.requireNonNull(service.serviceId(), "service.serviceId()");

        ReentrantReadWriteLock.WriteLock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            if (this.registrationsByContract.containsKey(contract)) {
                throw new IllegalStateException("Service contract is already registered: " + contract.getName());
            }
            if (this.registrationsById.containsKey(serviceId)) {
                throw new IllegalStateException("Service ID is already registered: " + serviceId);
            }

            long registrationId = ++this.nextRegistrationId;
            Entry entry = new Entry(registrationId, contract, serviceId, scope, service);
            this.registrationsByContract.put(contract, entry);
            this.registrationsById.put(serviceId, entry);
            this.revision++;
            return new RegistrationHandle(this, entry);
        } finally {
            writeLock.unlock();
        }
    }

    public <T extends UApiService> Optional<T> find(Class<T> contract) {
        Objects.requireNonNull(contract, "contract");
        ReentrantReadWriteLock.ReadLock readLock = this.lock.readLock();
        readLock.lock();
        try {
            Entry entry = this.registrationsByContract.get(contract);
            return entry == null ? Optional.empty() : Optional.of(contract.cast(entry.service()));
        } finally {
            readLock.unlock();
        }
    }

    public <T extends UApiService> Optional<T> find(Class<T> contract, ServiceScope scope) {
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(scope, "scope");
        ReentrantReadWriteLock.ReadLock readLock = this.lock.readLock();
        readLock.lock();
        try {
            Entry entry = this.registrationsByContract.get(contract);
            return entry == null || entry.scope() != scope
                ? Optional.empty()
                : Optional.of(contract.cast(entry.service()));
        } finally {
            readLock.unlock();
        }
    }

    public int clearScope(ServiceScope scope) {
        Objects.requireNonNull(scope, "scope");
        ReentrantReadWriteLock.WriteLock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            List<Entry> removed = this.registrationsByContract.values().stream()
                .filter(entry -> entry.scope() == scope)
                .toList();
            for (Entry entry : removed) {
                this.registrationsByContract.remove(entry.contract(), entry);
                this.registrationsById.remove(entry.serviceId(), entry);
                this.revision++;
            }
            return removed.size();
        } finally {
            writeLock.unlock();
        }
    }

    public ServiceDiagnosticSnapshot diagnosticSnapshot() {
        ReentrantReadWriteLock.ReadLock readLock = this.lock.readLock();
        readLock.lock();
        try {
            List<Entry> entries = new ArrayList<>(this.registrationsByContract.values());
            entries.sort(DIAGNOSTIC_ORDER);
            List<ServiceDiagnosticSnapshot.Entry> diagnostics = entries.stream()
                .map(entry -> new ServiceDiagnosticSnapshot.Entry(
                    entry.contract().getName(),
                    entry.serviceId(),
                    entry.scope(),
                    entry.service().getClass().getName()
                ))
                .toList();
            return new ServiceDiagnosticSnapshot(this.revision, diagnostics);
        } finally {
            readLock.unlock();
        }
    }

    private boolean isActive(Entry expected) {
        ReentrantReadWriteLock.ReadLock readLock = this.lock.readLock();
        readLock.lock();
        try {
            Entry current = this.registrationsByContract.get(expected.contract());
            return current != null && current.registrationId() == expected.registrationId();
        } finally {
            readLock.unlock();
        }
    }

    private void unregister(Entry expected) {
        ReentrantReadWriteLock.WriteLock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            Entry current = this.registrationsByContract.get(expected.contract());
            if (current == null || current.registrationId() != expected.registrationId()) {
                return;
            }
            this.registrationsByContract.remove(expected.contract());
            this.registrationsById.remove(expected.serviceId(), expected);
            this.revision++;
        } finally {
            writeLock.unlock();
        }
    }

    private record Entry(
        long registrationId,
        Class<? extends UApiService> contract,
        Identifier serviceId,
        ServiceScope scope,
        UApiService service
    ) {
    }

    private static final class RegistrationHandle implements ServiceRegistration {
        private final ServiceRegistryBackend registry;
        private final Entry entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private RegistrationHandle(ServiceRegistryBackend registry, Entry entry) {
            this.registry = registry;
            this.entry = entry;
        }

        @Override
        public Class<? extends UApiService> contract() {
            return this.entry.contract();
        }

        @Override
        public Identifier serviceId() {
            return this.entry.serviceId();
        }

        @Override
        public ServiceScope scope() {
            return this.entry.scope();
        }

        @Override
        public boolean isActive() {
            return !this.closed.get() && this.registry.isActive(this.entry);
        }

        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) {
                this.registry.unregister(this.entry);
            }
        }
    }
}
