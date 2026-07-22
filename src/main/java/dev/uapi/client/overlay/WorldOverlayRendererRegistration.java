package dev.uapi.client.overlay;

import net.minecraft.resources.Identifier;

public interface WorldOverlayRendererRegistration extends AutoCloseable {
    Identifier type();

    boolean isActive();

    @Override
    void close();
}
