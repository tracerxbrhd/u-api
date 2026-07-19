package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIRenderContext;

@FunctionalInterface
public interface UIIconRenderer {
    void render(UIRenderContext context, UIBounds bounds, boolean enabled);
}
