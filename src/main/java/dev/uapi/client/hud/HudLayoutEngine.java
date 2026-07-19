package dev.uapi.client.hud;

import dev.uapi.client.ui.core.UIBounds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class HudLayoutEngine {
    private static final int MARGIN = 6;
    private static final int GAP = 4;

    private HudLayoutEngine() {
    }

    static List<PlacedElement> layout(List<UApiHud.Entry> entries, Collection<UIBounds> reserved,
                                      int guiWidth, int guiHeight) {
        guiWidth = Math.max(0, guiWidth);
        guiHeight = Math.max(0, guiHeight);
        List<UIBounds> occupied = new ArrayList<>(reserved);
        List<PlacedElement> result = new ArrayList<>(entries.size());
        for (UApiHud.Entry entry : entries) {
            HudPlacement placement = entry.placement();
            if (!placement.visible()) continue;
            int width = scaledSize(entry.width(), placement.scale(), guiWidth);
            int height = scaledSize(entry.height(), placement.scale(), guiHeight);
            UIBounds candidate = anchored(placement, width, height, guiWidth, guiHeight);
            candidate = avoid(candidate, placement.anchor(), occupied, guiWidth, guiHeight);
            occupied.add(candidate);
            result.add(new PlacedElement(entry, candidate));
        }
        return List.copyOf(result);
    }

    private static UIBounds anchored(HudPlacement placement, int width, int height, int guiWidth, int guiHeight) {
        int x = switch (placement.anchor()) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> (guiWidth - width) / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> guiWidth - MARGIN - width;
        };
        int y = switch (placement.anchor()) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> MARGIN;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> (guiHeight - height) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> guiHeight - MARGIN - height;
        };
        return clamp(new UIBounds(saturatedAdd(x, placement.offsetX()), saturatedAdd(y, placement.offsetY()),
            width, height), guiWidth, guiHeight);
    }

    private static UIBounds avoid(UIBounds candidate, HudAnchor anchor, List<UIBounds> occupied,
                                  int guiWidth, int guiHeight) {
        int attempts = 0;
        while (intersectsAny(candidate, occupied) && attempts++ < 64) {
            int direction = switch (anchor) {
                case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> -1;
                default -> 1;
            };
            candidate = clamp(candidate.translate(0, direction * (candidate.height() + GAP)), guiWidth, guiHeight);
        }
        return candidate;
    }

    private static boolean intersectsAny(UIBounds candidate, List<UIBounds> occupied) {
        for (UIBounds bounds : occupied) if (candidate.intersects(bounds)) return true;
        return false;
    }

    private static UIBounds clamp(UIBounds bounds, int guiWidth, int guiHeight) {
        int x = Math.max(0, Math.min(Math.max(0, guiWidth - bounds.width()), bounds.x()));
        int y = Math.max(0, Math.min(Math.max(0, guiHeight - bounds.height()), bounds.y()));
        return new UIBounds(x, y, Math.min(bounds.width(), guiWidth), Math.min(bounds.height(), guiHeight));
    }

    private static int scaledSize(int logicalSize, float scale, int screenSize) {
        long scaled = Math.max(1L, Math.round(logicalSize * (double) scale));
        return (int) Math.min(Math.max(0, screenSize), scaled);
    }

    private static int saturatedAdd(int left, int right) {
        long result = (long) left + right;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, result));
    }

    record PlacedElement(UApiHud.Entry entry, UIBounds bounds) {
    }
}
