package dev.uapi.internal.permissions;

import dev.uapi.api.permissions.PermissionContext;
import dev.uapi.api.permissions.PermissionDecision;
import dev.uapi.api.permissions.PermissionKey;
import dev.uapi.api.permissions.PermissionService;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Internal server-safe foundation for permission services which want bounded decision caching. */
public abstract class AbstractCachingPermissionService implements PermissionService {
    private final BoundedPermissionDecisionCache decisionCache;

    protected AbstractCachingPermissionService(int maximumCachedDecisions, Duration decisionTimeToLive) {
        this.decisionCache = new BoundedPermissionDecisionCache(
            maximumCachedDecisions,
            Objects.requireNonNull(decisionTimeToLive, "decisionTimeToLive")
        );
    }

    @Override
    public final PermissionDecision check(PermissionKey permission, PermissionContext context) {
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(context, "context");
        return this.decisionCache.getOrCompute(permission, context,
            () -> evaluate(permission, context));
    }

    @Override
    public final Set<PermissionKey> effectivePermissions(PermissionContext context) {
        Objects.requireNonNull(context, "context");
        return Set.copyOf(Objects.requireNonNull(resolveEffectivePermissions(context),
            "resolveEffectivePermissions result"));
    }

    @Override
    public final void invalidate(UUID actorId) {
        this.decisionCache.invalidate(Objects.requireNonNull(actorId, "actorId"));
        onInvalidated(actorId);
    }

    protected abstract PermissionDecision evaluate(PermissionKey permission, PermissionContext context);

    protected abstract Set<PermissionKey> resolveEffectivePermissions(PermissionContext context);

    protected void onInvalidated(UUID actorId) {
    }
}
