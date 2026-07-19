package dev.uapi.client.ui.layout;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UILayout;
import java.util.List;

/** Two-panel layout. Extra children are stacked into the second panel. */
public record UISplitLayout(Axis axis, double ratio, int divider, UILayoutPadding padding) implements UILayout {
    public enum Axis { HORIZONTAL, VERTICAL }

    public UISplitLayout {
        if (axis == null) axis = Axis.HORIZONTAL;
        if (!Double.isFinite(ratio) || ratio <= 0 || ratio >= 1) {
            throw new IllegalArgumentException("Split ratio must be between zero and one");
        }
        if (divider < 0) throw new IllegalArgumentException("Split divider cannot be negative");
        if (padding == null) padding = UILayoutPadding.NONE;
    }

    public UISplitLayout(Axis axis, double ratio, int divider) {
        this(axis, ratio, divider, UILayoutPadding.NONE);
    }

    @Override
    public void layout(UIContainer container, UIBounds bounds) {
        List<UIComponent> children = UILayoutSupport.visibleChildren(container);
        if (children.isEmpty()) return;
        UIBounds content = UILayoutSupport.contentBounds(bounds, padding);
        UIBounds first;
        UIBounds second;
        if (axis == Axis.HORIZONTAL) {
            int effectiveDivider = Math.min(divider, content.width());
            int firstWidth = (int) Math.round((content.width() - effectiveDivider) * ratio);
            first = new UIBounds(content.x(), content.y(), firstWidth, content.height());
            second = new UIBounds(content.x() + firstWidth + effectiveDivider, content.y(),
                content.width() - effectiveDivider - firstWidth, content.height());
        } else {
            int effectiveDivider = Math.min(divider, content.height());
            int firstHeight = (int) Math.round((content.height() - effectiveDivider) * ratio);
            first = new UIBounds(content.x(), content.y(), content.width(), firstHeight);
            second = new UIBounds(content.x(), content.y() + firstHeight + effectiveDivider,
                content.width(), content.height() - effectiveDivider - firstHeight);
        }
        children.getFirst().setBounds(first);
        for (int index = 1; index < children.size(); index++) children.get(index).setBounds(second);
    }
}
