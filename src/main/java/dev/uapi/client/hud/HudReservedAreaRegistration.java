package dev.uapi.client.hud;

import net.minecraft.resources.Identifier;

public interface HudReservedAreaRegistration extends AutoCloseable {
    Identifier id();

    boolean isActive();

    @Override
    void close();
}
