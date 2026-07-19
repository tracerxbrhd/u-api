package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;

/** Compact reusable Party/Guild player summary. */
public final class UIPlayerCard extends UIComponent {
    private final UIPlayerHeadRenderer headRenderer;
    private UIPlayerCardModel model;

    public UIPlayerCard(UIPlayerCardModel model, UIPlayerHeadRenderer headRenderer) {
        this.model = Objects.requireNonNull(model, "model");
        this.headRenderer = Objects.requireNonNull(headRenderer, "headRenderer");
    }

    public void setModel(UIPlayerCardModel model) {
        this.model = Objects.requireNonNull(model, "model");
        invalidateRender();
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(),
            theme().color(ColorToken.BACKGROUND_PANEL));
        UIRenderPrimitives.border(context.graphics(), bounds(), theme().color(ColorToken.BORDER_DEFAULT));
        int padding = 4;
        int headSize = Math.max(0, Math.min(bounds().height() - padding * 2, 24));
        var headBounds = new dev.uapi.client.ui.core.UIBounds(bounds().x() + padding, bounds().y() + padding,
            headSize, headSize);
        if (headSize > 0) headRenderer.render(context, headBounds, model.playerId());
        int textX = headBounds.right() + 5;
        context.graphics().drawString(context.font(), model.displayName(), textX, bounds().y() + 4,
            theme().color(ColorToken.TEXT_PRIMARY), false);
        context.graphics().drawString(context.font(), model.status(), textX,
            bounds().y() + 5 + context.font().lineHeight,
            theme().color(ColorToken.TEXT_SECONDARY), false);
        int barY = bounds().bottom() - 4;
        int barWidth = Math.max(0, bounds().right() - textX - 4);
        context.graphics().fill(textX, barY - 2, textX + barWidth, barY,
            theme().color(ColorToken.BACKGROUND_SECONDARY));
        context.graphics().fill(textX, barY - 2,
            textX + (int) Math.round(barWidth * model.healthFraction()), barY,
            theme().color(ColorToken.ACCENT_SUCCESS));
    }
}
