package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIRenderContext;
import java.util.UUID;

@FunctionalInterface
public interface UIPlayerHeadRenderer {
    void render(UIRenderContext context, UIBounds bounds, UUID playerId);
}
