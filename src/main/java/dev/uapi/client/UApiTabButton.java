package dev.uapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class UApiTabButton extends AbstractButton {
    private final UApiScreenTabs.Tab tab;
    private final ItemStack itemIcon;
    private final boolean selected;
    private final UApiTabSprites customSprites;
    private boolean pressed;

    UApiTabButton(int x, int y, UApiScreenTabs.Tab tab, boolean selected, UApiTabSprites customSprites) {
        super(x, y, 24, 24, tab.title());
        this.tab = tab;
        this.itemIcon = tab.itemIcon() == null ? null : tab.itemIcon().get();
        this.selected = selected;
        this.customSprites = customSprites;
        setTooltip(Tooltip.create(tab.title()));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (selected) return;
        setFocused(false);
        var minecraft = Minecraft.getInstance();
        var target = tab.opener().apply(minecraft);
        if (target != null) minecraft.setScreenAndShow(target);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubledClick) {
        pressed = true;
        super.onClick(event, doubledClick);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        pressed = false;
        super.onRelease(event);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Focus remains available to keyboard navigation, but is never reused as a persistent hover state.
        boolean hovered = isMouseOver(mouseX, mouseY);
        if (customSprites == null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(active, hovered),
                getX(), getY(), getWidth(), getHeight());
        } else {
            var sprite = !active ? customSprites.disabled() : selected ? customSprites.selected()
                : pressed ? customSprites.pressed() : hovered ? customSprites.hovered() : customSprites.normal();
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), getWidth(), getHeight());
        }
        if (tab.textureIcon() != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, tab.textureIcon(),
                getX() + 4, getY() + 4, 0, 0, 16, 16, 16, 16);
        } else if (itemIcon != null) {
            graphics.item(itemIcon, getX() + 4, getY() + 4);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
