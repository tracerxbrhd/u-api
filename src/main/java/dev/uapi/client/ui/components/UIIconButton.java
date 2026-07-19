package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIRenderContext;
import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Button whose icon renderer is retained and invoked inside a padded square. */
public final class UIIconButton extends UIButton {
    private final UIIconRenderer icon;

    public UIIconButton(Component accessibleLabel, UIIconRenderer icon, Runnable action) {
        super(accessibleLabel, action);
        this.icon = Objects.requireNonNull(icon, "icon");
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        super.renderComponent(context);
        icon.render(context, bounds().inset(4), enabled());
    }

    @Override
    protected boolean drawLabel() {
        return false;
    }
}
