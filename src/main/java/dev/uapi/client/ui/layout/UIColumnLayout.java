package dev.uapi.client.ui.layout;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UILayout;
import java.util.List;

/** Equal-height vertical column. Remainder pixels are assigned from top to bottom. */
public record UIColumnLayout(int gap, UILayoutPadding padding) implements UILayout {
    public UIColumnLayout {
        if (gap < 0) throw new IllegalArgumentException("Column gap cannot be negative");
        if (padding == null) padding = UILayoutPadding.NONE;
    }

    public UIColumnLayout(int gap) {
        this(gap, UILayoutPadding.NONE);
    }

    @Override
    public void layout(UIContainer container, UIBounds bounds) {
        List<UIComponent> children = UILayoutSupport.visibleChildren(container);
        UIBounds content = UILayoutSupport.contentBounds(bounds, padding);
        int effectiveGap = UILayoutSupport.effectiveGap(content.height(), gap, children.size());
        int y = content.y();
        for (int index = 0; index < children.size(); index++) {
            int height = UILayoutSupport.distributedSize(content.height(), effectiveGap, children.size(), index);
            children.get(index).setBounds(content.x(), y, content.width(), height);
            y += height + effectiveGap;
        }
    }
}
