package dev.uapi.api.network;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;
import net.minecraft.resources.ResourceLocation;

/**
 * Thread-safe exact sliding-window limiter keyed by actor and action.
 *
 * <p>The injected clock must return monotonically increasing nanoseconds. Tracking is bounded by
 * {@code maxTrackedKeys}. Once that bound is reached, unseen keys fail closed until a tracked
 * bucket expires or is explicitly cleared; active limits are never evicted in a way that could
 * reset an actor's allowance.</p>
 */
public final class ActorActionRateLimiter {
    private final int permitsPerWindow;
    private final long windowNanos;
    private final int maxTrackedKeys;
    private final LongSupplier nanoTime;
    private final LinkedHashMap<Key, Bucket> buckets = new LinkedHashMap<>(16, 0.75f, true);
    private boolean clockInitialized;
    private long lastObservedNanos;

    public ActorActionRateLimiter(int permitsPerWindow, Duration window, int maxTrackedKeys) {
        this(permitsPerWindow, window, maxTrackedKeys, System::nanoTime);
    }

    public ActorActionRateLimiter(int permitsPerWindow, Duration window, int maxTrackedKeys,
                                  LongSupplier nanoTime) {
        if (permitsPerWindow < 1) throw new IllegalArgumentException("permitsPerWindow must be positive");
        Objects.requireNonNull(window, "window");
        if (window.isZero() || window.isNegative()) throw new IllegalArgumentException("window must be positive");
        if (maxTrackedKeys < 1) throw new IllegalArgumentException("maxTrackedKeys must be positive");
        this.permitsPerWindow = permitsPerWindow;
        this.windowNanos = window.toNanos();
        this.maxTrackedKeys = maxTrackedKeys;
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    public synchronized RateLimitDecision tryAcquire(UUID actorId, ResourceLocation actionId) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(actionId, "actionId");
        long now = readNow();
        Key key = new Key(actorId, actionId);
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            cleanupExpired(now);
            if (buckets.size() >= maxTrackedKeys) {
                return new RateLimitDecision(false, 0, retryUntilTrackingCapacity(now));
            }
            bucket = new Bucket();
            buckets.put(key, bucket);
        }

        prune(bucket, now);
        if (bucket.acquisitions.size() < permitsPerWindow) {
            bucket.acquisitions.addLast(now);
            return new RateLimitDecision(true, permitsPerWindow - bucket.acquisitions.size(), Duration.ZERO);
        }

        long elapsed = Math.max(0L, now - bucket.acquisitions.getFirst());
        long retryNanos = Math.max(1L, windowNanos - elapsed);
        return new RateLimitDecision(false, 0, Duration.ofNanos(retryNanos));
    }

    public synchronized int cleanupExpired() {
        return cleanupExpired(readNow());
    }

    public synchronized int clearActor(UUID actorId) {
        Objects.requireNonNull(actorId, "actorId");
        int before = buckets.size();
        buckets.entrySet().removeIf(entry -> entry.getKey().actorId().equals(actorId));
        return before - buckets.size();
    }

    public synchronized void clear() {
        buckets.clear();
    }

    public synchronized int trackedKeys() {
        return buckets.size();
    }

    private int cleanupExpired(long now) {
        int removed = 0;
        Iterator<Map.Entry<Key, Bucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Bucket bucket = iterator.next().getValue();
            prune(bucket, now);
            if (bucket.acquisitions.isEmpty()) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private Duration retryUntilTrackingCapacity(long now) {
        long retryNanos = windowNanos;
        for (Bucket bucket : buckets.values()) {
            if (bucket.acquisitions.isEmpty()) continue;
            long elapsed = Math.max(0L, now - bucket.acquisitions.getLast());
            retryNanos = Math.min(retryNanos, Math.max(1L, windowNanos - elapsed));
        }
        return Duration.ofNanos(Math.max(1L, retryNanos));
    }

    private void prune(Bucket bucket, long now) {
        while (!bucket.acquisitions.isEmpty()
            && elapsedAtLeast(now, bucket.acquisitions.getFirst(), windowNanos)) {
            bucket.acquisitions.removeFirst();
        }
    }

    private long readNow() {
        long now = nanoTime.getAsLong();
        if (clockInitialized && now < lastObservedNanos)
            throw new IllegalStateException("Rate limiter clock moved backwards");
        clockInitialized = true;
        lastObservedNanos = now;
        return now;
    }

    private static boolean elapsedAtLeast(long now, long start, long duration) {
        return now >= start && now - start >= duration;
    }

    private record Key(UUID actorId, ResourceLocation actionId) {
        private Key {
            Objects.requireNonNull(actorId, "actorId");
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    private static final class Bucket {
        private final ArrayDeque<Long> acquisitions = new ArrayDeque<>();
    }
}
