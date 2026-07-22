package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import net.minecraft.network.chat.Component;

public final class UIEmptyState extends UIComponent {
    private final Component title;
    private final Component description;

    public UIEmptyState(Component title, Component description) {
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        int centerX = bounds().x() + bounds().width() / 2;
        int y = bounds().y() + bounds().height() / 2 - context.font().lineHeight;
        context.graphics().centeredText(context.font(), title, centerX, y, theme().color(ColorToken.TEXT_PRIMARY));
        context.graphics().centeredText(context.font(), description, centerX,
            y + context.font().lineHeight + 4, theme().color(ColorToken.TEXT_MUTED));
    }
}
