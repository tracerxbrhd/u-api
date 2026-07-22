package dev.uapi.client.overlay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record WorldOverlayRenderContext(Minecraft minecraft, GuiGraphicsExtractor graphics, Font font,
                                        DeltaTracker deltaTracker, int screenX, int screenY, int pixelSize,
                                        double distance, boolean edgeIndicator, WorldOverlayLod lod) {
}
