package dev.uapi.client.ui.layout;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UILayout;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** Per-child nine-point anchoring without mutable global layout metadata. */
public final class UIAnchorLayout implements UILayout {
    private final Map<UIComponent, UIAnchorPlacement> placements = new IdentityHashMap<>();
    private final UILayoutPadding padding;

    public UIAnchorLayout() {
        this(UILayoutPadding.NONE);
    }

    public UIAnchorLayout(UILayoutPadding padding) {
        this.padding = Objects.requireNonNull(padding, "padding");
    }

    public UIAnchorLayout place(UIComponent component, UIAnchorPlacement placement) {
        placements.put(Objects.requireNonNull(component, "component"), Objects.requireNonNull(placement, "placement"));
        return this;
    }

    public UIAnchorLayout unplace(UIComponent component) {
        placements.remove(Objects.requireNonNull(component, "component"));
        return this;
    }

    @Override
    public void layout(UIContainer container, UIBounds bounds) {
        placements.keySet().retainAll(container.children());
        UIBounds content = UILayoutSupport.contentBounds(bounds, padding);
        for (UIComponent child : container.children()) {
            if (!child.visible()) continue;
            UIAnchorPlacement placement = placements.getOrDefault(child,
                UIAnchorPlacement.at(UIAnchor.TOP_LEFT, child.bounds().width(), child.bounds().height()));
            int width = placement.width() == 0 ? child.bounds().width() : Math.min(placement.width(), content.width());
            int height = placement.height() == 0 ? child.bounds().height() : Math.min(placement.height(), content.height());
            int x = switch (placement.anchor()) {
                case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> content.x();
                case TOP_CENTER, CENTER, BOTTOM_CENTER -> content.x() + (content.width() - width) / 2;
                case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> content.right() - width;
            };
            int y = switch (placement.anchor()) {
                case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> content.y();
                case CENTER_LEFT, CENTER, CENTER_RIGHT -> content.y() + (content.height() - height) / 2;
                case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> content.bottom() - height;
            };
            child.setBounds(x + placement.offsetX(), y + placement.offsetY(), width, height);
        }
    }
}
