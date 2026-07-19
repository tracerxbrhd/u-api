package dev.uapi.client.ui.components;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.chat.Component;

public record UIToastNotification(UUID id, Component message, Severity severity, Duration lifetime) {
    public enum Severity { INFO, SUCCESS, WARNING, ERROR }

    public UIToastNotification {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(lifetime, "lifetime");
        if (lifetime.isNegative() || lifetime.isZero()) throw new IllegalArgumentException("Toast lifetime must be positive");
        if (lifetime.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Toast lifetime cannot exceed one day");
        }
    }

    public static UIToastNotification create(Component message, Severity severity, Duration lifetime) {
        return new UIToastNotification(UUID.randomUUID(), message, severity, lifetime);
    }
}
