package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;

/** Full-screen modal layer with an isolated retained content container. */
public class UIModal extends UIContainer {
    private final UIContainer content = add(new UIContainer());
    private final int panelWidth;
    private final int panelHeight;
    private boolean dismissOnBackdrop;

    public UIModal(int panelWidth, int panelHeight) {
        if (panelWidth <= 0 || panelHeight <= 0) throw new IllegalArgumentException("Modal panel size must be positive");
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        setLayout((container, bounds) -> content.setBounds(panelBounds(bounds)));
    }

    public UIContainer content() {
        return content;
    }

    public void setDismissOnBackdrop(boolean dismissOnBackdrop) {
        this.dismissOnBackdrop = dismissOnBackdrop;
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        if (content.bounds().contains(context.mouseX(), context.mouseY())) {
            super.mouseClicked(context);
            return true;
        }
        if (context.button() == 0 && dismissOnBackdrop) setVisible(false);
        return true;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(), 0x99000000);
        UIBounds panel = content.bounds();
        context.graphics().fill(panel.x(), panel.y(), panel.right(), panel.bottom(),
            theme().color(ColorToken.BACKGROUND_PRIMARY));
        UIRenderPrimitives.border(context.graphics(), panel, theme().color(ColorToken.BORDER_DEFAULT));
        super.renderComponent(context);
    }

    private UIBounds panelBounds(UIBounds bounds) {
        int width = Math.min(panelWidth, Math.max(0, bounds.width() - 8));
        int height = Math.min(panelHeight, Math.max(0, bounds.height() - 8));
        return new UIBounds(bounds.x() + (bounds.width() - width) / 2,
            bounds.y() + (bounds.height() - height) / 2, width, height);
    }
}
