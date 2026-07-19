package dev.uapi.client.hud;

import dev.uapi.client.ui.core.UIBounds;
import java.util.Collection;

/** Dynamic areas used by layout to avoid vanilla or another mod's HUD. */
@FunctionalInterface
public interface HudReservedAreaProvider {
    Collection<UIBounds> reservedAreas(int guiWidth, int guiHeight);
}
