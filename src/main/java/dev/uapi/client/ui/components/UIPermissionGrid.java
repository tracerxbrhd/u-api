package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

/** Virtualized permission matrix suitable for role editors. */
public final class UIPermissionGrid extends UIComponent {
    public record Entry(ResourceLocation key, Component label, boolean granted, boolean editable) {
        public Entry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
        }

        public Entry toggled() {
            return new Entry(key, label, !granted, editable);
        }
    }

    private final int rowHeight;
    private final Consumer<Entry> onChanged;
    private List<Entry> entries = List.of();
    private int scrollOffset;

    public UIPermissionGrid(int rowHeight, Consumer<Entry> onChanged) {
        if (rowHeight <= 0) throw new IllegalArgumentException("Permission row height must be positive");
        this.rowHeight = rowHeight;
        this.onChanged = Objects.requireNonNull(onChanged, "onChanged");
    }

    public void setEntries(List<Entry> entries) {
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        scrollOffset = Math.min(scrollOffset, maxScroll());
        invalidateRender();
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        if (!enabled() || context.button() != 0) return false;
        int index = (int) ((context.mouseY() - bounds().y() + scrollOffset) / rowHeight);
        if (index < 0 || index >= entries.size() || !entries.get(index).editable()) return false;
        Entry toggled = entries.get(index).toggled();
        java.util.ArrayList<Entry> next = new java.util.ArrayList<>(entries);
        next.set(index, toggled);
        entries = List.copyOf(next);
        onChanged.accept(toggled);
        invalidateRender();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0 || maxScroll() == 0) return false;
        int previous = scrollOffset;
        long requested = scrollOffset - (long) Math.signum(scrollY) * rowHeight;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll(), requested));
        if (previous != scrollOffset) invalidateRender();
        return previous != scrollOffset;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        if (entries.isEmpty() || bounds().width() <= 0 || bounds().height() <= 0) return;
        int first = Math.max(0, scrollOffset / rowHeight);
        long viewportBottom = (long) scrollOffset + bounds().height() - 1;
        int last = (int) Math.min(entries.size() - 1L, viewportBottom / rowHeight);
        context.graphics().enableScissor(bounds().x(), bounds().y(), bounds().right(), bounds().bottom());
        try {
            for (int index = first; index <= last; index++) {
                Entry entry = entries.get(index);
                int y = bounds().y() + index * rowHeight - scrollOffset;
                UIBounds row = new UIBounds(bounds().x(), y, bounds().width(), rowHeight);
                context.graphics().fill(row.x(), row.y(), row.right(), row.bottom(), theme().color(index % 2 == 0
                    ? ColorToken.BACKGROUND_SECONDARY : ColorToken.BACKGROUND_PANEL));
                context.graphics().drawString(context.font(), entry.label(), row.x() + 4,
                    row.y() + (row.height() - context.font().lineHeight) / 2,
                    theme().color(entry.editable() ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_MUTED), false);
                int indicator = entry.granted() ? theme().color(ColorToken.ACCENT_SUCCESS)
                    : theme().color(ColorToken.ACCENT_DANGER);
                int inset = Math.min(4, row.height() / 2);
                context.graphics().fill(Math.max(row.x(), row.right() - 14), row.y() + inset,
                    Math.max(row.x(), row.right() - 4), row.bottom() - inset, indicator);
            }
        } finally {
            context.graphics().disableScissor();
        }
    }

    private int maxScroll() {
        long contentHeight = (long) entries.size() * rowHeight;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, contentHeight - bounds().height()));
    }

    @Override
    protected void onBoundsChanged(UIBounds previousBounds, UIBounds nextBounds) {
        scrollOffset = Math.min(scrollOffset, maxScroll());
    }
}
