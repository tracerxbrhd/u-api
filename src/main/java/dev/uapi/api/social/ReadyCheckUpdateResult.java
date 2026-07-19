package dev.uapi.api.social;

import java.util.Objects;
import java.util.Optional;

/** Immutable operation result containing the newest snapshot and, when applicable, its delta. */
public record ReadyCheckUpdateResult(
    ReadyCheckUpdateStatus status,
    Optional<ReadyCheckSnapshot> snapshot,
    Optional<ReadyCheckDelta> delta
) {
    public ReadyCheckUpdateResult {
        Objects.requireNonNull(status, "status");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        delta = Objects.requireNonNull(delta, "delta");
        if (status == ReadyCheckUpdateStatus.APPLIED && snapshot.isEmpty())
            throw new IllegalArgumentException("applied update requires a snapshot");
        if (status != ReadyCheckUpdateStatus.APPLIED && delta.isPresent())
            throw new IllegalArgumentException("rejected update must not contain a delta");
        if (delta.isPresent()) {
            ReadyCheckSnapshot current = snapshot.orElseThrow();
            ReadyCheckDelta change = delta.orElseThrow();
            if (!current.checkId().equals(change.checkId()))
                throw new IllegalArgumentException("delta checkId mismatch");
            if (current.revision() != change.toRevision())
                throw new IllegalArgumentException("delta revision mismatch");
            if (current.phase() != change.phase() || !current.result().equals(change.result()))
                throw new IllegalArgumentException("delta terminal state mismatch");
        }
    }

    public boolean applied() {
        return status == ReadyCheckUpdateStatus.APPLIED;
    }

    public Optional<ReadyCheckResult> terminalResult() {
        return snapshot.flatMap(ReadyCheckSnapshot::result);
    }

    public static ReadyCheckUpdateResult notFound() {
        return new ReadyCheckUpdateResult(ReadyCheckUpdateStatus.NOT_FOUND, Optional.empty(), Optional.empty());
    }

    static ReadyCheckUpdateResult started(ReadyCheckSnapshot snapshot) {
        return new ReadyCheckUpdateResult(ReadyCheckUpdateStatus.APPLIED, Optional.of(snapshot), Optional.empty());
    }

    static ReadyCheckUpdateResult applied(ReadyCheckSnapshot snapshot, ReadyCheckDelta delta) {
        return new ReadyCheckUpdateResult(ReadyCheckUpdateStatus.APPLIED, Optional.of(snapshot), Optional.of(delta));
    }

    static ReadyCheckUpdateResult unchanged(ReadyCheckUpdateStatus status, ReadyCheckSnapshot snapshot) {
        if (status == ReadyCheckUpdateStatus.APPLIED)
            throw new IllegalArgumentException("unchanged result cannot use APPLIED");
        return new ReadyCheckUpdateResult(status, Optional.ofNullable(snapshot), Optional.empty());
    }
}
