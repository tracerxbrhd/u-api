package dev.uapi.client.ui.layout;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UILayout;
import java.util.List;

/** Equal-width horizontal row. Remainder pixels are assigned from left to right. */
public record UIRowLayout(int gap, UILayoutPadding padding) implements UILayout {
    public UIRowLayout {
        if (gap < 0) throw new IllegalArgumentException("Row gap cannot be negative");
        if (padding == null) padding = UILayoutPadding.NONE;
    }

    public UIRowLayout(int gap) {
        this(gap, UILayoutPadding.NONE);
    }

    @Override
    public void layout(UIContainer container, UIBounds bounds) {
        List<UIComponent> children = UILayoutSupport.visibleChildren(container);
        UIBounds content = UILayoutSupport.contentBounds(bounds, padding);
        int effectiveGap = UILayoutSupport.effectiveGap(content.width(), gap, children.size());
        int x = content.x();
        for (int index = 0; index < children.size(); index++) {
            int width = UILayoutSupport.distributedSize(content.width(), effectiveGap, children.size(), index);
            children.get(index).setBounds(x, content.y(), width, content.height());
            x += width + effectiveGap;
        }
    }
}
