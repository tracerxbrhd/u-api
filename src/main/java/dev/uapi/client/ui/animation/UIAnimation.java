package dev.uapi.client.ui.animation;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Allocation-free, finite animation clock sampled from render code. */
public final class UIAnimation {
    private static final Duration MAX_DURATION = Duration.ofHours(1);
    private final long durationNanos;
    private final UIEasing easing;
    private final LongSupplier clock;
    private long startedNanos;
    private boolean running;

    public UIAnimation(Duration duration, UIEasing easing) {
        this(duration, easing, System::nanoTime);
    }

    UIAnimation(Duration duration, UIEasing easing, LongSupplier clock) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) throw new IllegalArgumentException("Animation duration cannot be negative");
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException("Animation duration cannot exceed one hour");
        }
        durationNanos = duration.toNanos();
        this.easing = Objects.requireNonNull(easing, "easing");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void start() {
        startedNanos = clock.getAsLong();
        running = true;
    }

    public void stop() {
        running = false;
    }

    public boolean running() {
        if (!running) return false;
        if (rawProgress() < 1) return true;
        running = false;
        return false;
    }

    public double progress() {
        double raw = rawProgress();
        if (raw >= 1) running = false;
        return easing.apply(raw);
    }

    public double interpolate(double from, double to) {
        return from + (to - from) * progress();
    }

    private double rawProgress() {
        if (!running) return 1;
        if (durationNanos == 0) return 1;
        return Math.max(0, Math.min(1, (clock.getAsLong() - startedNanos) / (double) durationNanos));
    }
}
