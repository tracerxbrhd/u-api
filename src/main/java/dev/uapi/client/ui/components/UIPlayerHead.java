package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import java.util.Objects;
import java.util.UUID;

/** Renderer-neutral player head component; consumers can share a bounded PlayerHeadCache. */
public final class UIPlayerHead extends UIComponent {
    private UUID playerId;
    private final UIPlayerHeadRenderer renderer;

    public UIPlayerHead(UUID playerId, UIPlayerHeadRenderer renderer) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        invalidateRender();
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        renderer.render(context, bounds(), playerId);
    }
}
