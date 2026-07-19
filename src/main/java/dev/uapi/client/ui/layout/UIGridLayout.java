package dev.uapi.client.ui.layout;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UILayout;
import java.util.List;

/** Fixed-column grid with equal cells and deterministic remainder distribution. */
public record UIGridLayout(int columns, int horizontalGap, int verticalGap, UILayoutPadding padding)
    implements UILayout {
    public UIGridLayout {
        if (columns <= 0 || columns > 256) {
            throw new IllegalArgumentException("Grid columns must be within 1..256");
        }
        if (horizontalGap < 0 || verticalGap < 0) throw new IllegalArgumentException("Grid gaps cannot be negative");
        if (padding == null) padding = UILayoutPadding.NONE;
    }

    public UIGridLayout(int columns, int gap) {
        this(columns, gap, gap, UILayoutPadding.NONE);
    }

    @Override
    public void layout(UIContainer container, UIBounds bounds) {
        List<UIComponent> children = UILayoutSupport.visibleChildren(container);
        if (children.isEmpty()) return;
        UIBounds content = UILayoutSupport.contentBounds(bounds, padding);
        int rows = (int) ((children.size() + (long) columns - 1) / columns);
        int effectiveHorizontalGap = UILayoutSupport.effectiveGap(content.width(), horizontalGap, columns);
        int effectiveVerticalGap = UILayoutSupport.effectiveGap(content.height(), verticalGap, rows);
        int y = content.y();
        for (int row = 0; row < rows; row++) {
            int height = UILayoutSupport.distributedSize(content.height(), effectiveVerticalGap, rows, row);
            int x = content.x();
            for (int column = 0; column < columns; column++) {
                int width = UILayoutSupport.distributedSize(content.width(), effectiveHorizontalGap, columns, column);
                int index = row * columns + column;
                if (index < children.size()) children.get(index).setBounds(x, y, width, height);
                x += width + effectiveHorizontalGap;
            }
            y += height + effectiveVerticalGap;
        }
    }
}
