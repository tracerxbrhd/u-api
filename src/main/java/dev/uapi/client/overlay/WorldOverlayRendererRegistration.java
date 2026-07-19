package dev.uapi.client.overlay;

import net.minecraft.resources.ResourceLocation;

public interface WorldOverlayRendererRegistration extends AutoCloseable {
    ResourceLocation type();

    boolean isActive();

    @Override
    void close();
}
