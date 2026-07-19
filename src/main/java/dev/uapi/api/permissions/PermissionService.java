package dev.uapi.api.permissions;

import dev.uapi.api.services.UApiService;
import java.util.Set;
import java.util.UUID;

/** Neutral server-authoritative permission service contract. */
public interface PermissionService extends UApiService {
    PermissionDecision check(PermissionKey permission, PermissionContext context);

    /** Returns a point-in-time set of permissions effective in the supplied context. */
    Set<PermissionKey> effectivePermissions(PermissionContext context);

    /** Invalidates cached decisions and effective permissions for one actor. */
    void invalidate(UUID actorId);
}
