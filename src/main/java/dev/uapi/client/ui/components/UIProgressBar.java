package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import net.minecraft.network.chat.Component;

public final class UIProgressBar extends UIComponent {
    private double progress;
    private Component label;

    public UIProgressBar(double progress, Component label) {
        this.progress = clamp(progress);
        this.label = Objects.requireNonNull(label, "label");
    }

    public double progress() {
        return progress;
    }

    public void setProgress(double progress) {
        double next = clamp(progress);
        if (Double.compare(this.progress, next) == 0) return;
        this.progress = next;
        invalidateRender();
    }

    public void setLabel(Component label) {
        this.label = Objects.requireNonNull(label, "label");
        invalidateRender();
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(),
            theme().color(ColorToken.BACKGROUND_SECONDARY));
        int fill = (int) Math.round(bounds().width() * progress);
        context.graphics().fill(bounds().x(), bounds().y(), bounds().x() + fill, bounds().bottom(),
            theme().color(ColorToken.ACCENT_SUCCESS));
        UIRenderPrimitives.border(context.graphics(), bounds(), theme().color(ColorToken.BORDER_DEFAULT));
        context.graphics().text(context.font(), label,
            bounds().x() + (bounds().width() - context.font().width(label)) / 2,
            bounds().y() + (bounds().height() - context.font().lineHeight) / 2,
            theme().color(ColorToken.TEXT_PRIMARY), true);
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0;
        return Math.max(0, Math.min(1, value));
    }
}
