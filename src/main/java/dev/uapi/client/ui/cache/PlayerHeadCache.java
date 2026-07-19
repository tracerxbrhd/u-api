package dev.uapi.client.ui.cache;

import dev.uapi.api.diagnostics.UApiDiagnostics;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/** Bounded access-order cache for skin/head render data. Values are never loaded from render implicitly. */
public final class PlayerHeadCache<K, V> {
    private static final AtomicLong TOTAL_ENTRIES = new AtomicLong();
    private final int capacity;
    private final LinkedHashMap<K, V> entries = new LinkedHashMap<>(16, 0.75f, true);

    public PlayerHeadCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Player head cache capacity must be positive");
        this.capacity = capacity;
    }

    public synchronized Optional<V> find(K key) {
        Objects.requireNonNull(key, "key");
        V value = entries.get(key);
        if (value == null) UApiDiagnostics.recordPlayerHeadCacheMiss();
        else UApiDiagnostics.recordPlayerHeadCacheHit();
        return Optional.ofNullable(value);
    }

    /** Explicit load path intended for snapshot/update handlers rather than render methods. */
    public synchronized V getOrLoad(K key, Function<? super K, ? extends V> loader) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(loader, "loader");
        V existing = entries.get(key);
        if (existing != null) {
            UApiDiagnostics.recordPlayerHeadCacheHit();
            return existing;
        }
        UApiDiagnostics.recordPlayerHeadCacheMiss();
        V loaded = Objects.requireNonNull(loader.apply(key), "loader result");
        put(key, loaded);
        return loaded;
    }

    public synchronized void put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        boolean added = !entries.containsKey(key);
        entries.put(key, value);
        if (added) TOTAL_ENTRIES.incrementAndGet();
        while (entries.size() > capacity) {
            Map.Entry<K, V> eldest = entries.entrySet().iterator().next();
            entries.remove(eldest.getKey());
            TOTAL_ENTRIES.decrementAndGet();
            UApiDiagnostics.recordPlayerHeadCacheEviction();
        }
        UApiDiagnostics.setPlayerHeadCacheEntries(TOTAL_ENTRIES.get());
    }

    public synchronized void clear() {
        TOTAL_ENTRIES.addAndGet(-entries.size());
        entries.clear();
        UApiDiagnostics.setPlayerHeadCacheEntries(TOTAL_ENTRIES.get());
    }

    public synchronized int size() {
        return entries.size();
    }
}
