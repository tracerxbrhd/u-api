package dev.uapi.internal.permissions;

import dev.uapi.api.permissions.PermissionContext;
import dev.uapi.api.permissions.PermissionDecision;
import dev.uapi.api.permissions.PermissionKey;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Small synchronized LRU/TTL cache for server-side permission decisions. */
final class BoundedPermissionDecisionCache {
    private final int maximumEntries;
    private final long timeToLiveNanos;
    private final LongSupplier nanoClock;
    private final LinkedHashMap<Key, Value> values = new LinkedHashMap<>(16, 0.75F, true);
    private long hits;
    private long misses;
    private long evictions;
    private long revision;
    private long invalidationRevision;

    BoundedPermissionDecisionCache(int maximumEntries, Duration timeToLive) {
        this(maximumEntries, timeToLive, System::nanoTime);
    }

    BoundedPermissionDecisionCache(int maximumEntries, Duration timeToLive, LongSupplier nanoClock) {
        if (maximumEntries <= 0) {
            throw new IllegalArgumentException("maximumEntries must be positive");
        }
        Objects.requireNonNull(timeToLive, "timeToLive");
        long nanos = timeToLive.toNanos();
        if (nanos <= 0) {
            throw new IllegalArgumentException("timeToLive must be positive");
        }
        this.maximumEntries = maximumEntries;
        this.timeToLiveNanos = nanos;
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
    }

    PermissionDecision getOrCompute(
        PermissionKey permission,
        PermissionContext context,
        Supplier<PermissionDecision> resolver
    ) {
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(resolver, "resolver");
        Key key = new Key(permission, context);
        long now = this.nanoClock.getAsLong();
        long observedInvalidationRevision;

        synchronized (this) {
            Value cached = this.values.get(key);
            if (cached != null && !isExpired(cached, now)) {
                this.hits++;
                return cached.decision();
            }
            if (cached != null) {
                this.values.remove(key);
                this.revision++;
            }
            this.misses++;
            observedInvalidationRevision = this.invalidationRevision;
        }

        PermissionDecision resolved = Objects.requireNonNull(resolver.get(), "resolver result");
        synchronized (this) {
            // An explicit invalidation which raced with evaluation wins: return the decision to
            // the current caller, but never repopulate the cache with a potentially stale value.
            if (observedInvalidationRevision != this.invalidationRevision) {
                return resolved;
            }
            this.values.put(key, new Value(resolved, this.nanoClock.getAsLong()));
            this.revision++;
            evictOldestEntries();
        }
        return resolved;
    }

    synchronized int invalidate(UUID actorId) {
        Objects.requireNonNull(actorId, "actorId");
        this.invalidationRevision++;
        this.revision++;
        int removed = 0;
        Iterator<Map.Entry<Key, Value>> iterator = this.values.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getKey().context().actorId().equals(actorId)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    synchronized CacheSnapshot snapshot() {
        return new CacheSnapshot(this.values.size(), this.hits, this.misses, this.evictions, this.revision);
    }

    private boolean isExpired(Value value, long now) {
        return now - value.createdAtNanos() >= this.timeToLiveNanos;
    }

    private void evictOldestEntries() {
        while (this.values.size() > this.maximumEntries) {
            Iterator<Map.Entry<Key, Value>> iterator = this.values.entrySet().iterator();
            iterator.next();
            iterator.remove();
            this.evictions++;
            this.revision++;
        }
    }

    record CacheSnapshot(int size, long hits, long misses, long evictions, long revision) {
    }

    private record Key(PermissionKey permission, PermissionContext context) {
    }

    private record Value(PermissionDecision decision, long createdAtNanos) {
    }
}
