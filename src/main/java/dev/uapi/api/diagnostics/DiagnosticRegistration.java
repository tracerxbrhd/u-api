package dev.uapi.api.diagnostics;

import net.minecraft.resources.Identifier;

/** Lifecycle handle for a third-party diagnostic gauge. */
public interface DiagnosticRegistration extends AutoCloseable {
    Identifier id();

    boolean isActive();

    @Override
    void close();
}
