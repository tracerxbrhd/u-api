package dev.uapi.api.network;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import net.minecraft.resources.Identifier;

/**
 * Thread-safe pending-request tracker shared by multiple connections.
 *
 * <p>Request IDs are unique across the tracker. Ack/error completion requires both the exact
 * connection ID and request ID, removes a request at most once, and rejects responses received at
 * or after the configured timeout.</p>
 */
public final class ConnectionRequestTracker {
    private final int maxPendingPerConnection;
    private final int maxPendingTotal;
    private final long timeoutNanos;
    private final LongSupplier nanoTime;
    private final Map<UUID, LinkedHashMap<RequestId, PendingRequest>> pendingByConnection =
        new LinkedHashMap<>();
    private final Map<RequestId, UUID> requestOwners = new LinkedHashMap<>();
    private boolean clockInitialized;
    private long lastObservedNanos;

    public ConnectionRequestTracker(int maxPendingPerConnection, int maxPendingTotal, Duration timeout) {
        this(maxPendingPerConnection, maxPendingTotal, timeout, System::nanoTime);
    }

    public ConnectionRequestTracker(int maxPendingPerConnection, int maxPendingTotal, Duration timeout,
                                    LongSupplier nanoTime) {
        if (maxPendingPerConnection < 1)
            throw new IllegalArgumentException("maxPendingPerConnection must be positive");
        if (maxPendingTotal < 1) throw new IllegalArgumentException("maxPendingTotal must be positive");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        this.maxPendingPerConnection = maxPendingPerConnection;
        this.maxPendingTotal = maxPendingTotal;
        this.timeoutNanos = timeout.toNanos();
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    public synchronized RegistrationStatus register(UUID connectionId, RequestId requestId,
                                                    Identifier actionId) {
        Objects.requireNonNull(connectionId, "connectionId");
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(actionId, "actionId");
        long now = readNow();
        cleanupTimedOut(now);
        if (requestOwners.containsKey(requestId)) return RegistrationStatus.DUPLICATE_REQUEST_ID;
        LinkedHashMap<RequestId, PendingRequest> connection = pendingByConnection.get(connectionId);
        if (connection != null && connection.size() >= maxPendingPerConnection)
            return RegistrationStatus.CONNECTION_LIMIT_REACHED;
        if (requestOwners.size() >= maxPendingTotal) return RegistrationStatus.TOTAL_LIMIT_REACHED;
        if (connection == null) {
            connection = new LinkedHashMap<>();
            pendingByConnection.put(connectionId, connection);
        }
        PendingRequest pending = new PendingRequest(requestId, actionId, now);
        connection.put(requestId, pending);
        requestOwners.put(requestId, connectionId);
        return RegistrationStatus.ACCEPTED;
    }

    public synchronized Optional<PendingRequest> complete(UUID connectionId, OperationAck acknowledgement) {
        Objects.requireNonNull(connectionId, "connectionId");
        Objects.requireNonNull(acknowledgement, "acknowledgement");
        return complete(connectionId, acknowledgement.requestId(), readNow());
    }

    public synchronized Optional<PendingRequest> complete(UUID connectionId, OperationError error) {
        Objects.requireNonNull(connectionId, "connectionId");
        Objects.requireNonNull(error, "error");
        return complete(connectionId, error.requestId(), readNow());
    }

    public synchronized List<TimedOutRequest> cleanupTimedOut() {
        return cleanupTimedOut(readNow());
    }

    public synchronized List<PendingRequest> clearConnection(UUID connectionId) {
        Objects.requireNonNull(connectionId, "connectionId");
        LinkedHashMap<RequestId, PendingRequest> removed = pendingByConnection.remove(connectionId);
        if (removed == null) return List.of();
        removed.keySet().forEach(requestOwners::remove);
        return List.copyOf(removed.values());
    }

    public synchronized void clear() {
        pendingByConnection.clear();
        requestOwners.clear();
    }

    public synchronized int pendingCount(UUID connectionId) {
        Objects.requireNonNull(connectionId, "connectionId");
        Map<RequestId, PendingRequest> pending = pendingByConnection.get(connectionId);
        return pending == null ? 0 : pending.size();
    }

    public synchronized int pendingCount() {
        return requestOwners.size();
    }

    private Optional<PendingRequest> complete(UUID connectionId, RequestId requestId, long now) {
        Objects.requireNonNull(connectionId, "connectionId");
        cleanupTimedOut(now);
        UUID owner = requestOwners.get(requestId);
        if (!connectionId.equals(owner)) return Optional.empty();
        LinkedHashMap<RequestId, PendingRequest> connection = pendingByConnection.get(connectionId);
        if (connection == null) return Optional.empty();
        PendingRequest removed = connection.remove(requestId);
        if (removed == null) return Optional.empty();
        requestOwners.remove(requestId);
        if (connection.isEmpty()) pendingByConnection.remove(connectionId);
        return Optional.of(removed);
    }

    private List<TimedOutRequest> cleanupTimedOut(long now) {
        List<TimedOutRequest> timedOut = new ArrayList<>();
        var connectionIterator = pendingByConnection.entrySet().iterator();
        while (connectionIterator.hasNext()) {
            Map.Entry<UUID, LinkedHashMap<RequestId, PendingRequest>> connectionEntry =
                connectionIterator.next();
            var requestIterator = connectionEntry.getValue().entrySet().iterator();
            while (requestIterator.hasNext()) {
                PendingRequest pending = requestIterator.next().getValue();
                if (!elapsedAtLeast(now, pending.createdAtNanos(), timeoutNanos)) continue;
                requestIterator.remove();
                requestOwners.remove(pending.requestId());
                timedOut.add(new TimedOutRequest(connectionEntry.getKey(), pending));
            }
            if (connectionEntry.getValue().isEmpty()) connectionIterator.remove();
        }
        return List.copyOf(timedOut);
    }

    private long readNow() {
        long now = nanoTime.getAsLong();
        if (clockInitialized && now < lastObservedNanos)
            throw new IllegalStateException("Request tracker clock moved backwards");
        clockInitialized = true;
        lastObservedNanos = now;
        return now;
    }

    private static boolean elapsedAtLeast(long now, long start, long duration) {
        return now >= start && now - start >= duration;
    }

    public enum RegistrationStatus {
        ACCEPTED,
        DUPLICATE_REQUEST_ID,
        CONNECTION_LIMIT_REACHED,
        TOTAL_LIMIT_REACHED
    }

    public record PendingRequest(RequestId requestId, Identifier actionId, long createdAtNanos) {
        public PendingRequest {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record TimedOutRequest(UUID connectionId, PendingRequest request) {
        public TimedOutRequest {
            Objects.requireNonNull(connectionId, "connectionId");
            Objects.requireNonNull(request, "request");
        }
    }
}
