package dev.uapi.client.overlay;

/** Fallback renderer keeps unknown marker types visible without coupling U-API to an owning mod. */
final class DefaultWorldOverlayRenderer implements WorldOverlayRenderer {
    static final DefaultWorldOverlayRenderer INSTANCE = new DefaultWorldOverlayRenderer();

    private DefaultWorldOverlayRenderer() {
    }

    @Override
    public void render(WorldOverlayRenderContext context, WorldOverlayMarker marker) {
        int radius = Math.max(2, context.pixelSize() / 2);
        int color = context.edgeIndicator() ? 0xFFF0B34C : 0xFF6699FF;
        context.graphics().fill(context.screenX() - radius, context.screenY() - radius,
            context.screenX() + radius + 1, context.screenY() + radius + 1, color);
        if (context.lod() != WorldOverlayLod.MINIMAL && !marker.label().getString().isEmpty()) {
            int halfWidth = Math.min(context.graphics().guiWidth() / 2,
                context.font().width(marker.label()) / 2 + 2);
            int labelX = Math.max(halfWidth,
                Math.min(context.graphics().guiWidth() - halfWidth, context.screenX()));
            context.graphics().centeredText(context.font(), marker.label(), labelX,
                context.screenY() + radius + 3, 0xFFFFFFFF);
        }
    }
}
