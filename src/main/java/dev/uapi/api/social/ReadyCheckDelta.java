package dev.uapi.api.social;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Minimal immutable change from one ready-check revision to the next. */
public record ReadyCheckDelta(
    UUID checkId,
    long fromRevision,
    long toRevision,
    Map<UUID, ReadyCheckParticipantState> changedParticipants,
    ReadyCheckPhase phase,
    Optional<ReadyCheckResult> result
) {
    public ReadyCheckDelta {
        Objects.requireNonNull(checkId, "checkId");
        if (fromRevision < 0 || toRevision != fromRevision + 1)
            throw new IllegalArgumentException("delta revisions must be adjacent and non-negative");
        Objects.requireNonNull(changedParticipants, "changedParticipants");
        LinkedHashMap<UUID, ReadyCheckParticipantState> copied = new LinkedHashMap<>();
        changedParticipants.forEach((participant, state) -> copied.put(
            Objects.requireNonNull(participant, "participant id"),
            Objects.requireNonNull(state, "participant state")));
        changedParticipants = Collections.unmodifiableMap(copied);
        Objects.requireNonNull(phase, "phase");
        result = Objects.requireNonNull(result, "result");
        if (phase.terminal() != result.isPresent())
            throw new IllegalArgumentException("terminal delta and result must be present together");
        result.ifPresent(value -> {
            if (!value.checkId().equals(checkId)) throw new IllegalArgumentException("result checkId mismatch");
            if (value.outcome() != phase) throw new IllegalArgumentException("result outcome mismatch");
        });
    }
}
