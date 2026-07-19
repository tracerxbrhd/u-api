package dev.uapi.client.overlay;

import dev.uapi.UApi;
import dev.uapi.api.diagnostics.UApiDiagnostics;
import dev.uapi.config.UApiClientConfigManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Connection-scoped marker store, interpolation, projection, pruning and renderer dispatch. */
public final class UApiWorldOverlays {
    private static final Object LOCK = new Object();
    private static final Map<UUID, Entry> MARKERS = new HashMap<>();
    private static final Map<UUID, VisibilitySample> OCCLUSION_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RendererEntry> RENDERERS = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> VISIBILITY = new HashMap<>();
    private static final Set<ResourceLocation> FAILED_RENDERERS = new HashSet<>();
    private static final AtomicLong NEXT_REGISTRATION_ID = new AtomicLong();
    private static volatile WorldOverlayProjection projection;
    private static volatile WorldOverlayVisibilityResolver visibilityResolver =
        DefaultWorldOverlayVisibilityResolver.INSTANCE;
    private static final AtomicBoolean VISIBILITY_RESOLVER_FAILURE_LOGGED = new AtomicBoolean();

    private UApiWorldOverlays() {
    }

    public static void upsert(WorldOverlayMarker marker) {
        tryUpsert(marker);
    }

    /** Returns false only when the bounded store contains exclusively higher-priority markers. */
    public static boolean tryUpsert(WorldOverlayMarker marker) {
        Objects.requireNonNull(marker, "marker");
        long now = System.nanoTime();
        synchronized (LOCK) {
            Entry previous = MARKERS.get(marker.id());
            if (previous != null && previous.expiresNanos() <= now) {
                MARKERS.remove(marker.id());
                OCCLUSION_CACHE.remove(marker.id());
                UApiDiagnostics.overlayRemoved();
                previous = null;
            }
            if (previous == null && MARKERS.size() >= UApiClientConfigManager.maximumWorldOverlays()
                && !makeRoomFor(marker)) return false;
            Vec3 start = previous == null || !previous.marker().dimension().equals(marker.dimension())
                ? marker.position() : previous.positionAt(now);
            long expires = marker.lifetime().isZero() ? Long.MAX_VALUE : saturatingAdd(now, marker.lifetime().toNanos());
            MARKERS.put(marker.id(), new Entry(marker, start, marker.position(), now, expires));
            OCCLUSION_CACHE.remove(marker.id());
            if (previous == null) UApiDiagnostics.overlayAdded();
            return true;
        }
    }

    public static boolean remove(UUID markerId) {
        Objects.requireNonNull(markerId, "markerId");
        synchronized (LOCK) {
            if (MARKERS.remove(markerId) == null) return false;
            OCCLUSION_CACHE.remove(markerId);
            UApiDiagnostics.overlayRemoved();
            return true;
        }
    }

    /** Clears connection-owned markers and the last world projection, but preserves global renderers. */
    public static void clear() {
        synchronized (LOCK) {
            int count = MARKERS.size();
            MARKERS.clear();
            OCCLUSION_CACHE.clear();
            for (int index = 0; index < count; index++) UApiDiagnostics.overlayRemoved();
        }
        projection = null;
    }

    public static int activeMarkerCount() {
        synchronized (LOCK) {
            return MARKERS.size();
        }
    }

    /** Prunes lifetimes even while the overlay render preference is disabled. */
    public static void tick() {
        long now = System.nanoTime();
        pruneExpired(now);
        trimToLimit();
    }

    /** Client preference keyed independently from renderer type. */
    public static void setVisibility(ResourceLocation visibilityKey, boolean visible) {
        synchronized (LOCK) {
            VISIBILITY.put(Objects.requireNonNull(visibilityKey, "visibilityKey"), visible);
        }
    }

    public static boolean isVisible(ResourceLocation visibilityKey) {
        synchronized (LOCK) {
            return VISIBILITY.getOrDefault(Objects.requireNonNull(visibilityKey, "visibilityKey"), true);
        }
    }

    public static WorldOverlayRendererRegistration registerRenderer(ResourceLocation type,
                                                                     WorldOverlayRenderer renderer) {
        Objects.requireNonNull(type, "type");
        RendererEntry entry = new RendererEntry(NEXT_REGISTRATION_ID.incrementAndGet(),
            Objects.requireNonNull(renderer, "renderer"));
        synchronized (LOCK) {
            if (RENDERERS.putIfAbsent(type, entry) != null) {
                throw new IllegalStateException("World overlay renderer already registered: " + type);
            }
            FAILED_RENDERERS.remove(type);
        }
        return new RendererHandle(type, entry);
    }

