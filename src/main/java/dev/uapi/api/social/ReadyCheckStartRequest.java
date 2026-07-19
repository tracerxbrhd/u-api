package dev.uapi.api.social;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Request passed by trusted server code to a {@link ReadyCheckService}.
 * The service must still validate the initiator and every participant against current social state.
 */
public record ReadyCheckStartRequest(
    UUID groupId,
    UUID initiatorId,
    Set<UUID> participants,
    Duration timeout
) {
    public ReadyCheckStartRequest {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(initiatorId, "initiatorId");
        Objects.requireNonNull(participants, "participants");
        LinkedHashSet<UUID> copied = new LinkedHashSet<>();
        for (UUID participant : participants)
            copied.add(Objects.requireNonNull(participant, "participants must not contain null"));
        if (copied.isEmpty()) throw new IllegalArgumentException("participants must not be empty");
        if (!copied.contains(initiatorId))
            throw new IllegalArgumentException("initiator must be a ready-check participant");
        participants = Collections.unmodifiableSet(copied);

        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative())
            throw new IllegalArgumentException("timeout must be positive");
    }
}
