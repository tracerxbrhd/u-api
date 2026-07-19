package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import java.util.Objects;
import net.minecraft.network.chat.Component;

public final class UIRoleBadge extends UIComponent {
    private Component label;
    private int accentColor;

    public UIRoleBadge(Component label, int accentColor) {
        this.label = Objects.requireNonNull(label, "label");
        this.accentColor = accentColor;
    }

    public void setRole(Component label, int accentColor) {
        this.label = Objects.requireNonNull(label, "label");
        this.accentColor = accentColor;
        invalidateRender();
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(), accentColor);
        context.graphics().drawString(context.font(), label, bounds().x() + 4,
            bounds().y() + (bounds().height() - context.font().lineHeight) / 2,
            theme().color(dev.uapi.client.ui.theme.UITheme.ColorToken.TEXT_PRIMARY), true);
    }
}
