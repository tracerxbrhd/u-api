package dev.uapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class UApiTabButton extends AbstractButton {
    private static final int TAB_SPACING = 26;

    private final UApiScreenTabs.Tab tab;
    private final int barLeft;
    private final boolean selected;
    private final UApiTabSprites customSprites;
    private ItemStack itemIcon;
    private boolean tabWasVisible;
    private boolean pressed;

    UApiTabButton(int barLeft, int y, UApiScreenTabs.Tab tab, boolean selected, UApiTabSprites customSprites) {
        super(barLeft, y, 24, 24, tab.title());
        this.tab = tab;
        this.barLeft = barLeft;
        this.selected = selected;
        this.customSprites = customSprites;
        setTooltip(Tooltip.create(tab.title()));
        refreshPlacement();
    }

    @Override
    public void onPress() {
        if (selected || !refreshPlacement()) return;
        setFocused(false);
        var minecraft = Minecraft.getInstance();
        var target = tab.opener().apply(minecraft);
        if (target != null) minecraft.setScreen(target);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        pressed = true;
        super.onClick(mouseX, mouseY, button);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        pressed = false;
        super.onRelease(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return refreshPlacement() && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!refreshPlacement()) return;
        // Focus remains available to keyboard navigation, but is never reused as a persistent hover state.
        boolean hovered = isMouseOver(mouseX, mouseY);
        if (customSprites == null) {
            graphics.blitSprite(SPRITES.get(active, hovered), getX(), getY(), getWidth(), getHeight());
        } else {
            var sprite = !active ? customSprites.disabled() : selected ? customSprites.selected()
                : pressed ? customSprites.pressed() : hovered ? customSprites.hovered() : customSprites.normal();
            graphics.blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
        }
        if (tab.textureIcon() != null) {
            graphics.blit(tab.textureIcon(), getX() + 4, getY() + 4, 0, 0, 16, 16, 16, 16);
        } else if (itemIcon != null) {
            graphics.renderItem(itemIcon, getX() + 4, getY() + 4);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (refreshPlacement()) defaultButtonNarrationText(output);
    }

    private boolean refreshPlacement() {
        int visibleIndex = UApiScreenTabs.visibleIndex(tab);
        if (visibleIndex < 0) {
            active = false;
            tabWasVisible = false;
            pressed = false;
            setFocused(false);
            return false;
        }
        active = true;
        setX(barLeft + visibleIndex * TAB_SPACING);
        if (!tabWasVisible) {
            itemIcon = tab.itemIcon() == null ? null : tab.itemIcon().get();
            tabWasVisible = true;
        }
        return true;
    }
}
