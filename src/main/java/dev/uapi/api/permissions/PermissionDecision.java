package dev.uapi.api.permissions;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Server-authoritative permission result with a stable, machine-readable reason. */
public record PermissionDecision(boolean allowed, ResourceLocation reasonKey) {
    public PermissionDecision {
        Objects.requireNonNull(reasonKey, "reasonKey");
    }

    public static PermissionDecision allowed(ResourceLocation reasonKey) {
        return new PermissionDecision(true, reasonKey);
    }

    public static PermissionDecision denied(ResourceLocation reasonKey) {
        return new PermissionDecision(false, reasonKey);
    }
}
