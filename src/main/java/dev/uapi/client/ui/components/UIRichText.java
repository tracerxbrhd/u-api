package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Wrapped immutable Component text; splitting is recomputed only when width or value changes. */
public final class UIRichText extends UIComponent {
    private Component text;
    private int color;
    private ColorToken colorToken;
    private int cachedWidth = -1;
    private net.minecraft.client.gui.Font cachedFont;
    private java.util.List<net.minecraft.util.FormattedCharSequence> cachedLines = java.util.List.of();

    public UIRichText(Component text, int color) {
        this.text = Objects.requireNonNull(text, "text");
        this.color = color;
    }

    public UIRichText(Component text, ColorToken colorToken) {
        this.text = Objects.requireNonNull(text, "text");
        this.colorToken = Objects.requireNonNull(colorToken, "colorToken");
    }

    public void setText(Component text) {
        text = Objects.requireNonNull(text, "text");
        if (this.text.equals(text)) return;
        this.text = text;
        cachedWidth = -1;
        invalidateLayout();
    }

    public void setColor(int color) {
        if (colorToken == null && this.color == color) return;
        colorToken = null;
        this.color = color;
        invalidateRender();
    }

    public void setColor(ColorToken colorToken) {
        colorToken = Objects.requireNonNull(colorToken, "colorToken");
        if (this.colorToken == colorToken) return;
        this.colorToken = colorToken;
        invalidateRender();
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        int width = Math.max(1, bounds().width());
        if (cachedWidth != width || cachedFont != context.font()) {
            cachedLines = context.font().split(text, width);
            cachedWidth = width;
            cachedFont = context.font();
        }
        int y = bounds().y();
        int resolvedColor = colorToken == null ? color : theme().color(colorToken);
        for (var line : cachedLines) {
            if (y + context.font().lineHeight > bounds().bottom()) break;
            context.graphics().text(context.font(), line, bounds().x(), y, resolvedColor, false);
            y += context.font().lineHeight + 1;
        }
    }
}
