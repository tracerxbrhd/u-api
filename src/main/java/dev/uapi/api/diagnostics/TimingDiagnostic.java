package dev.uapi.api.diagnostics;

/** Accumulated timing metric; values are only collected while diagnostics are enabled. */
public record TimingDiagnostic(long samples, long totalNanos, long maximumNanos) {
    public double averageMicros() {
        return samples == 0 ? 0 : totalNanos / (double) samples / 1_000.0;
    }

    public double maximumMicros() {
        return maximumNanos / 1_000.0;
    }
}
