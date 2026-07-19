package dev.uapi.api.network;

import java.time.Duration;
import java.util.Objects;

/** Immutable outcome of one rate-limit acquisition attempt. */
public record RateLimitDecision(boolean allowed, int remainingPermits, Duration retryAfter) {
    public RateLimitDecision {
        if (remainingPermits < 0) throw new IllegalArgumentException("remainingPermits must not be negative");
        Objects.requireNonNull(retryAfter, "retryAfter");
        if (retryAfter.isNegative()) throw new IllegalArgumentException("retryAfter must not be negative");
        if (allowed && !retryAfter.isZero())
            throw new IllegalArgumentException("An allowed decision cannot require a retry delay");
    }
}
