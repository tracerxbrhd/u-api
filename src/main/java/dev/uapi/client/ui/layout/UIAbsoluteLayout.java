package dev.uapi.client.ui.layout;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UILayout;

/** Leaves child bounds untouched, making it suitable for overlays and custom positioning. */
public enum UIAbsoluteLayout implements UILayout {
    INSTANCE;

    @Override
    public void layout(UIContainer container, UIBounds bounds) {
    }
}
