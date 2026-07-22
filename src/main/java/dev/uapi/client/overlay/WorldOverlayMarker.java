package dev.uapi.client.overlay;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Immutable marker snapshot. Reusing an ID lets packet deltas interpolate to a new position. */
public final class WorldOverlayMarker {
    private final UUID id;
    private final Identifier type;
    private final Identifier visibilityKey;
    private final ResourceKey<Level> dimension;
    private final Vec3 position;
    private final Component label;
    private final double minimumDistance;
    private final double maximumDistance;
    private final int minimumPixelSize;
    private final int maximumPixelSize;
    private final OcclusionMode occlusionMode;
    private final boolean edgeIndicator;
    private final Duration interpolationDuration;
    private final Duration lifetime;
    private final int priority;
    private final WorldOverlayLodPolicy lodPolicy;

    private WorldOverlayMarker(Builder builder) {
        id = builder.id;
        type = builder.type;
        visibilityKey = builder.visibilityKey == null ? builder.type : builder.visibilityKey;
        dimension = builder.dimension;
        position = builder.position;
        label = builder.label;
        minimumDistance = builder.minimumDistance;
        maximumDistance = builder.maximumDistance;
        minimumPixelSize = builder.minimumPixelSize;
        maximumPixelSize = builder.maximumPixelSize;
        occlusionMode = builder.occlusionMode;
        edgeIndicator = builder.edgeIndicator;
        interpolationDuration = builder.interpolationDuration;
        lifetime = builder.lifetime;
        priority = builder.priority;
        lodPolicy = builder.lodPolicy;
    }

    public UUID id() { return id; }
    public Identifier type() { return type; }
    public Identifier visibilityKey() { return visibilityKey; }
    public ResourceKey<Level> dimension() { return dimension; }
    public Vec3 position() { return position; }
    public Component label() { return label; }
    public double minimumDistance() { return minimumDistance; }
    public double maximumDistance() { return maximumDistance; }
    public int minimumPixelSize() { return minimumPixelSize; }
    public int maximumPixelSize() { return maximumPixelSize; }
    public OcclusionMode occlusionMode() { return occlusionMode; }
    public boolean edgeIndicator() { return edgeIndicator; }
    public Duration interpolationDuration() { return interpolationDuration; }
    /** Zero means the marker remains until explicitly removed or connection cleanup. */
    public Duration lifetime() { return lifetime; }
    public int priority() { return priority; }
    public WorldOverlayLodPolicy lodPolicy() { return lodPolicy; }

    public Builder toBuilder() {
        return new Builder(id, type, dimension, position)
            .visibilityKey(visibilityKey).label(label)
            .distanceRange(minimumDistance, maximumDistance)
            .pixelSizeRange(minimumPixelSize, maximumPixelSize)
            .occlusionMode(occlusionMode).edgeIndicator(edgeIndicator)
            .interpolation(interpolationDuration).lifetime(lifetime)
            .priority(priority).lodPolicy(lodPolicy);
    }

    public WorldOverlayMarker withPosition(ResourceKey<Level> dimension, Vec3 position) {
        return toBuilder().dimension(dimension).position(position).build();
    }

    public static Builder builder(UUID id, Identifier type, ResourceKey<Level> dimension, Vec3 position) {
        return new Builder(id, type, dimension, position);
    }

    public static Builder atBlock(UUID id, Identifier type, ResourceKey<Level> dimension, BlockPos position) {
        Objects.requireNonNull(position, "position");
        return builder(id, type, dimension, Vec3.atCenterOf(position));
    }

    public static final class Builder {
        private final UUID id;
        private final Identifier type;
        private ResourceKey<Level> dimension;
        private Vec3 position;
        private Identifier visibilityKey;
        private Component label = Component.empty();
        private double minimumDistance;
        private double maximumDistance = 256;
        private int minimumPixelSize = 6;
        private int maximumPixelSize = 18;
        private OcclusionMode occlusionMode = OcclusionMode.RESPECT_BLOCKS;
        private boolean edgeIndicator = true;
        private Duration interpolationDuration = Duration.ofMillis(150);
        private Duration lifetime = Duration.ZERO;
        private int priority;
        private WorldOverlayLodPolicy lodPolicy = WorldOverlayLodPolicy.DEFAULT;

        private Builder(UUID id, Identifier type, ResourceKey<Level> dimension, Vec3 position) {
            this.id = Objects.requireNonNull(id, "id");
            this.type = Objects.requireNonNull(type, "type");
            this.dimension = Objects.requireNonNull(dimension, "dimension");
            this.position = Objects.requireNonNull(position, "position");
            if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
                throw new IllegalArgumentException("Overlay position must be finite");
            }
        }

        public Builder visibilityKey(Identifier visibilityKey) { this.visibilityKey = Objects.requireNonNull(visibilityKey, "visibilityKey"); return this; }
        public Builder dimension(ResourceKey<Level> dimension) { this.dimension = Objects.requireNonNull(dimension, "dimension"); return this; }
        public Builder position(Vec3 position) {
            this.position = requireFinitePosition(position);
            return this;
        }
        public Builder label(Component label) { this.label = Objects.requireNonNull(label, "label"); return this; }
        public Builder distanceRange(double minimum, double maximum) {
            if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum < 0 || maximum <= minimum) {
                throw new IllegalArgumentException("Overlay distance range must be finite and increasing");
            }
            minimumDistance = minimum; maximumDistance = maximum; return this;
        }
        public Builder pixelSizeRange(int minimum, int maximum) {
            if (minimum <= 0 || maximum < minimum || maximum > 512) {
                throw new IllegalArgumentException("Overlay pixel size range must be within 1..512");
            }
            minimumPixelSize = minimum; maximumPixelSize = maximum; return this;
        }
        public Builder occlusionMode(OcclusionMode mode) { occlusionMode = Objects.requireNonNull(mode, "mode"); return this; }
        public Builder edgeIndicator(boolean edgeIndicator) { this.edgeIndicator = edgeIndicator; return this; }
        public Builder interpolation(Duration duration) {
            interpolationDuration = nonNegative(duration, "interpolation"); return this;
        }
        public Builder lifetime(Duration duration) { lifetime = nonNegative(duration, "lifetime"); return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder lodPolicy(WorldOverlayLodPolicy lodPolicy) { this.lodPolicy = Objects.requireNonNull(lodPolicy, "lodPolicy"); return this; }
        public WorldOverlayMarker build() { return new WorldOverlayMarker(this); }

        private static Vec3 requireFinitePosition(Vec3 position) {
            Objects.requireNonNull(position, "position");
            if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
                throw new IllegalArgumentException("Overlay position must be finite");
            }
            return position;
        }

        private static Duration nonNegative(Duration duration, String name) {
            Objects.requireNonNull(duration, name);
            if (duration.isNegative()) throw new IllegalArgumentException("Overlay " + name + " cannot be negative");
            if (duration.compareTo(Duration.ofDays(365)) > 0) {
                throw new IllegalArgumentException("Overlay " + name + " cannot exceed 365 days");
            }
            return duration;
        }
    }
}
