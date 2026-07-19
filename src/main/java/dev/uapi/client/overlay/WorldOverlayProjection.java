package dev.uapi.client.overlay;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

record WorldOverlayProjection(ResourceKey<Level> dimension, Vec3 cameraPosition,
                              Matrix4f modelView, Matrix4f projection) {
    ProjectedPoint project(Vec3 worldPosition, int guiWidth, int guiHeight, int margin) {
        guiWidth = Math.max(0, guiWidth);
        guiHeight = Math.max(0, guiHeight);
        int marginX = Math.max(0, Math.min(margin, guiWidth / 2));
        int marginY = Math.max(0, Math.min(margin, guiHeight / 2));
        Vec3 relative = worldPosition.subtract(cameraPosition);
        Vector4f clip = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1F)
            .mul(modelView)
            .mul(projection);
        boolean behind = clip.w <= 0.0001F;
        float divisor = behind ? Math.max(0.0001F, -clip.w) : clip.w;
        double ndcX = clip.x / divisor;
        double ndcY = clip.y / divisor;
        double ndcZ = clip.z / divisor;
        if (behind) {
            ndcX = -ndcX;
            ndcY = -ndcY;
        }
        double rawX = (ndcX * 0.5 + 0.5) * guiWidth;
        double rawY = (0.5 - ndcY * 0.5) * guiHeight;
        boolean finite = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(ndcZ);
        if (!finite) {
            rawX = guiWidth / 2.0;
            rawY = guiHeight / 2.0;
        }
        boolean outside = !finite || behind || ndcZ < -1 || ndcZ > 1
            || rawX < marginX || rawX > guiWidth - marginX
            || rawY < marginY || rawY > guiHeight - marginY;
        int x = (int) Math.round(Math.max(marginX, Math.min(guiWidth - marginX, rawX)));
        int y = (int) Math.round(Math.max(marginY, Math.min(guiHeight - marginY, rawY)));
        return new ProjectedPoint(x, y, outside);
    }

    record ProjectedPoint(int x, int y, boolean outsideScreen) {
    }
}
