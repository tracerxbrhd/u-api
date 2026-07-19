package dev.uapi.client.ui.layout;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UILayout;

/** Places every visible child in the same content rectangle. */
public record UIStackLayout(UILayoutPadding padding) implements UILayout {
    public UIStackLayout {
        if (padding == null) padding = UILayoutPadding.NONE;
    }

    public UIStackLayout() {
        this(UILayoutPadding.NONE);
    }

    @Override
    public void layout(UIContainer container, UIBounds bounds) {
        UIBounds content = UILayoutSupport.contentBounds(bounds, padding);
        for (UIComponent child : container.children()) if (child.visible()) child.setBounds(content);
    }
}
