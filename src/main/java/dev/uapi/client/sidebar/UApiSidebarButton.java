package dev.uapi.client.sidebar;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public final class UApiSidebarButton extends AbstractButton {
    private final UApiSidebarButtons.ButtonDefinition definition;
    private boolean lastAllowed;

    public UApiSidebarButton(int x, int y, int size, UApiSidebarButtons.ButtonDefinition definition) {
        super(x, y, size, size, definition.title());
        this.definition = definition;
        this.lastAllowed = UApiSidebarButtons.isAllowed(definition);
        this.active = lastAllowed;
        setTooltip(Tooltip.create(UApiSidebarButtons.tooltip(definition)));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        setFocused(false);
        refreshPermissionState();
        UApiSidebarButtons.execute(definition);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        refreshPermissionState();
        boolean hovered = isMouseOver(mouseX, mouseY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(active, hovered),
            getX(), getY(), getWidth(), getHeight());
        graphics.item(definition.icon(), getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2);
    }

    private void refreshPermissionState() {
        boolean allowed = UApiSidebarButtons.isAllowed(definition);
        if (allowed == lastAllowed) return;
        lastAllowed = allowed;
        active = allowed;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
