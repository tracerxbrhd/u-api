package dev.uapi.api.diagnostics;

import dev.uapi.api.services.UApiServices;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongSupplier;
import net.minecraft.resources.Identifier;

/**
 * Opt-in low-overhead instrumentation shared by U-API modules.
 *
 * <p>Hot-path timing and counters return immediately while disabled. Active object counts are
 * maintained independently so enabling diagnostics mid-session still produces a correct snapshot.</p>
 */
public final class UApiDiagnostics {
    private static final LongAdder UI_LAYOUT_SAMPLES = new LongAdder();
    private static final LongAdder UI_LAYOUT_NANOS = new LongAdder();
    private static final LongAccumulator UI_LAYOUT_MAX = new LongAccumulator(Long::max, 0);
    private static final LongAdder UI_RENDER_SAMPLES = new LongAdder();
    private static final LongAdder UI_RENDER_NANOS = new LongAdder();
    private static final LongAccumulator UI_RENDER_MAX = new LongAccumulator(Long::max, 0);
    private static final LongAdder LAYOUT_INVALIDATIONS = new LongAdder();
    private static final LongAdder RENDER_INVALIDATIONS = new LongAdder();
    private static final AtomicLong ACTIVE_COMPONENTS = new AtomicLong();
    private static final AtomicLong ACTIVE_OVERLAYS = new AtomicLong();
    private static final LongAdder PLAYER_HEAD_HITS = new LongAdder();
    private static final LongAdder PLAYER_HEAD_MISSES = new LongAdder();
    private static final LongAdder PLAYER_HEAD_EVICTIONS = new LongAdder();
    private static final AtomicLong PLAYER_HEAD_ENTRIES = new AtomicLong();
    private static final LongAdder INBOUND_PACKETS = new LongAdder();
    private static final LongAdder OUTBOUND_PACKETS = new LongAdder();
    private static final Map<Identifier, GaugeEntry> GAUGES = new ConcurrentHashMap<>();
    private static final AtomicLong NEXT_GAUGE_ID = new AtomicLong();
    private static volatile boolean enabled;
    private static volatile long enabledSinceNanos = System.nanoTime();
    private static volatile long collectionStoppedNanos = enabledSinceNanos;

