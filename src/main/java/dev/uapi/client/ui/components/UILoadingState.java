package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import net.minecraft.network.chat.Component;

public final class UILoadingState extends UIComponent {
    private final Component label;
    private final Component[] frames = new Component[4];

    public UILoadingState(Component label) {
        this.label = Objects.requireNonNull(label, "label");
        for (int index = 0; index < frames.length; index++) {
            frames[index] = Component.empty().append(label).append(Component.literal(".".repeat(index)));
        }
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        int dots = (int) (System.currentTimeMillis() / 350 % 4);
        Component text = frames[dots];
        context.graphics().centeredText(context.font(), text,
            bounds().x() + bounds().width() / 2,
            bounds().y() + (bounds().height() - context.font().lineHeight) / 2,
            theme().color(ColorToken.TEXT_SECONDARY));
    }
}
