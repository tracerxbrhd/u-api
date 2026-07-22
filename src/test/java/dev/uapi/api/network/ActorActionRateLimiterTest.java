package dev.uapi.api.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

final class ActorActionRateLimiterTest {
    @Test
    void limitsEachActorActionPairUsingAnExactSlidingWindow() {
        AtomicLong clock = new AtomicLong();
        ActorActionRateLimiter limiter = new ActorActionRateLimiter(
            2, Duration.ofSeconds(10), 16, clock::get
        );
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertTrue(limiter.tryAcquire(actor, id("invite")).allowed());
        assertTrue(limiter.tryAcquire(actor, id("invite")).allowed());
        RateLimitDecision denied = limiter.tryAcquire(actor, id("invite"));

        assertFalse(denied.allowed());
        assertEquals(Duration.ofSeconds(10), denied.retryAfter());
        assertTrue(limiter.tryAcquire(actor, id("ping")).allowed());
        assertTrue(limiter.tryAcquire(UUID.fromString("00000000-0000-0000-0000-000000000002"),
            id("invite")).allowed());

        clock.set(Duration.ofSeconds(10).toNanos());
        assertTrue(limiter.tryAcquire(actor, id("invite")).allowed());
    }

    @Test
    void boundsTrackedKeysAndCanClearAnActor() {
        AtomicLong clock = new AtomicLong();
        ActorActionRateLimiter limiter = new ActorActionRateLimiter(
            1, Duration.ofMinutes(1), 2, clock::get
        );
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID third = UUID.fromString("00000000-0000-0000-0000-000000000003");

        limiter.tryAcquire(first, id("action"));
        limiter.tryAcquire(second, id("action"));
        RateLimitDecision capacityDenied = limiter.tryAcquire(third, id("action"));

        assertFalse(capacityDenied.allowed());
        assertEquals(2, limiter.trackedKeys());
        assertEquals(1, limiter.clearActor(second));
        assertEquals(1, limiter.trackedKeys());
    }

    @Test
    void rejectsAClockThatMovesBackwards() {
        AtomicLong clock = new AtomicLong(10);
        ActorActionRateLimiter limiter = new ActorActionRateLimiter(
            1, Duration.ofSeconds(1), 2, clock::get
        );
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000001");
        limiter.tryAcquire(actor, id("action"));
        clock.set(9);

        assertThrows(IllegalStateException.class, () -> limiter.tryAcquire(actor, id("action")));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }
}