    public static void setVisibilityResolver(WorldOverlayVisibilityResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");
        synchronized (LOCK) {
            visibilityResolver = resolver;
            OCCLUSION_CACHE.clear();
        }
        VISIBILITY_RESOLVER_FAILURE_LOGGED.set(false);
    }

    public static void resetVisibilityResolver() {
        synchronized (LOCK) {
            visibilityResolver = DefaultWorldOverlayVisibilityResolver.INSTANCE;
            OCCLUSION_CACHE.clear();
        }
        VISIBILITY_RESOLVER_FAILURE_LOGGED.set(false);
    }

    /** Captures immutable matrices at the end of world rendering for the following GUI overlay pass. */
    public static void captureProjection(RenderLevelStageEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            projection = null;
            return;
        }
        projection = new WorldOverlayProjection(minecraft.level.dimension(), event.getCamera().getPosition(),
            new Matrix4f(event.getModelViewMatrix()), new Matrix4f(event.getProjectionMatrix()));
    }

    public static void render(Minecraft minecraft, GuiGraphics graphics, DeltaTracker deltaTracker) {
        WorldOverlayProjection frame = projection;
        if (frame == null || minecraft.level == null || !frame.dimension().equals(minecraft.level.dimension())) return;
        long now = System.nanoTime();
        List<ResolvedMarker> resolved = resolveMarkers(minecraft, now, frame, graphics.guiWidth(), graphics.guiHeight());
        resolved.sort(Comparator.comparingInt((ResolvedMarker value) -> value.marker().priority())
            .thenComparing(Comparator.comparingDouble(ResolvedMarker::distance).reversed()));
        for (ResolvedMarker value : resolved) {
            WorldOverlayRenderer renderer;
            synchronized (LOCK) {
                RendererEntry entry = RENDERERS.get(value.marker().type());
                renderer = entry == null || FAILED_RENDERERS.contains(value.marker().type())
                    ? DefaultWorldOverlayRenderer.INSTANCE : entry.renderer();
            }
            WorldOverlayRenderContext context = new WorldOverlayRenderContext(minecraft, graphics, minecraft.font,
                deltaTracker, value.point().x(), value.point().y(), value.pixelSize(), value.distance(),
                value.point().outsideScreen(), value.marker().lodPolicy().atDistance(value.distance()));
            try {
                renderer.render(context, value.marker());
            } catch (RuntimeException exception) {
                boolean firstFailure;
                synchronized (LOCK) {
                    firstFailure = FAILED_RENDERERS.add(value.marker().type());
                }
                if (firstFailure) UApi.LOGGER.error(
                    "Disabling failed world overlay renderer " + value.marker().type(), exception);
                DefaultWorldOverlayRenderer.INSTANCE.render(context, value.marker());
            }
        }
    }

    private static List<ResolvedMarker> resolveMarkers(Minecraft minecraft, long now, WorldOverlayProjection frame,
                                                        int guiWidth, int guiHeight) {
        List<Entry> entries;
        pruneExpired(now);
        synchronized (LOCK) {
            entries = List.copyOf(MARKERS.values());
        }
        List<ResolvedMarker> result = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            WorldOverlayMarker marker = entry.marker();
            if (!marker.dimension().equals(frame.dimension()) || !isVisible(marker.visibilityKey())) continue;
            Vec3 position = entry.positionAt(now);
            double distance = position.distanceTo(frame.cameraPosition());
            if (!Double.isFinite(distance)) continue;
            if (distance < marker.minimumDistance() || distance > marker.maximumDistance()) continue;
            if (!resolveVisibility(minecraft, marker, position, distance, now)) continue;
            int pixelSize = pixelSize(marker, distance);
            WorldOverlayProjection.ProjectedPoint point = frame.project(position, guiWidth, guiHeight,
                Math.max(4, pixelSize / 2 + 2));
            if (point.outsideScreen() && !marker.edgeIndicator()) continue;
            result.add(new ResolvedMarker(marker, point, distance, pixelSize));
        }
        return result;
    }

    private static void pruneExpired(long now) {
        synchronized (LOCK) {
            int before = MARKERS.size();
            MARKERS.entrySet().removeIf(mapEntry -> {
                boolean expired = mapEntry.getValue().expiresNanos() <= now;
                if (expired) OCCLUSION_CACHE.remove(mapEntry.getKey());
                return expired;
            });
            int removed = before - MARKERS.size();
            for (int index = 0; index < removed; index++) UApiDiagnostics.overlayRemoved();
        }
    }

    private static int pixelSize(WorldOverlayMarker marker, double distance) {
        double range = marker.maximumDistance() - marker.minimumDistance();
        double fraction = range <= 0 ? 0 : 1 - (distance - marker.minimumDistance()) / range;
        return marker.minimumPixelSize() + (int) Math.round(Math.max(0, Math.min(1, fraction))
            * (marker.maximumPixelSize() - marker.minimumPixelSize()));
    }

    private static boolean resolveVisibility(Minecraft minecraft, WorldOverlayMarker marker, Vec3 position,
                                             double distance, long now) {
        if (marker.occlusionMode() == OcclusionMode.THROUGH_WALLS) return true;
        if (VISIBILITY_RESOLVER_FAILURE_LOGGED.get()) return false;
        long cacheDuration = switch (marker.lodPolicy().atDistance(distance)) {
            case FULL -> 100_000_000L;
            case COMPACT -> 250_000_000L;
            case MINIMAL -> 500_000_000L;
        };
        synchronized (LOCK) {
            VisibilitySample cached = OCCLUSION_CACHE.get(marker.id());
            if (cached != null && now - cached.checkedNanos() < cacheDuration) return cached.visible();
        }
        boolean visible;
        try {
            visible = visibilityResolver.isVisible(minecraft, marker, position, distance);
        } catch (RuntimeException exception) {
            if (VISIBILITY_RESOLVER_FAILURE_LOGGED.compareAndSet(false, true)) {
                UApi.LOGGER.error("World overlay visibility resolver failed; markers fail closed", exception);
            }
            visible = false;
        }
        synchronized (LOCK) {
            OCCLUSION_CACHE.put(marker.id(), new VisibilitySample(now, visible));
        }
        return visible;
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static boolean makeRoomFor(WorldOverlayMarker incoming) {
        Map.Entry<UUID, Entry> candidate = MARKERS.entrySet().stream().min(Comparator
            .comparingInt((Map.Entry<UUID, Entry> value) -> value.getValue().marker().priority())
            .thenComparingLong(value -> value.getValue().transitionStartedNanos())).orElse(null);
        if (candidate == null) return true;
        if (candidate.getValue().marker().priority() > incoming.priority()) return false;
        MARKERS.remove(candidate.getKey());
        OCCLUSION_CACHE.remove(candidate.getKey());
        UApiDiagnostics.overlayRemoved();
        return true;
    }

    private static void trimToLimit() {
        synchronized (LOCK) {
            int maximum = UApiClientConfigManager.maximumWorldOverlays();
            while (MARKERS.size() > maximum) {
                Map.Entry<UUID, Entry> candidate = MARKERS.entrySet().stream().min(Comparator
                    .comparingInt((Map.Entry<UUID, Entry> value) -> value.getValue().marker().priority())
                    .thenComparingLong(value -> value.getValue().transitionStartedNanos())).orElse(null);
                if (candidate == null) return;
                MARKERS.remove(candidate.getKey());
                OCCLUSION_CACHE.remove(candidate.getKey());
                UApiDiagnostics.overlayRemoved();
            }
        }
    }

    private record Entry(WorldOverlayMarker marker, Vec3 previousPosition, Vec3 targetPosition,
                         long transitionStartedNanos, long expiresNanos) {
        Vec3 positionAt(long now) {
            long duration = marker.interpolationDuration().toNanos();
            if (duration <= 0) return targetPosition;
            double progress = Math.max(0, Math.min(1, (now - transitionStartedNanos) / (double) duration));
            return previousPosition.lerp(targetPosition, progress);
        }
    }

    private record RendererEntry(long registrationId, WorldOverlayRenderer renderer) {
    }

    private record ResolvedMarker(WorldOverlayMarker marker, WorldOverlayProjection.ProjectedPoint point,
                                  double distance, int pixelSize) {
    }

    private record VisibilitySample(long checkedNanos, boolean visible) {
    }

    private static final class RendererHandle implements WorldOverlayRendererRegistration {
        private final ResourceLocation type;
        private final RendererEntry expected;
        private final AtomicBoolean closed = new AtomicBoolean();

        private RendererHandle(ResourceLocation type, RendererEntry expected) {
            this.type = type;
            this.expected = expected;
        }

        @Override public ResourceLocation type() { return type; }
        @Override public boolean isActive() { synchronized (LOCK) { return !closed.get() && RENDERERS.get(type) == expected; } }
        @Override public void close() { if (closed.compareAndSet(false, true)) synchronized (LOCK) {
            if (RENDERERS.remove(type, expected)) FAILED_RENDERERS.remove(type);
        } }
    }
}
