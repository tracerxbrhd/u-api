package dev.uapi.api.permissions;

import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Server-authoritative permission result with a stable, machine-readable reason. */
public record PermissionDecision(boolean allowed, Identifier reasonKey) {
    public PermissionDecision {
        Objects.requireNonNull(reasonKey, "reasonKey");
    }

    public static PermissionDecision allowed(Identifier reasonKey) {
        return new PermissionDecision(true, reasonKey);
    }

    public static PermissionDecision denied(Identifier reasonKey) {
        return new PermissionDecision(false, reasonKey);
    }
}
