package dev.uapi.client.ui.layout;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIContainer;
import java.util.List;

final class UILayoutSupport {
    private UILayoutSupport() {
    }

    static List<UIComponent> visibleChildren(UIContainer container) {
        return container.children().stream().filter(UIComponent::visible).toList();
    }

    static UIBounds contentBounds(UIBounds bounds, UILayoutPadding padding) {
        return bounds.inset(padding.left(), padding.top(), padding.right(), padding.bottom());
    }

    static int distributedSize(int available, int gap, int count, int index) {
        if (count <= 0) return 0;
        int effectiveGap = effectiveGap(available, gap, count);
        int content = Math.max(0, available - Math.max(0, count - 1) * effectiveGap);
        int base = content / count;
        return base + (index < content % count ? 1 : 0);
    }

    static int effectiveGap(int available, int requestedGap, int count) {
        if (count <= 1) return 0;
        return Math.min(Math.max(0, requestedGap), Math.max(0, available) / (count - 1));
    }
}
