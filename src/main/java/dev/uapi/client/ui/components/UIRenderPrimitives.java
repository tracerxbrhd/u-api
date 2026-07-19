package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIBounds;
import net.minecraft.client.gui.GuiGraphics;

final class UIRenderPrimitives {
    private UIRenderPrimitives() {
    }

    static void border(GuiGraphics graphics, UIBounds bounds, int color) {
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, color);
        graphics.fill(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), color);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.bottom(), color);
        graphics.fill(bounds.right() - 1, bounds.y(), bounds.right(), bounds.bottom(), color);
    }
}
