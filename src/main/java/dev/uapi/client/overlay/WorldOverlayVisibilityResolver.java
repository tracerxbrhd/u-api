package dev.uapi.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface WorldOverlayVisibilityResolver {
    boolean isVisible(Minecraft minecraft, WorldOverlayMarker marker, Vec3 renderedPosition, double distance);
}
