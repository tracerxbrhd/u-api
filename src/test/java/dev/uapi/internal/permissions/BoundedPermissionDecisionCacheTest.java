package dev.uapi.internal.permissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.uapi.api.permissions.PermissionContext;
import dev.uapi.api.permissions.PermissionDecision;
import dev.uapi.api.permissions.PermissionKey;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

final class BoundedPermissionDecisionCacheTest {
    @Test
    void hitsByFullContextIdentityAndExplicitInvalidationForcesEvaluation() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger evaluations = new AtomicInteger();
        BoundedPermissionDecisionCache cache = new BoundedPermissionDecisionCache(
            8, Duration.ofNanos(100), clock::get
        );
        UUID actor = UUID.randomUUID();
        PermissionContext context = PermissionContext.action(actor, id("break"));
        PermissionKey permission = new PermissionKey(id("territory.break"));
        PermissionDecision decision = PermissionDecision.allowed(id("allowed"));

        PermissionDecision first = cache.getOrCompute(permission, context, () -> {
            evaluations.incrementAndGet();
            return decision;
        });
        PermissionDecision cached = cache.getOrCompute(permission, context, () -> {
            evaluations.incrementAndGet();
            return PermissionDecision.denied(id("unexpected"));
        });

        assertSame(first, cached);
        assertEquals(1, evaluations.get());
        assertEquals(1, cache.snapshot().hits());
        assertEquals(1, cache.invalidate(actor));

        cache.getOrCompute(permission, context, () -> {
            evaluations.incrementAndGet();
            return decision;
        });
        assertEquals(2, evaluations.get());
    }

    @Test
    void expiresEntriesAndBoundsSizeWithLruEviction() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger evaluations = new AtomicInteger();
        BoundedPermissionDecisionCache cache = new BoundedPermissionDecisionCache(
            2, Duration.ofNanos(10), clock::get
        );
        UUID actor = UUID.randomUUID();
        PermissionKey permission = new PermissionKey(id("action"));

        evaluate(cache, permission, PermissionContext.action(actor, id("one")), evaluations);
        evaluate(cache, permission, PermissionContext.action(actor, id("two")), evaluations);
        evaluate(cache, permission, PermissionContext.action(actor, id("three")), evaluations);
        assertEquals(2, cache.snapshot().size());
        assertEquals(1, cache.snapshot().evictions());

        PermissionContext expiring = PermissionContext.action(UUID.randomUUID(), id("expiring"));
        evaluate(cache, permission, expiring, evaluations);
        int beforeExpiry = evaluations.get();
        clock.addAndGet(11);
        evaluate(cache, permission, expiring, evaluations);

        assertEquals(beforeExpiry + 1, evaluations.get());
        assertEquals(0, cache.snapshot().hits());
        assertEquals(5, cache.snapshot().misses());
    }

    private static PermissionDecision evaluate(
        BoundedPermissionDecisionCache cache,
        PermissionKey permission,
        PermissionContext context,
        AtomicInteger evaluations
    ) {
        return cache.getOrCompute(permission, context, () -> {
            evaluations.incrementAndGet();
            return PermissionDecision.allowed(id("allowed"));
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("permission_test", path);
    }
}
