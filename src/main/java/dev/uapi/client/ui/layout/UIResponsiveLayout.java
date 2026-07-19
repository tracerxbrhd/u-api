package dev.uapi.client.ui.layout;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UILayout;
import java.util.Objects;

/** Selects a compact or wide layout from the actual GUI-space width. */
public record UIResponsiveLayout(int breakpointWidth, UILayout compact, UILayout wide) implements UILayout {
    public UIResponsiveLayout {
        if (breakpointWidth <= 0) throw new IllegalArgumentException("Responsive breakpoint must be positive");
        Objects.requireNonNull(compact, "compact");
        Objects.requireNonNull(wide, "wide");
    }

    @Override
    public void layout(UIContainer container, UIBounds bounds) {
        (bounds.width() < breakpointWidth ? compact : wide).layout(container, bounds);
    }
}
