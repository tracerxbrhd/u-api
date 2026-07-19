package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/** Retained text label. Its component value is reused until explicitly changed. */
public final class UILabel extends UIComponent {
    private Component text;
    private int color;
    private ColorToken colorToken;
    private boolean shadow;

    public UILabel(Component text, int color) {
        this.text = Objects.requireNonNull(text, "text");
        this.color = color;
    }

    public UILabel(Component text, ColorToken colorToken) {
        this.text = Objects.requireNonNull(text, "text");
        this.colorToken = Objects.requireNonNull(colorToken, "colorToken");
    }

    public Component text() {
        return text;
    }

    public void setText(Component text) {
        text = Objects.requireNonNull(text, "text");
        if (this.text.equals(text)) return;
        this.text = text;
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

    public void setShadow(boolean shadow) {
        if (this.shadow == shadow) return;
        this.shadow = shadow;
        invalidateRender();
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        int resolvedColor = colorToken == null ? color : theme().color(colorToken);
        context.graphics().drawString(context.font(), text, bounds().x(), bounds().y(), resolvedColor, shadow);
    }
}
