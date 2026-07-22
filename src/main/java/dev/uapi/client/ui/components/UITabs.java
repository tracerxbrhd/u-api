package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class UITabs<T> extends UIComponent {
    public record Tab<T>(T id, Component label) {
        public Tab {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(label, "label");
        }
    }

    private final List<Tab<T>> tabs;
    private final Consumer<T> onSelected;
    private int selectedIndex;

    public UITabs(List<Tab<T>> tabs, T selected, Consumer<T> onSelected) {
        this.tabs = List.copyOf(Objects.requireNonNull(tabs, "tabs"));
        if (this.tabs.isEmpty()) throw new IllegalArgumentException("Tabs require at least one entry");
        this.selectedIndex = findIndex(Objects.requireNonNull(selected, "selected"));
        this.onSelected = Objects.requireNonNull(onSelected, "onSelected");
    }

    public T selected() {
        return tabs.get(selectedIndex).id();
    }

    public void select(T id) {
        int next = findIndex(Objects.requireNonNull(id, "id"));
        if (next == selectedIndex) return;
        selectedIndex = next;
        onSelected.accept(id);
        invalidateRender();
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        if (!enabled() || context.button() != 0) return false;
        requestFocus();
        int index = Math.min(tabs.size() - 1,
            Math.max(0, (int) ((context.mouseX() - bounds().x()) * tabs.size() / Math.max(1, bounds().width()))));
        select(tabs.get(index).id());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled()) return false;
        int direction = switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> -1;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_DOWN -> 1;
            default -> 0;
        };
        if (direction == 0) return false;
        select(tabs.get(Math.floorMod(selectedIndex + direction, tabs.size())).id());
        return true;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        int x = bounds().x();
        for (int index = 0; index < tabs.size(); index++) {
            int width = bounds().width() / tabs.size() + (index < bounds().width() % tabs.size() ? 1 : 0);
            int background = theme().color(index == selectedIndex
                ? ColorToken.BACKGROUND_PANEL : ColorToken.BACKGROUND_SECONDARY);
            context.graphics().fill(x, bounds().y(), x + width, bounds().bottom(), background);
            if (index == selectedIndex) context.graphics().fill(x, bounds().bottom() - 2, x + width, bounds().bottom(),
                theme().color(ColorToken.ACCENT_PRIMARY));
            Component label = tabs.get(index).label();
            context.graphics().text(context.font(), label,
                x + (width - context.font().width(label)) / 2,
                bounds().y() + (bounds().height() - context.font().lineHeight) / 2,
                theme().color(index == selectedIndex ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_SECONDARY), false);
            x += width;
        }
        UIRenderPrimitives.border(context.graphics(), bounds(), theme().color(ColorToken.BORDER_DEFAULT));
    }

    private int findIndex(T id) {
        for (int index = 0; index < tabs.size(); index++) if (tabs.get(index).id().equals(id)) return index;
        throw new IllegalArgumentException("Unknown tab: " + id);
    }
}