    private UApiDiagnostics() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        if (UApiDiagnostics.enabled == enabled) return;
        if (enabled) {
            resetCollectedMetrics();
            enabledSinceNanos = System.nanoTime();
            collectionStoppedNanos = enabledSinceNanos;
        } else {
            collectionStoppedNanos = System.nanoTime();
        }
        UApiDiagnostics.enabled = enabled;
    }

    /** Returns zero without reading the system clock while collection is disabled. */
    public static long startTimer() {
        return enabled ? System.nanoTime() : 0;
    }

    public static void recordUiLayoutTime(long startedNanos) {
        if (!enabled || startedNanos == 0) return;
        recordTiming(startedNanos, UI_LAYOUT_SAMPLES, UI_LAYOUT_NANOS, UI_LAYOUT_MAX);
    }

    public static void recordUiRenderTime(long startedNanos) {
        if (!enabled || startedNanos == 0) return;
        recordTiming(startedNanos, UI_RENDER_SAMPLES, UI_RENDER_NANOS, UI_RENDER_MAX);
    }

    public static void recordUiLayoutInvalidation() {
        if (enabled) LAYOUT_INVALIDATIONS.increment();
    }

    public static void recordUiRenderInvalidation() {
        if (enabled) RENDER_INVALIDATIONS.increment();
    }

    public static void uiComponentMounted() {
        ACTIVE_COMPONENTS.incrementAndGet();
    }

    public static void uiComponentUnmounted() {
        ACTIVE_COMPONENTS.updateAndGet(value -> Math.max(0, value - 1));
    }

    public static void overlayAdded() {
        ACTIVE_OVERLAYS.incrementAndGet();
    }

    public static void overlayRemoved() {
        ACTIVE_OVERLAYS.updateAndGet(value -> Math.max(0, value - 1));
    }

    public static void recordPlayerHeadCacheHit() {
        if (enabled) PLAYER_HEAD_HITS.increment();
    }

    public static void recordPlayerHeadCacheMiss() {
        if (enabled) PLAYER_HEAD_MISSES.increment();
    }

    public static void recordPlayerHeadCacheEviction() {
        if (enabled) PLAYER_HEAD_EVICTIONS.increment();
    }

    public static void setPlayerHeadCacheEntries(long entries) {
        PLAYER_HEAD_ENTRIES.set(Math.max(0, entries));
    }

    public static void recordInboundPacket() {
        if (enabled) INBOUND_PACKETS.increment();
    }

    public static void recordOutboundPacket() {
        if (enabled) OUTBOUND_PACKETS.increment();
    }

    /** Registers a scalar extension such as a consumer-owned loaded-chunk count. */
    public static DiagnosticRegistration registerGauge(Identifier id, LongSupplier supplier) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(supplier, "supplier");
        GaugeEntry entry = new GaugeEntry(NEXT_GAUGE_ID.incrementAndGet(), supplier);
        if (GAUGES.putIfAbsent(id, entry) != null) {
            throw new IllegalStateException("Diagnostic gauge is already registered: " + id);
        }
        return new GaugeHandle(id, entry);
    }

    public static UApiDiagnosticSnapshot snapshot() {
        long inbound = INBOUND_PACKETS.sum();
        long outbound = OUTBOUND_PACKETS.sum();
        long sampledAt = enabled ? System.nanoTime() : collectionStoppedNanos;
        double seconds = Math.max(0.001, (sampledAt - enabledSinceNanos) / 1_000_000_000.0);
        Map<Identifier, Long> gauges = new LinkedHashMap<>();
        GAUGES.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                gauges.put(entry.getKey(), entry.getValue().supplier().getAsLong());
            } catch (RuntimeException ignored) {
                // A broken optional contributor must not break the diagnostic screen or command.
            }
        });
        return new UApiDiagnosticSnapshot(
            enabled,
            Instant.now(),
            timing(UI_LAYOUT_SAMPLES, UI_LAYOUT_NANOS, UI_LAYOUT_MAX),
            timing(UI_RENDER_SAMPLES, UI_RENDER_NANOS, UI_RENDER_MAX),
            ACTIVE_COMPONENTS.get(),
            LAYOUT_INVALIDATIONS.sum(),
            RENDER_INVALIDATIONS.sum(),
            ACTIVE_OVERLAYS.get(),
            new PlayerHeadCacheDiagnostic(PLAYER_HEAD_HITS.sum(), PLAYER_HEAD_MISSES.sum(),
                PLAYER_HEAD_EVICTIONS.sum(), PLAYER_HEAD_ENTRIES.get()),
            new PacketRateDiagnostic(inbound, outbound, inbound / seconds, outbound / seconds),
            UApiServices.diagnosticSnapshot(),
            gauges
        );
    }

    private static void recordTiming(long startedNanos, LongAdder samples, LongAdder total, LongAccumulator maximum) {
        long elapsed = Math.max(0, System.nanoTime() - startedNanos);
        samples.increment();
        total.add(elapsed);
        maximum.accumulate(elapsed);
    }

    private static TimingDiagnostic timing(LongAdder samples, LongAdder total, LongAccumulator maximum) {
        return new TimingDiagnostic(samples.sum(), total.sum(), maximum.get());
    }

    private static void resetCollectedMetrics() {
        UI_LAYOUT_SAMPLES.reset();
        UI_LAYOUT_NANOS.reset();
        UI_LAYOUT_MAX.reset();
        UI_RENDER_SAMPLES.reset();
        UI_RENDER_NANOS.reset();
        UI_RENDER_MAX.reset();
        LAYOUT_INVALIDATIONS.reset();
        RENDER_INVALIDATIONS.reset();
        PLAYER_HEAD_HITS.reset();
        PLAYER_HEAD_MISSES.reset();
        PLAYER_HEAD_EVICTIONS.reset();
        INBOUND_PACKETS.reset();
        OUTBOUND_PACKETS.reset();
    }

    private record GaugeEntry(long registrationId, LongSupplier supplier) {
    }

    private static final class GaugeHandle implements DiagnosticRegistration {
        private final Identifier id;
        private final GaugeEntry entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private GaugeHandle(Identifier id, GaugeEntry entry) {
            this.id = id;
            this.entry = entry;
        }

        @Override
        public Identifier id() {
            return id;
        }

        @Override
        public boolean isActive() {
            return !closed.get() && GAUGES.get(id) == entry;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) GAUGES.remove(id, entry);
        }
    }
}
