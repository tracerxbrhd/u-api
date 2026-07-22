package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class UICheckbox extends UIComponent {
    private final Component label;
    private final Consumer<Boolean> onChanged;
    private boolean checked;

    public UICheckbox(Component label, boolean checked, Consumer<Boolean> onChanged) {
        this.label = Objects.requireNonNull(label, "label");
        this.checked = checked;
        this.onChanged = Objects.requireNonNull(onChanged, "onChanged");
    }

    public boolean checked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked == checked) return;
        this.checked = checked;
        onChanged.accept(checked);
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
        setChecked(!checked);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled() || keyCode != GLFW.GLFW_KEY_SPACE) return false;
        setChecked(!checked);
        return true;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        int size = Math.min(12, bounds().height());
        if (size <= 0 || bounds().width() <= 0) return;
        int y = bounds().y() + (bounds().height() - size) / 2;
        context.graphics().fill(bounds().x(), y, bounds().x() + size, y + size,
            theme().color(ColorToken.BACKGROUND_SECONDARY));
        UIRenderPrimitives.border(context.graphics(),
            new dev.uapi.client.ui.core.UIBounds(bounds().x(), y, size, size),
            theme().color(focused() ? ColorToken.BORDER_FOCUSED : ColorToken.BORDER_DEFAULT));
        if (checked && size >= 7) context.graphics().fill(bounds().x() + 3, y + 3, bounds().x() + size - 3, y + size - 3,
            theme().color(ColorToken.ACCENT_SUCCESS));
        context.graphics().text(context.font(), label, bounds().x() + size + 5,
            bounds().y() + (bounds().height() - context.font().lineHeight) / 2,
            theme().color(enabled() ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_MUTED), false);
    }
}
