package dev.uapi.client.ui.layout;

import java.util.Objects;

/** Width/height of zero preserves the child's current dimension. */
public record UIAnchorPlacement(UIAnchor anchor, int width, int height, int offsetX, int offsetY) {
    public UIAnchorPlacement {
        Objects.requireNonNull(anchor, "anchor");
        if (width < 0 || height < 0) throw new IllegalArgumentException("Anchored size cannot be negative");
    }

    public static UIAnchorPlacement at(UIAnchor anchor, int width, int height) {
        return new UIAnchorPlacement(anchor, width, height, 0, 0);
    }
}
