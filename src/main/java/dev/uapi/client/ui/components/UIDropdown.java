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

/** Compact dropdown: left/right click selects the next/previous retained option. */
public final class UIDropdown<T> extends UIComponent {
    public record Option<T>(T value, Component label) {
        public Option {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(label, "label");
        }
    }

    private final List<Option<T>> options;
    private final Consumer<T> onChanged;
    private int selectedIndex;

    public UIDropdown(List<Option<T>> options, int selectedIndex, Consumer<T> onChanged) {
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        if (this.options.isEmpty()) throw new IllegalArgumentException("Dropdown requires at least one option");
        if (selectedIndex < 0 || selectedIndex >= this.options.size()) {
            throw new IndexOutOfBoundsException("Selected dropdown option is out of range");
        }
        this.selectedIndex = selectedIndex;
        this.onChanged = Objects.requireNonNull(onChanged, "onChanged");
    }

    public T value() {
        return options.get(selectedIndex).value();
    }

    public void select(int index) {
        if (index < 0 || index >= options.size()) throw new IndexOutOfBoundsException(index);
        if (selectedIndex == index) return;
        selectedIndex = index;
        onChanged.accept(value());
        invalidateRender();
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        if (!enabled() || context.button() != 0 && context.button() != 1) return false;
        requestFocus();
        int direction = context.button() == 0 ? 1 : -1;
        select(Math.floorMod(selectedIndex + direction, options.size()));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled()) return false;
        int direction = switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> -1;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_ENTER,
                 GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> 1;
            default -> 0;
        };
        if (direction == 0) return false;
        select(Math.floorMod(selectedIndex + direction, options.size()));
        return true;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(),
            theme().color(ColorToken.BACKGROUND_PANEL));
        UIRenderPrimitives.border(context.graphics(), bounds(),
            theme().color(focused() ? ColorToken.BORDER_FOCUSED : ColorToken.BORDER_DEFAULT));
        Component label = options.get(selectedIndex).label();
        int y = bounds().y() + (bounds().height() - context.font().lineHeight) / 2;
        context.graphics().text(context.font(), label, bounds().x() + 4, y,
            theme().color(enabled() ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_MUTED), false);
        context.graphics().text(context.font(), Component.literal("▾"), bounds().right() - 10, y,
            theme().color(ColorToken.TEXT_SECONDARY), false);
    }
}
