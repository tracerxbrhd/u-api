package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIBounds;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;

public final class UIContextMenu extends UIComponent {
    public record Action(Component label, boolean enabled, Runnable callback) {
        public Action {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(callback, "callback");
        }
    }

    private final int rowHeight;
    private List<Action> actions;

    public UIContextMenu(List<Action> actions, int rowHeight) {
        this.actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        if (rowHeight <= 0) throw new IllegalArgumentException("Context menu row height must be positive");
        this.rowHeight = rowHeight;
    }

    public void setActions(List<Action> actions) {
        this.actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        invalidateLayout();
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        if (context.button() != 0) return false;
        int index = (int) ((context.mouseY() - bounds().y()) / rowHeight);
        if (index < 0 || index >= actions.size() || !actions.get(index).enabled()) return false;
        actions.get(index).callback().run();
        setVisible(false);
        return true;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(),
            theme().color(ColorToken.BACKGROUND_PRIMARY));
        UIRenderPrimitives.border(context.graphics(), bounds(), theme().color(ColorToken.BORDER_DEFAULT));
        for (int index = 0; index < actions.size(); index++) {
            int y = bounds().y() + index * rowHeight;
            if (y >= bounds().bottom()) break;
            Action action = actions.get(index);
            if (context.mouseX() >= bounds().x() && context.mouseX() < bounds().right()
                && context.mouseY() >= y && context.mouseY() < y + rowHeight) {
                context.graphics().fill(bounds().x() + 1, y, bounds().right() - 1, y + rowHeight,
                    theme().color(ColorToken.BACKGROUND_PANEL));
            }
            context.graphics().drawString(context.font(), action.label(), bounds().x() + 5,
                y + (rowHeight - context.font().lineHeight) / 2,
                theme().color(action.enabled() ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_MUTED), false);
        }
    }
}
