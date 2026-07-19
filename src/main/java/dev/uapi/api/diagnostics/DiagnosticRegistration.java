package dev.uapi.api.diagnostics;

import net.minecraft.resources.ResourceLocation;

/** Lifecycle handle for a third-party diagnostic gauge. */
public interface DiagnosticRegistration extends AutoCloseable {
    ResourceLocation id();

    boolean isActive();

    @Override
    void close();
}
