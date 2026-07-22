package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;

/** Lightweight single-line retained text input. Server requests belong in the change/submit handler. */
public class UITextField extends UIComponent {
    private final int maxLength;
    private Component placeholder;
    private Consumer<String> onChanged;
    private Consumer<String> onSubmitted = ignored -> {};
    private String value = "";
    private int cursor;

    public UITextField(Component placeholder, int maxLength, Consumer<String> onChanged) {
        this.placeholder = Objects.requireNonNull(placeholder, "placeholder");
        if (maxLength <= 0) throw new IllegalArgumentException("Text field maxLength must be positive");
        this.maxLength = maxLength;
        this.onChanged = Objects.requireNonNull(onChanged, "onChanged");
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        value = Objects.requireNonNull(value, "value");
        if (value.length() > maxLength) value = value.substring(0, maxLength);
        if (this.value.equals(value)) return;
        this.value = value;
        cursor = Math.min(cursor, value.length());
        onChanged.accept(value);
        invalidateRender();
    }

    public void setOnSubmitted(Consumer<String> onSubmitted) {
        this.onSubmitted = Objects.requireNonNull(onSubmitted, "onSubmitted");
    }

    public void setPlaceholder(Component placeholder) {
        this.placeholder = Objects.requireNonNull(placeholder, "placeholder");
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
        cursor = value.length();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled()) return false;
        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> { deleteBeforeCursor(); yield true; }
            case GLFW.GLFW_KEY_DELETE -> { deleteAtCursor(); yield true; }
            case GLFW.GLFW_KEY_LEFT -> { cursor = Math.max(0, cursor - 1); invalidateRender(); yield true; }
            case GLFW.GLFW_KEY_RIGHT -> { cursor = Math.min(value.length(), cursor + 1); invalidateRender(); yield true; }
            case GLFW.GLFW_KEY_HOME -> { cursor = 0; invalidateRender(); yield true; }
            case GLFW.GLFW_KEY_END -> { cursor = value.length(); invalidateRender(); yield true; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { onSubmitted.accept(value); clearFocus(); yield true; }
            case GLFW.GLFW_KEY_ESCAPE -> { clearFocus(); yield true; }
            default -> false;
        };
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!focused() || !enabled() || value.length() >= maxLength
            || !StringUtil.isAllowedChatCharacter(codePoint)) return false;
        updateValue(value.substring(0, cursor) + codePoint + value.substring(cursor));
        cursor++;
        return true;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        if (bounds().width() <= 0 || bounds().height() <= 0) return;
        int background = theme().color(ColorToken.BACKGROUND_SECONDARY);
        int border = theme().color(focused() ? ColorToken.BORDER_FOCUSED : ColorToken.BORDER_DEFAULT);
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(), background);
        UIRenderPrimitives.border(context.graphics(), bounds(), border);
        Component shown = value.isEmpty() ? placeholder : Component.literal(value);
        int color = theme().color(value.isEmpty() ? ColorToken.TEXT_MUTED : ColorToken.TEXT_PRIMARY);
        int x = bounds().x() + 4;
        int y = bounds().y() + (bounds().height() - context.font().lineHeight) / 2;
        int scissorLeft = Math.min(bounds().right(), bounds().x() + 2);
        int scissorRight = Math.max(scissorLeft, bounds().right() - 2);
        context.graphics().enableScissor(scissorLeft, bounds().y(), scissorRight, bounds().bottom());
        try {
            context.graphics().text(context.font(), shown, x, y, color, false);
            if (focused() && (System.currentTimeMillis() / 500 & 1) == 0) {
                int cursorX = x + context.font().width(value.substring(0, cursor));
                context.graphics().fill(cursorX, y - 1, cursorX + 1, y + context.font().lineHeight + 1,
                    theme().color(ColorToken.TEXT_PRIMARY));
            }
        } finally {
            context.graphics().disableScissor();
        }
    }

    private void deleteBeforeCursor() {
        if (cursor == 0) return;
        updateValue(value.substring(0, cursor - 1) + value.substring(cursor));
        cursor--;
    }

    private void deleteAtCursor() {
        if (cursor >= value.length()) return;
        updateValue(value.substring(0, cursor) + value.substring(cursor + 1));
    }

    private void updateValue(String next) {
        value = next;
        onChanged.accept(next);
        invalidateRender();
    }
}
