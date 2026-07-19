package dev.uapi.client.overlay;

@FunctionalInterface
public interface WorldOverlayRenderer {
    void render(WorldOverlayRenderContext context, WorldOverlayMarker marker);
}
