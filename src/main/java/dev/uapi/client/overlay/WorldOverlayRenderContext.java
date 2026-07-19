package dev.uapi.client.overlay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public record WorldOverlayRenderContext(Minecraft minecraft, GuiGraphics graphics, Font font,
                                        DeltaTracker deltaTracker, int screenX, int screenY, int pixelSize,
                                        double distance, boolean edgeIndicator, WorldOverlayLod lod) {
}
