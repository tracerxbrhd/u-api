package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIRenderContext;

@FunctionalInterface
public interface UIVirtualListRowRenderer<T> {
    void render(UIRenderContext context, UIBounds rowBounds, T item, int index, boolean selected, boolean hovered);
}
