package dev.uapi.api.social;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/**
 * Pure server-side ready-check state machine. It owns no global state and never mutates an input snapshot.
 */
public final class ReadyCheckStateMachine {
    public static final Identifier REASON_ALL_READY = reason("all_ready");
    public static final Identifier REASON_DECLINED = reason("declined");
    public static final Identifier REASON_TIMEOUT = reason("timeout");
    public static final Identifier REASON_CANCELLED = reason("cancelled");

    private ReadyCheckStateMachine() {
    }

    public static ReadyCheckUpdateResult start(
        UUID checkId,
        ReadyCheckStartRequest request,
        Set<UUID> eligibleParticipants,
        boolean initiatorAuthorized,
        Instant now
    ) {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(eligibleParticipants, "eligibleParticipants");
        Objects.requireNonNull(now, "now");
        if (!initiatorAuthorized)
            return ReadyCheckUpdateResult.unchanged(ReadyCheckUpdateStatus.UNAUTHORIZED, null);
        if (!eligibleParticipants.containsAll(request.participants()))
            return ReadyCheckUpdateResult.unchanged(ReadyCheckUpdateStatus.UNKNOWN_PARTICIPANT, null);

        LinkedHashMap<UUID, ReadyCheckParticipantState> participants = new LinkedHashMap<>();
        request.participants().forEach(participant ->
            participants.put(participant, ReadyCheckParticipantState.NOT_READY));
        ReadyCheckSnapshot snapshot = new ReadyCheckSnapshot(
            checkId,
            request.groupId(),
            request.initiatorId(),
            now,
            now.plus(request.timeout()),
            0,
            participants,
            ReadyCheckPhase.ACTIVE,
            Optional.empty()
        );
        return ReadyCheckUpdateResult.started(snapshot);
    }

    public static ReadyCheckUpdateResult respond(
        ReadyCheckSnapshot snapshot,
        UUID participantId,
        ReadyCheckParticipantState response,
        Instant now
    ) {
        requireOperation(snapshot, now);
        Objects.requireNonNull(participantId, "participantId");
        Objects.requireNonNull(response, "response");
        if (snapshot.terminal()) return terminal(snapshot);
        if (!now.isBefore(snapshot.expiresAt())) return timeout(snapshot, now);
        if (!snapshot.participants().containsKey(participantId))
            return ReadyCheckUpdateResult.unchanged(ReadyCheckUpdateStatus.UNKNOWN_PARTICIPANT, snapshot);
        if (snapshot.participants().get(participantId) == response)
            return ReadyCheckUpdateResult.unchanged(ReadyCheckUpdateStatus.NO_CHANGE, snapshot);

        LinkedHashMap<UUID, ReadyCheckParticipantState> participants =
            new LinkedHashMap<>(snapshot.participants());
        participants.put(participantId, response);

        ReadyCheckPhase phase = response == ReadyCheckParticipantState.DECLINED
            ? ReadyCheckPhase.DECLINED
            : participants.values().stream().allMatch(state -> state == ReadyCheckParticipantState.READY)
                ? ReadyCheckPhase.ALL_READY
                : ReadyCheckPhase.ACTIVE;
        Optional<ReadyCheckResult> result = phase.terminal()
            ? Optional.of(new ReadyCheckResult(snapshot.checkId(), phase, now,
                Optional.of(participantId), phase == ReadyCheckPhase.ALL_READY
                    ? REASON_ALL_READY : REASON_DECLINED))
            : Optional.empty();
        return advance(snapshot, participants, phase, result, Map.of(participantId, response));
    }

    public static ReadyCheckUpdateResult cancel(
        ReadyCheckSnapshot snapshot,
        UUID actorId,
        boolean managerAuthorized,
        Instant now,
        Identifier reasonCode
    ) {
        requireOperation(snapshot, now);
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(reasonCode, "reasonCode");
        if (snapshot.terminal()) return terminal(snapshot);
        if (!now.isBefore(snapshot.expiresAt())) return timeout(snapshot, now);
        if (!actorId.equals(snapshot.initiatorId()) && !managerAuthorized)
            return ReadyCheckUpdateResult.unchanged(ReadyCheckUpdateStatus.UNAUTHORIZED, snapshot);

        Optional<ReadyCheckResult> result = Optional.of(new ReadyCheckResult(
            snapshot.checkId(), ReadyCheckPhase.CANCELLED, now, Optional.of(actorId), reasonCode));
        return advance(snapshot, snapshot.participants(), ReadyCheckPhase.CANCELLED,
            result, Map.of());
    }

    public static ReadyCheckUpdateResult timeout(ReadyCheckSnapshot snapshot, Instant now) {
        requireOperation(snapshot, now);
        if (snapshot.terminal()) return terminal(snapshot);
        if (now.isBefore(snapshot.expiresAt()))
            return ReadyCheckUpdateResult.unchanged(ReadyCheckUpdateStatus.NO_CHANGE, snapshot);

        Optional<ReadyCheckResult> result = Optional.of(new ReadyCheckResult(
            snapshot.checkId(), ReadyCheckPhase.TIMED_OUT, now, Optional.empty(), REASON_TIMEOUT));
        return advance(snapshot, snapshot.participants(), ReadyCheckPhase.TIMED_OUT,
            result, Map.of());
    }

    private static ReadyCheckUpdateResult advance(
        ReadyCheckSnapshot previous,
        Map<UUID, ReadyCheckParticipantState> participants,
        ReadyCheckPhase phase,
        Optional<ReadyCheckResult> result,
        Map<UUID, ReadyCheckParticipantState> changedParticipants
    ) {
        long nextRevision = previous.revision() + 1;
        ReadyCheckSnapshot current = new ReadyCheckSnapshot(
            previous.checkId(), previous.groupId(), previous.initiatorId(), previous.createdAt(),
            previous.expiresAt(), nextRevision, participants, phase, result);
        ReadyCheckDelta delta = new ReadyCheckDelta(
            current.checkId(), previous.revision(), nextRevision, changedParticipants, phase, result);
        return ReadyCheckUpdateResult.applied(current, delta);
    }

    private static ReadyCheckUpdateResult terminal(ReadyCheckSnapshot snapshot) {
        return ReadyCheckUpdateResult.unchanged(ReadyCheckUpdateStatus.TERMINAL_STATE, snapshot);
    }

    private static void requireOperation(ReadyCheckSnapshot snapshot, Instant now) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(now, "now");
        if (now.isBefore(snapshot.createdAt()))
            throw new IllegalArgumentException("operation time predates ready check");
    }

    private static Identifier reason(String path) {
        return Identifier.fromNamespaceAndPath("u_api", "ready_check/" + path);
    }
}
