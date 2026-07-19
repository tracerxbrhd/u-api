package dev.uapi.api.diagnostics;

public record PlayerHeadCacheDiagnostic(long hits, long misses, long evictions, long entries) {
    public double hitRatio() {
        long lookups = hits + misses;
        return lookups == 0 ? 0 : hits / (double) lookups;
    }
}
