package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.lwjgl.glfw.GLFW;

/** Renders only visible rows and never creates per-item child components. */
public final class UIVirtualizedList<T> extends UIComponent {
    private final int rowHeight;
    private final UIVirtualListRowRenderer<T> rowRenderer;
    private final Consumer<T> onSelected;
    private List<T> items = List.of();
    private int scrollOffset;
    private int selectedIndex = -1;

    public UIVirtualizedList(int rowHeight, UIVirtualListRowRenderer<T> rowRenderer, Consumer<T> onSelected) {
        if (rowHeight <= 0) throw new IllegalArgumentException("Virtual list row height must be positive");
        this.rowHeight = rowHeight;
        this.rowRenderer = Objects.requireNonNull(rowRenderer, "rowRenderer");
        this.onSelected = Objects.requireNonNull(onSelected, "onSelected");
    }

    public void setItems(List<T> items) {
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (selectedIndex >= this.items.size()) selectedIndex = -1;
        setScrollOffset(scrollOffset);
        invalidateRender();
    }

    public List<T> items() {
        return items;
    }

    public Optional<T> selectedItem() {
        return selectedIndex < 0 ? Optional.empty() : Optional.of(items.get(selectedIndex));
    }

    public void setScrollOffset(int scrollOffset) {
        int next = Math.max(0, Math.min(maxScroll(), scrollOffset));
        if (this.scrollOffset == next) return;
        this.scrollOffset = next;
        invalidateRender();
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public int maxScroll() {
        long contentHeight = (long) items.size() * rowHeight;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, contentHeight - bounds().height()));
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled() || items.isEmpty()) return false;
        int direction = switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> -1;
            case GLFW.GLFW_KEY_DOWN -> 1;
            default -> 0;
        };
        if (direction == 0) return false;
        int next = selectedIndex < 0 ? (direction > 0 ? 0 : items.size() - 1)
            : Math.max(0, Math.min(items.size() - 1, selectedIndex + direction));
        if (next != selectedIndex) {
            selectedIndex = next;
            ensureSelectedRowVisible();
            onSelected.accept(items.get(next));
            invalidateRender();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        if (!enabled() || context.button() != 0) return false;
        int index = (int) ((context.mouseY() - bounds().y() + scrollOffset) / rowHeight);
        if (index < 0 || index >= items.size()) return false;
        requestFocus();
        selectedIndex = index;
        onSelected.accept(items.get(index));
        invalidateRender();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!enabled() || scrollY == 0 || maxScroll() == 0) return false;
        int previous = scrollOffset;
        long requested = scrollOffset - (long) Math.signum(scrollY) * rowHeight;
        setScrollOffset((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, requested)));
        return previous != scrollOffset;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        if (items.isEmpty() || bounds().width() == 0 || bounds().height() == 0) return;
        int first = Math.max(0, scrollOffset / rowHeight);
        long viewportBottom = (long) scrollOffset + bounds().height() - 1;
        int last = (int) Math.min(items.size() - 1L, viewportBottom / rowHeight);
        int firstY = bounds().y() + first * rowHeight - scrollOffset;
        context.graphics().enableScissor(bounds().x(), bounds().y(), bounds().right(), bounds().bottom());
        try {
            for (int index = first; index <= last; index++) {
                UIBounds row = new UIBounds(bounds().x(), firstY + (index - first) * rowHeight,
                    bounds().width(), rowHeight);
                boolean selected = index == selectedIndex;
                boolean hovered = row.contains(context.mouseX(), context.mouseY());
                int background = theme().color(selected ? ColorToken.ACCENT_PRIMARY
                    : hovered ? ColorToken.BACKGROUND_PANEL
                    : index % 2 == 0 ? ColorToken.BACKGROUND_PRIMARY : ColorToken.BACKGROUND_SECONDARY);
                if (selected) background = background & 0x00FFFFFF | 0x70000000;
                context.graphics().fill(row.x(), row.y(), row.right(), row.bottom(), background);
                rowRenderer.render(context, row, items.get(index), index, selected, hovered);
            }
        } finally {
            context.graphics().disableScissor();
        }
    }

    private void ensureSelectedRowVisible() {
        if (selectedIndex < 0) return;
        long rowTop = (long) selectedIndex * rowHeight;
        long rowBottom = rowTop + rowHeight;
        if (rowTop < scrollOffset) setScrollOffset((int) Math.min(Integer.MAX_VALUE, rowTop));
        else if (rowBottom > (long) scrollOffset + bounds().height()) {
            setScrollOffset((int) Math.min(Integer.MAX_VALUE, rowBottom - bounds().height()));
        }
    }

    @Override
    protected void onBoundsChanged(UIBounds previousBounds, UIBounds nextBounds) {
        setScrollOffset(scrollOffset);
    }
}
