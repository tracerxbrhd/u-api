package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

/** Bounded toast host. Expiry is handled in tick, never by rebuilding the screen tree. */
public final class UIToastManager extends UIComponent {
    private final Deque<ActiveToast> active = new ArrayDeque<>();
    private final int capacity;

    public UIToastManager(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Toast capacity must be positive");
        this.capacity = capacity;
    }

    public void post(UIToastNotification toast) {
        Objects.requireNonNull(toast, "toast");
        while (active.size() >= capacity) active.removeFirst();
        long now = System.nanoTime();
        long lifetime = toast.lifetime().toNanos();
        long expires = now > Long.MAX_VALUE - lifetime ? Long.MAX_VALUE : now + lifetime;
        active.addLast(new ActiveToast(toast, expires));
        invalidateRender();
    }

    public void dismiss(java.util.UUID id) {
        if (active.removeIf(toast -> toast.notification().id().equals(id))) invalidateRender();
    }

    @Override
    public void tick() {
        long now = System.nanoTime();
        if (active.removeIf(toast -> toast.expiresNanos() <= now)) invalidateRender();
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        if (bounds().width() <= 0 || bounds().height() <= 0) return;
        int y = bounds().y();
        Iterator<ActiveToast> iterator = active.descendingIterator();
        while (iterator.hasNext() && y < bounds().bottom()) {
            UIToastNotification toast = iterator.next().notification();
            int height = context.font().lineHeight + 10;
            int width = Math.min(bounds().width(), context.font().width(toast.message()) + 24);
            int x = bounds().right() - width;
            int accent = switch (toast.severity()) {
                case INFO -> theme().color(ColorToken.ACCENT_PRIMARY);
                case SUCCESS -> theme().color(ColorToken.ACCENT_SUCCESS);
                case WARNING -> theme().color(ColorToken.ACCENT_WARNING);
                case ERROR -> theme().color(ColorToken.ACCENT_DANGER);
            };
            context.graphics().fill(x, y, x + width, y + height, theme().color(ColorToken.BACKGROUND_PRIMARY));
            context.graphics().fill(x, y, x + 3, y + height, accent);
            context.graphics().drawString(context.font(), toast.message(), x + 8,
                y + (height - context.font().lineHeight) / 2, theme().color(ColorToken.TEXT_PRIMARY), false);
            y += height + 4;
        }
    }

    private record ActiveToast(UIToastNotification notification, long expiresNanos) {
    }
}
