package dev.uapi.api.permissions;

import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Stable, namespace-qualified permission identifier. */
public record PermissionKey(Identifier id) implements Comparable<PermissionKey> {
    public PermissionKey {
        Objects.requireNonNull(id, "id");
        if (id.getNamespace().isBlank() || id.getPath().isBlank()) {
            throw new IllegalArgumentException("Permission ID must have a namespace and path");
        }
    }

    public static PermissionKey parse(String value) {
        Objects.requireNonNull(value, "value");
        Identifier id = Identifier.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException("Invalid permission ID: " + value);
        }
        return new PermissionKey(id);
    }

    @Override
    public int compareTo(PermissionKey other) {
        Objects.requireNonNull(other, "other");
        return this.id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
