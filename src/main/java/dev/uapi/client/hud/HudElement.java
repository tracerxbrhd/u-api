package dev.uapi.client.hud;

import net.minecraft.resources.Identifier;

/** Public registration contract for Party, dungeon, ready-check and notification HUD elements. */
public interface HudElement {
    Identifier id();

    int width();

    int height();

    default HudPlacement defaultPlacement() {
        return HudPlacement.at(HudAnchor.TOP_LEFT);
    }

    /** Runtime visibility predicate; invisible elements do not reserve layout space. */
    default boolean visible(HudTickContext context) {
        return true;
    }

    default void tick(HudTickContext context) {
    }

    void render(HudRenderContext context);
}
