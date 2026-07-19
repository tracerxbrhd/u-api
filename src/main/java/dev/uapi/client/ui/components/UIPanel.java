package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;

/** A bordered retained surface whose colors come from the active semantic theme. */
public class UIPanel extends UIContainer {
    private final ColorToken backgroundToken;

    public UIPanel() {
        this(ColorToken.BACKGROUND_PRIMARY);
    }

    public UIPanel(ColorToken backgroundToken) {
        this.backgroundToken = Objects.requireNonNull(backgroundToken, "backgroundToken");
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(),
            theme().color(backgroundToken));
        UIRenderPrimitives.border(context.graphics(), bounds(), theme().color(ColorToken.BORDER_DEFAULT));
        super.renderComponent(context);
    }
}
