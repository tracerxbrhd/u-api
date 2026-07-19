package dev.uapi.api.social;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Complete immutable state of one ready check at a specific revision. */
public record ReadyCheckSnapshot(
    UUID checkId,
    UUID groupId,
    UUID initiatorId,
    Instant createdAt,
    Instant expiresAt,
    long revision,
    Map<UUID, ReadyCheckParticipantState> participants,
    ReadyCheckPhase phase,
    Optional<ReadyCheckResult> result
) {
    public ReadyCheckSnapshot {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(initiatorId, "initiatorId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) throw new IllegalArgumentException("expiresAt must be after createdAt");
        if (revision < 0) throw new IllegalArgumentException("revision must not be negative");

        Objects.requireNonNull(participants, "participants");
        LinkedHashMap<UUID, ReadyCheckParticipantState> copied = new LinkedHashMap<>();
        participants.forEach((participant, state) -> copied.put(
            Objects.requireNonNull(participant, "participant id"),
            Objects.requireNonNull(state, "participant state")));
        if (copied.isEmpty()) throw new IllegalArgumentException("participants must not be empty");
        if (!copied.containsKey(initiatorId))
            throw new IllegalArgumentException("initiator must be a ready-check participant");
        participants = Collections.unmodifiableMap(copied);

        Objects.requireNonNull(phase, "phase");
        result = Objects.requireNonNull(result, "result");
        if (phase.terminal() != result.isPresent())
            throw new IllegalArgumentException("terminal phase and result must be present together");
        result.ifPresent(value -> {
            if (!value.checkId().equals(checkId)) throw new IllegalArgumentException("result checkId mismatch");
            if (value.outcome() != phase) throw new IllegalArgumentException("result outcome mismatch");
            if (value.completedAt().isBefore(createdAt))
                throw new IllegalArgumentException("result predates ready check");
        });

        boolean anyDeclined = participants.values().stream()
            .anyMatch(state -> state == ReadyCheckParticipantState.DECLINED);
        boolean allReady = participants.values().stream()
            .allMatch(state -> state == ReadyCheckParticipantState.READY);
        if (phase == ReadyCheckPhase.ACTIVE && (anyDeclined || allReady))
            throw new IllegalArgumentException("active ready check has terminal participant state");
        if (phase == ReadyCheckPhase.ALL_READY && !allReady)
            throw new IllegalArgumentException("ALL_READY requires every participant to be ready");
        if (phase == ReadyCheckPhase.DECLINED && !anyDeclined)
            throw new IllegalArgumentException("DECLINED requires a declined participant");
        if (phase != ReadyCheckPhase.DECLINED && anyDeclined)
            throw new IllegalArgumentException("declined participant requires DECLINED phase");
    }

    public boolean terminal() {
        return phase.terminal();
    }

    public Optional<ReadyCheckParticipantState> participantState(UUID participantId) {
        return Optional.ofNullable(participants.get(Objects.requireNonNull(participantId, "participantId")));
    }
}
