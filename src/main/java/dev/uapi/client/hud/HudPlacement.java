package dev.uapi.client.hud;

import java.util.Objects;

/** Client-overridable placement. Scale is bounded to keep collision layout predictable. */
public record HudPlacement(HudAnchor anchor, int offsetX, int offsetY, float scale, boolean visible, int priority) {
    public HudPlacement {
        Objects.requireNonNull(anchor, "anchor");
        if (!Float.isFinite(scale) || scale < 0.25F || scale > 4F) {
            throw new IllegalArgumentException("HUD scale must be within 0.25..4.0");
        }
    }

    public static HudPlacement at(HudAnchor anchor) {
        return new HudPlacement(anchor, 0, 0, 1F, true, 0);
    }
}
