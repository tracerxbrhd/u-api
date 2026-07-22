package dev.uapi.client.ui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Per-frame rendering inputs. Components must not retain this object. */
public record UIRenderContext(Minecraft minecraft, GuiGraphicsExtractor graphics, Font font,
                              int mouseX, int mouseY, float partialTick) {
}
