package dev.uapi.client.overlay;

/** Distance thresholds for lowering marker detail. */
public record WorldOverlayLodPolicy(double fullUntil, double compactUntil) {
    public static final WorldOverlayLodPolicy DEFAULT = new WorldOverlayLodPolicy(24, 64);

    public WorldOverlayLodPolicy {
        if (!Double.isFinite(fullUntil) || !Double.isFinite(compactUntil)
            || fullUntil < 0 || compactUntil < fullUntil) {
            throw new IllegalArgumentException("Overlay LOD thresholds must be finite and increasing");
        }
    }

    public WorldOverlayLod atDistance(double distance) {
        if (distance <= fullUntil) return WorldOverlayLod.FULL;
        if (distance <= compactUntil) return WorldOverlayLod.COMPACT;
        return WorldOverlayLod.MINIMAL;
    }
}
