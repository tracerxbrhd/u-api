package dev.uapi.client.ui.core;

import dev.uapi.client.ui.layout.UIAbsoluteLayout;

/** Deterministic layout strategy applied to one retained container. */
@FunctionalInterface
public interface UILayout {
    void layout(UIContainer container, UIBounds bounds);

    static UILayout absolute() {
        return UIAbsoluteLayout.INSTANCE;
    }
}
