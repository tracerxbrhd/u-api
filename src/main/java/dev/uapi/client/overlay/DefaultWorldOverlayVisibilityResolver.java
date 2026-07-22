package dev.uapi.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class DefaultWorldOverlayVisibilityResolver implements WorldOverlayVisibilityResolver {
    static final DefaultWorldOverlayVisibilityResolver INSTANCE = new DefaultWorldOverlayVisibilityResolver();

    private DefaultWorldOverlayVisibilityResolver() {
    }

    @Override
    public boolean isVisible(Minecraft minecraft, WorldOverlayMarker marker, Vec3 renderedPosition, double distance) {
        if (marker.occlusionMode() == OcclusionMode.THROUGH_WALLS) return true;
        if (minecraft.level == null || minecraft.player == null) return false;
        Vec3 camera = minecraft.player.getEyePosition();
        HitResult hit = minecraft.level.clip(new ClipContext(camera, renderedPosition,
            ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player));
        return hit.getType() == HitResult.Type.MISS
            || hit.getLocation().distanceToSqr(renderedPosition) <= 0.25;
    }
}
