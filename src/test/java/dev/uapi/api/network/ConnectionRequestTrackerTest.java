package dev.uapi.api.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.uapi.api.network.ConnectionRequestTracker.RegistrationStatus;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class ConnectionRequestTrackerTest {
    @Test
    void enforcesLimitsAndCompletesOnlyAnExactAckOrErrorOnce() {
        AtomicLong clock = new AtomicLong();
        ConnectionRequestTracker tracker = new ConnectionRequestTracker(
            2, 3, Duration.ofSeconds(30), clock::get
        );
        UUID firstConnection = uuid(1);
        UUID secondConnection = uuid(2);
        RequestId first = request(11);
        RequestId second = request(12);
        RequestId third = request(13);
        RequestId fourth = request(14);

        assertEquals(RegistrationStatus.ACCEPTED, tracker.register(firstConnection, first, id("one")));
        assertEquals(RegistrationStatus.ACCEPTED, tracker.register(firstConnection, second, id("two")));
        assertEquals(RegistrationStatus.CONNECTION_LIMIT_REACHED,
            tracker.register(firstConnection, third, id("three")));
        assertEquals(RegistrationStatus.ACCEPTED, tracker.register(secondConnection, third, id("three")));
        assertEquals(RegistrationStatus.TOTAL_LIMIT_REACHED,
            tracker.register(secondConnection, fourth, id("four")));
        assertEquals(RegistrationStatus.DUPLICATE_REQUEST_ID,
            tracker.register(secondConnection, first, id("duplicate")));

        assertTrue(tracker.complete(secondConnection, new OperationAck(first, id("ok"))).isEmpty());
        assertEquals(3, tracker.pendingCount());
        assertTrue(tracker.complete(firstConnection, new OperationAck(first, id("ok"))).isPresent());
        assertTrue(tracker.complete(firstConnection, new OperationAck(first, id("ok"))).isEmpty());
        assertTrue(tracker.complete(firstConnection, new OperationError(second, id("denied"))).isPresent());
        assertEquals(1, tracker.pendingCount());
    }

    @Test
    void expiresRequestsRejectsLateCompletionAndReturnsImmutableCleanup() {
        AtomicLong clock = new AtomicLong();
        ConnectionRequestTracker tracker = new ConnectionRequestTracker(
            4, 8, Duration.ofSeconds(5), clock::get
        );
        UUID connection = uuid(1);
        RequestId request = request(21);
        tracker.register(connection, request, id("operation"));
        clock.set(Duration.ofSeconds(5).toNanos());

        List<ConnectionRequestTracker.TimedOutRequest> timedOut = tracker.cleanupTimedOut();

        assertEquals(1, timedOut.size());
        assertEquals(request, timedOut.getFirst().request().requestId());
        assertEquals(0, tracker.pendingCount());
        assertTrue(tracker.complete(connection, new OperationError(request, id("late"))).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> timedOut.add(timedOut.getFirst()));
    }

    @Test
    void clearsOnlyTheSelectedConnection() {
        AtomicLong clock = new AtomicLong();
        ConnectionRequestTracker tracker = new ConnectionRequestTracker(
            4, 8, Duration.ofSeconds(5), clock::get
        );
        UUID firstConnection = uuid(1);
        UUID secondConnection = uuid(2);
        tracker.register(firstConnection, request(31), id("one"));
        tracker.register(firstConnection, request(32), id("two"));
        tracker.register(secondConnection, request(33), id("three"));

        List<ConnectionRequestTracker.PendingRequest> cleared = tracker.clearConnection(firstConnection);

        assertEquals(2, cleared.size());
        assertEquals(0, tracker.pendingCount(firstConnection));
        assertEquals(1, tracker.pendingCount(secondConnection));
        assertEquals(1, tracker.pendingCount());
        assertFalse(cleared.isEmpty());
    }

    private static RequestId request(long suffix) {
        return new RequestId(new UUID(0L, suffix));
    }

    private static UUID uuid(long suffix) {
        return new UUID(0L, suffix);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
