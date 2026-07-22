package dev.uapi.api.permissions;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/**
 * Immutable identity of one permission evaluation.
 *
 * <p>The action and resource are generic namespaced values; this contract has no knowledge of
 * guilds, parties, claims or any other implementation-specific model.</p>
 */
public record PermissionContext(
    UUID actorId,
    Optional<UUID> targetId,
    Identifier action,
    Optional<PermissionResource> resource
) {
    public PermissionContext {
        Objects.requireNonNull(actorId, "actorId");
        targetId = Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(action, "action");
        resource = Objects.requireNonNull(resource, "resource");
    }

    public static PermissionContext action(UUID actorId, Identifier action) {
        return new PermissionContext(actorId, Optional.empty(), action, Optional.empty());
    }

    public PermissionContext withTarget(UUID targetId) {
        return new PermissionContext(this.actorId, Optional.of(Objects.requireNonNull(targetId, "targetId")),
            this.action, this.resource);
    }

    public PermissionContext withResource(PermissionResource resource) {
        return new PermissionContext(this.actorId, this.targetId, this.action,
            Optional.of(Objects.requireNonNull(resource, "resource")));
    }
}
