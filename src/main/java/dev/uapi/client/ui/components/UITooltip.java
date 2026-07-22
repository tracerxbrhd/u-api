package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Hover tooltip with bounded on-screen placement and no native widget allocation. */
public final class UITooltip extends UIComponent {
    private Component text;

    public UITooltip(Component text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    public void setText(Component text) {
        this.text = Objects.requireNonNull(text, "text");
        invalidateRender();
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        if (!bounds().contains(context.mouseX(), context.mouseY())) return;
        int width = context.font().width(text) + 8;
        int height = context.font().lineHeight + 6;
        int x = Math.max(2, Math.min(context.graphics().guiWidth() - width - 2, context.mouseX() + 10));
        int y = Math.max(2, Math.min(context.graphics().guiHeight() - height - 2, context.mouseY() + 10));
        context.graphics().fill(x, y, x + width, y + height, theme().color(ColorToken.BACKGROUND_PRIMARY));
        UIRenderPrimitives.border(context.graphics(), new UIBounds(x, y, width, height),
            theme().color(ColorToken.BORDER_DEFAULT));
        context.graphics().text(context.font(), text, x + 4, y + 3,
            theme().color(ColorToken.TEXT_PRIMARY), false);
    }
}
