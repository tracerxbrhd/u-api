package dev.uapi.client.hud;

import net.minecraft.resources.ResourceLocation;

public interface HudReservedAreaRegistration extends AutoCloseable {
    ResourceLocation id();

    boolean isActive();

    @Override
    void close();
}
