package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class UIErrorState extends UIComponent {
    private final Component message;
    private final Component retryLabel;
    private final Runnable retry;

    public UIErrorState(Component message, Component retryLabel, Runnable retry) {
        this.message = Objects.requireNonNull(message, "message");
        this.retryLabel = Objects.requireNonNull(retryLabel, "retryLabel");
        this.retry = Objects.requireNonNull(retry, "retry");
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        if (!enabled() || context.button() != 0) return false;
        retry.run();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled()) return false;
        if (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER
            && keyCode != GLFW.GLFW_KEY_SPACE) return false;
        retry.run();
        return true;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        int centerX = bounds().x() + bounds().width() / 2;
        int y = bounds().y() + bounds().height() / 2 - context.font().lineHeight;
        context.graphics().drawCenteredString(context.font(), message, centerX, y,
            theme().color(ColorToken.ACCENT_DANGER));
        context.graphics().drawCenteredString(context.font(), retryLabel, centerX,
            y + context.font().lineHeight + 5, theme().color(ColorToken.TEXT_SECONDARY));
    }
}
