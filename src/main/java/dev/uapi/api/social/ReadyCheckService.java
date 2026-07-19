package dev.uapi.api.social;

import dev.uapi.api.services.UApiService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-authoritative ready-check service contract.
 *
 * <p>The implementation owns UUID generation, clock access, concurrent-check policy, membership validation,
 * authorization, storage, timeout processing, and distribution of snapshots/deltas. Callers must never apply
 * client-provided state directly.</p>
 */
public interface ReadyCheckService extends UApiService {
    Optional<ReadyCheckSnapshot> getReadyCheck(UUID checkId);

    Optional<ReadyCheckSnapshot> getActiveReadyCheck(UUID groupId);

    ReadyCheckUpdateResult startReadyCheck(ReadyCheckStartRequest request);

    ReadyCheckUpdateResult respondReadyCheck(
        UUID checkId,
        UUID participantId,
        ReadyCheckParticipantState response
    );

    ReadyCheckUpdateResult cancelReadyCheck(UUID checkId, UUID actorId, ResourceLocation reasonCode);

    /** Expires all checks due according to the implementation's authoritative server clock. */
    List<ReadyCheckUpdateResult> expireReadyChecks();
}
