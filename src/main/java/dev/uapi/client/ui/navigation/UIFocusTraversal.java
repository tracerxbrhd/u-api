package dev.uapi.client.ui.navigation;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Deterministic depth-first focus order used by {@code UIScreen} for Tab/Shift+Tab. */
public final class UIFocusTraversal {
    private UIFocusTraversal() {
    }

    public static Optional<UIComponent> next(UIContainer root, UIComponent current, boolean reverse) {
        Objects.requireNonNull(root, "root");
        List<UIComponent> candidates = new ArrayList<>();
        collect(root, candidates);
        if (candidates.isEmpty()) return Optional.empty();
        int currentIndex = candidates.indexOf(current);
        int nextIndex;
        if (currentIndex < 0) nextIndex = reverse ? candidates.size() - 1 : 0;
        else nextIndex = Math.floorMod(currentIndex + (reverse ? -1 : 1), candidates.size());
        return Optional.of(candidates.get(nextIndex));
    }

    private static void collect(UIContainer container, List<UIComponent> result) {
        if (!container.visible() || !container.enabled()) return;
        for (UIComponent child : container.children()) {
            if (!child.visible() || !child.enabled()) continue;
            if (child.focusable() && child.mounted()) result.add(child);
            if (child instanceof UIContainer nested) collect(nested, result);
        }
    }
}
