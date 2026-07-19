package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Retained button with no per-frame widget allocation. */
public class UIButton extends UIComponent {
    private Component label;
    private Runnable action;

    public UIButton(Component label, Runnable action) {
        this.label = Objects.requireNonNull(label, "label");
        this.action = Objects.requireNonNull(action, "action");
    }

    public Component label() {
        return label;
    }

    public void setLabel(Component label) {
        label = Objects.requireNonNull(label, "label");
        if (this.label.equals(label)) return;
        this.label = label;
        invalidateLayout();
    }

    public void setAction(Runnable action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        if (!enabled() || context.button() != 0) return false;
        requestFocus();
        action.run();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled()) return false;
        if (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER
            && keyCode != GLFW.GLFW_KEY_SPACE) return false;
        action.run();
        return true;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        boolean hovered = bounds().contains(context.mouseX(), context.mouseY());
        int background = enabled()
            ? theme().color(hovered ? ColorToken.ACCENT_PRIMARY : ColorToken.BACKGROUND_PANEL)
            : theme().color(ColorToken.BACKGROUND_SECONDARY);
        int border = theme().color(focused() ? ColorToken.BORDER_FOCUSED : ColorToken.BORDER_DEFAULT);
        int text = theme().color(enabled() ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_MUTED);
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(), background);
        UIRenderPrimitives.border(context.graphics(), bounds(), border);
        if (drawLabel()) {
            int x = bounds().x() + (bounds().width() - context.font().width(label)) / 2;
            int y = bounds().y() + (bounds().height() - context.font().lineHeight) / 2;
            context.graphics().drawString(context.font(), label, x, y, text, false);
        }
    }

    protected boolean drawLabel() {
        return true;
    }
}
