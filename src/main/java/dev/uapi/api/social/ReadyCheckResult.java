package dev.uapi.api.social;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** Immutable terminal result of a ready check. */
public record ReadyCheckResult(
    UUID checkId,
    ReadyCheckPhase outcome,
    Instant completedAt,
    Optional<UUID> actorId,
    ResourceLocation reasonCode
) {
    public ReadyCheckResult {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(outcome, "outcome");
        if (!outcome.terminal()) throw new IllegalArgumentException("result outcome must be terminal");
        Objects.requireNonNull(completedAt, "completedAt");
        actorId = Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(reasonCode, "reasonCode");
    }
}
