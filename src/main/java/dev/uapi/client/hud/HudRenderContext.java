package dev.uapi.client.hud;

import dev.uapi.client.ui.core.UIBounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Local element bounds begin at zero after placement scale has been applied. */
public record HudRenderContext(Minecraft minecraft, GuiGraphics graphics, DeltaTracker deltaTracker,
                               UIBounds bounds, float scale) {
}
