package dev.uapi.api.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class ReadyCheckStateMachineTest {
    private static final Instant START = Instant.parse("2026-07-12T00:00:00Z");
    private static final UUID GROUP = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID INITIATOR = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-000000000013");

    @Test
    void transitionsParticipantsToAllReadyWithAdjacentDeltas() {
        ReadyCheckSnapshot initial = started();
        assertEquals(Set.of(ReadyCheckParticipantState.NOT_READY),
            Set.copyOf(initial.participants().values()));

        ReadyCheckUpdateResult first = ReadyCheckStateMachine.respond(
            initial, INITIATOR, ReadyCheckParticipantState.READY, START.plusSeconds(1));
        ReadyCheckSnapshot afterFirst = first.snapshot().orElseThrow();
        assertEquals(ReadyCheckPhase.ACTIVE, afterFirst.phase());
        assertEquals(1, afterFirst.revision());
        assertEquals(Map.of(INITIATOR, ReadyCheckParticipantState.READY),
            first.delta().orElseThrow().changedParticipants());

        ReadyCheckUpdateResult second = ReadyCheckStateMachine.respond(
            afterFirst, MEMBER, ReadyCheckParticipantState.READY, START.plusSeconds(2));
        ReadyCheckSnapshot completed = second.snapshot().orElseThrow();
        assertEquals(ReadyCheckPhase.ALL_READY, completed.phase());
        assertTrue(completed.terminal());
        assertEquals(2, completed.revision());
        assertEquals(ReadyCheckPhase.ALL_READY, second.terminalResult().orElseThrow().outcome());
        assertThrows(UnsupportedOperationException.class,
            () -> completed.participants().put(OUTSIDER, ReadyCheckParticipantState.READY));
    }

    @Test
    void readyParticipantCanReturnToNotReadyWhileCheckIsActive() {
        ReadyCheckSnapshot initial = started();
        ReadyCheckSnapshot ready = ReadyCheckStateMachine.respond(
            initial, INITIATOR, ReadyCheckParticipantState.READY, START.plusSeconds(1))
            .snapshot().orElseThrow();

        ReadyCheckUpdateResult result = ReadyCheckStateMachine.respond(
            ready, INITIATOR, ReadyCheckParticipantState.NOT_READY, START.plusSeconds(2));

        assertEquals(ReadyCheckUpdateStatus.APPLIED, result.status());
        assertEquals(ReadyCheckPhase.ACTIVE, result.snapshot().orElseThrow().phase());
        assertEquals(ReadyCheckParticipantState.NOT_READY,
            result.snapshot().orElseThrow().participants().get(INITIATOR));
    }

    @Test
    void declineIsTerminalAndRejectsEveryLaterTransition() {
        ReadyCheckUpdateResult declined = ReadyCheckStateMachine.respond(
            started(), MEMBER, ReadyCheckParticipantState.DECLINED, START.plusSeconds(1));
        ReadyCheckSnapshot terminal = declined.snapshot().orElseThrow();

        assertEquals(ReadyCheckPhase.DECLINED, terminal.phase());
        assertEquals(MEMBER, terminal.result().orElseThrow().actorId().orElseThrow());
        assertEquals(ReadyCheckUpdateStatus.TERMINAL_STATE, ReadyCheckStateMachine.respond(
            terminal, INITIATOR, ReadyCheckParticipantState.READY, START.plusSeconds(2)).status());
        assertEquals(ReadyCheckUpdateStatus.TERMINAL_STATE, ReadyCheckStateMachine.cancel(
            terminal, INITIATOR, false, START.plusSeconds(2), reason("cancel")).status());
        assertEquals(ReadyCheckUpdateStatus.TERMINAL_STATE, ReadyCheckStateMachine.timeout(
            terminal, START.plusSeconds(60)).status());
    }

    @Test
    void timeoutWinsAtTheDeadlineAndBecomesTerminal() {
        ReadyCheckSnapshot initial = started();

        ReadyCheckUpdateResult early = ReadyCheckStateMachine.timeout(
            initial, initial.expiresAt().minusNanos(1));
        ReadyCheckUpdateResult expired = ReadyCheckStateMachine.respond(
            initial, INITIATOR, ReadyCheckParticipantState.READY, initial.expiresAt());

        assertEquals(ReadyCheckUpdateStatus.NO_CHANGE, early.status());
        assertEquals(ReadyCheckPhase.TIMED_OUT, expired.snapshot().orElseThrow().phase());
        assertEquals(ReadyCheckPhase.TIMED_OUT, expired.terminalResult().orElseThrow().outcome());
        assertEquals(ReadyCheckUpdateStatus.TERMINAL_STATE, ReadyCheckStateMachine.respond(
            expired.snapshot().orElseThrow(), MEMBER, ReadyCheckParticipantState.READY,
            initial.expiresAt().plusSeconds(1)).status());
    }

    @Test
    void rejectsUnauthorizedAndUnknownActorsWithoutChangingState() {
        ReadyCheckStartRequest request = request(Set.of(INITIATOR, MEMBER));
        ReadyCheckUpdateResult unauthorizedStart = ReadyCheckStateMachine.start(
            UUID.randomUUID(), request, request.participants(), false, START);
        ReadyCheckUpdateResult unknownAtStart = ReadyCheckStateMachine.start(
            UUID.randomUUID(), request(Set.of(INITIATOR, OUTSIDER)),
            Set.of(INITIATOR, MEMBER), true, START);
        ReadyCheckSnapshot initial = started();
        ReadyCheckUpdateResult unknownResponse = ReadyCheckStateMachine.respond(
            initial, OUTSIDER, ReadyCheckParticipantState.READY, START.plusSeconds(1));
        ReadyCheckUpdateResult unauthorizedCancel = ReadyCheckStateMachine.cancel(
            initial, OUTSIDER, false, START.plusSeconds(1), reason("spoofed"));

        assertEquals(ReadyCheckUpdateStatus.UNAUTHORIZED, unauthorizedStart.status());
        assertTrue(unauthorizedStart.snapshot().isEmpty());
        assertEquals(ReadyCheckUpdateStatus.UNKNOWN_PARTICIPANT, unknownAtStart.status());
        assertEquals(ReadyCheckUpdateStatus.UNKNOWN_PARTICIPANT, unknownResponse.status());
        assertEquals(ReadyCheckUpdateStatus.UNAUTHORIZED, unauthorizedCancel.status());
        assertFalse(unknownResponse.applied());
        assertEquals(initial, unknownResponse.snapshot().orElseThrow());
        assertEquals(initial, unauthorizedCancel.snapshot().orElseThrow());
    }

    @Test
    void validatesStartAndSnapshotInvariants() {
        assertThrows(IllegalArgumentException.class,
            () -> request(Set.of(MEMBER)));
        assertThrows(IllegalArgumentException.class, () -> new ReadyCheckSnapshot(
            UUID.randomUUID(), GROUP, INITIATOR, START, START.plusSeconds(30), 0,
            Map.of(INITIATOR, ReadyCheckParticipantState.READY), ReadyCheckPhase.ACTIVE,
            java.util.Optional.empty()));
    }

    private static ReadyCheckSnapshot started() {
        ReadyCheckStartRequest request = request(new LinkedHashSet<>(Set.of(INITIATOR, MEMBER)));
        return ReadyCheckStateMachine.start(
            UUID.fromString("00000000-0000-0000-0000-000000000020"),
            request, request.participants(), true, START).snapshot().orElseThrow();
    }

    private static ReadyCheckStartRequest request(Set<UUID> participants) {
        return new ReadyCheckStartRequest(GROUP, INITIATOR, participants, Duration.ofSeconds(30));
    }

    private static ResourceLocation reason(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", "ready_check/" + path);
    }
}
