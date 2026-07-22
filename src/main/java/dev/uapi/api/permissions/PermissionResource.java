package dev.uapi.api.permissions;

import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Generic logical resource involved in a permission check. */
public record PermissionResource(Identifier type, String identifier) {
    public PermissionResource {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(identifier, "identifier");
        identifier = identifier.trim();
        if (identifier.isEmpty()) {
            throw new IllegalArgumentException("Permission resource identifier cannot be blank");
        }
    }
}
